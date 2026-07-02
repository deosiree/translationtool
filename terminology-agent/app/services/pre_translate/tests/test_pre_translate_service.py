"""PreTranslateService 行为契约测试 — Mock Repo，不连 MySQL。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

import pytest

from app.services.pre_translate import PreTranslateService


@pytest.mark.service
async def test_exact_match_auto_approved(
    pre_translate_service, mock_repo, mock_translate_entry, mock_workbench_sync
):
    """精确匹配术语库时应 auto_approved 且不写 audit。"""
    mock_repo.find_exact.return_value = mock_translate_entry

    result = await pre_translate_service.run_batch(
        entries=[{"id": "e1", "entry": "正在查询第 %1/%2 个路径的OID...", "russian": ""}],
        task_id="task-1",
        task_name="任务A",
        product_name="产品A",
        target_lang="俄文",
        department="通用平台部",
        confidence_threshold=0.8,
    )

    assert result["auto_count"] == 1
    assert result["pending_count"] == 0
    item = result["list"][0]
    assert item["agent_meta"]["review_status"] == "auto_approved"
    assert item["agent_meta"]["confidence"] == 1.0
    assert item["translate"] == mock_translate_entry.translate
    assert item["agent_meta"]["reasoning"].startswith("基于术语")
    mock_repo.create_pretranslate_audit.assert_not_called()
    mock_workbench_sync.sync_translation_to_pending_audit.assert_awaited_once()


@pytest.mark.service
async def test_fuzzy_match_respects_threshold(pre_translate_service, mock_repo):
    """模糊匹配置信度不足时应 needs_human 并写 audit。"""
    mock_repo.find_exact.return_value = None
    mock_repo.find_fuzzy.return_value = [
        SimpleNamespace(entry="完全无关的词条文本", translate="несвязанный перевод"),
    ]

    async def fake_translate(state):
        """Mock LLM 节点：返回低置信度 fallback 译文。"""
        state["suggested_translation"] = "LLM fuzzy fallback"
        state["confidence"] = 0.65
        state["llm_detail"] = "fuzzy low conf"
        state["trace"] = [{"stage": "translate_suggest"}]
        return state

    with patch(
        "app.graph.pre_translate.builder.translate_suggest_node",
        fake_translate,
    ):
        result = await pre_translate_service.run_batch(
            entries=[{"id": "e1", "entry": "正在查询路径 OID", "russian": ""}],
            task_id="task-1",
            task_name=None,
            product_name=None,
            target_lang="俄文",
            department=None,
            confidence_threshold=0.8,
        )

    assert result["auto_count"] == 0
    assert result["pending_count"] == 1
    item = result["list"][0]
    assert item["agent_meta"]["review_status"] == "needs_human"
    mock_repo.create_pretranslate_audit.assert_called_once()


@pytest.mark.service
async def test_no_match_llm_pending(pre_translate_service, mock_repo, monkeypatch):
    """术语库无命中时应走 LLM 路径并 needs_human。"""
    mock_repo.find_exact.return_value = None
    mock_repo.find_fuzzy.return_value = []

    async def fake_translate(state):
        """Mock LLM 节点：返回 mock 译文。"""
        state["suggested_translation"] = "Mock LLM перевод"
        state["confidence"] = 0.65
        state["llm_detail"] = "术语库未命中"
        state["trace"] = [{"stage": "translate_suggest"}]
        return state

    monkeypatch.setattr(
        "app.graph.pre_translate.builder.translate_suggest_node",
        fake_translate,
    )

    result = await pre_translate_service.run_batch(
        entries=[{"id": "e1", "entry": "全新未收录词条", "russian": ""}],
        task_id="task-1",
        task_name=None,
        product_name=None,
        target_lang="俄文",
        department=None,
        confidence_threshold=0.8,
    )

    assert result["pending_count"] == 1
    item = result["list"][0]
    assert item["agent_meta"]["retrieval_method"] == "none"
    assert item["agent_meta"]["review_status"] == "needs_human"
    assert item["agent_meta"]["suggested_translation"] == "Mock LLM перевод"
    assert item["agent_meta"]["reasoning"].startswith("基于LLM机翻")
    assert "[Agent]" not in (item["agent_meta"]["suggested_translation"] or "")


@pytest.mark.service
async def test_skips_child_entries(pre_translate_service, mock_repo):
    """含 parentID 的子词条应跳过，不触发检索。"""
    result = await pre_translate_service.run_batch(
        entries=[{"id": "e2", "entry": "admin", "parentID": "parent-1"}],
        task_id="task-1",
        task_name=None,
        product_name=None,
        target_lang="俄文",
        department=None,
        confidence_threshold=0.8,
    )

    assert result["list"] == []
    mock_repo.find_exact.assert_not_called()


@pytest.mark.service
async def test_agent_meta_shape(pre_translate_service, mock_repo, mock_translate_entry):
    """agent_meta 应包含 API 契约要求的六字段。"""
    mock_repo.find_exact.return_value = mock_translate_entry

    result = await pre_translate_service.run_batch(
        entries=[{"id": "e1", "entry": "正在查询第 %1/%2 个路径的OID...", "russian": ""}],
        task_id="task-1",
        task_name=None,
        product_name=None,
        target_lang="俄文",
        department=None,
        confidence_threshold=0.8,
    )

    meta = result["list"][0]["agent_meta"]
    assert set(meta.keys()) == {
        "confidence",
        "review_status",
        "suggested_translation",
        "similar_terms",
        "retrieval_method",
        "reasoning",
    }


@pytest.mark.unit
def test_new_translate_id_hex32():
    """new_translate_id 应返回 32 位 hex 字符串。"""
    tid = PreTranslateService.new_translate_id()
    assert len(tid) == 32
    assert all(c in "0123456789abcdef" for c in tid)
