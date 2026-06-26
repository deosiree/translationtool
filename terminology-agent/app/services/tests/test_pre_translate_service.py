"""PreTranslateService 行为契约测试 — Mock Repo，不连 MySQL。"""

from types import SimpleNamespace

import pytest


@pytest.mark.service
async def test_exact_match_auto_approved(pre_translate_service, mock_repo, mock_translate_entry):
    """精确匹配 → auto_approved，回填译文且不写 audit。"""
    mock_repo.find_exact.return_value = mock_translate_entry

    result = await pre_translate_service.batch_pre_translate(
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
    mock_repo.create_pretranslate_audit.assert_not_called()


@pytest.mark.service
async def test_fuzzy_match_respects_threshold(pre_translate_service, mock_repo):
    """低相似度模糊匹配 → needs_human 并创建 audit。"""
    mock_repo.find_exact.return_value = None
    # 低相似度候选 → confidence = 0.55 + score*0.4 < 0.8
    mock_repo.find_fuzzy.return_value = [
        SimpleNamespace(entry="完全无关的词条文本", translate="несвязанный перевод"),
    ]

    result = await pre_translate_service.batch_pre_translate(
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
    assert item["agent_meta"]["confidence"] < 0.8
    mock_repo.create_pretranslate_audit.assert_called_once()


@pytest.mark.service
async def test_no_match_low_confidence_pending(pre_translate_service, mock_repo):
    """术语库无命中 → confidence=0.45，进入待审核。"""
    mock_repo.find_exact.return_value = None
    mock_repo.find_fuzzy.return_value = []

    result = await pre_translate_service.batch_pre_translate(
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
    assert item["agent_meta"]["confidence"] == 0.45
    assert item["agent_meta"]["review_status"] == "needs_human"
    assert item["agent_meta"]["retrieval_method"] == "hybrid"


@pytest.mark.service
async def test_skips_child_entries(pre_translate_service, mock_repo):
    """含 parentID 的子词条应跳过，不触发检索。"""
    result = await pre_translate_service.batch_pre_translate(
        entries=[{"id": "e2", "entry": "admin", "parentID": "parent-1"}],
        task_id="task-1",
        task_name=None,
        product_name=None,
        target_lang="俄文",
        department=None,
        confidence_threshold=0.8,
    )

    assert result["list"] == []
    assert result["auto_count"] == 0
    assert result["pending_count"] == 0
    mock_repo.find_exact.assert_not_called()


@pytest.mark.service
async def test_agent_meta_shape(pre_translate_service, mock_repo, mock_translate_entry):
    """agent_meta 应包含六字段且 similar_terms 结构正确。"""
    mock_repo.find_exact.return_value = mock_translate_entry

    result = await pre_translate_service.batch_pre_translate(
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
    assert isinstance(meta["similar_terms"], list)
    assert meta["similar_terms"][0]["score"] == 1.0
