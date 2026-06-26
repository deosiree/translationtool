"""PreTranslateGraph 集成测试 — mock Repo + LLM。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.graph.pre_translate.runner import PreTranslateGraph


@pytest.fixture
def mock_graph_repo(mock_translate_entry):
    repo = AsyncMock()
    repo.find_exact = AsyncMock(return_value=None)
    repo.find_fuzzy = AsyncMock(return_value=[])
    repo.create_pretranslate_audit = AsyncMock(return_value=SimpleNamespace(id="audit-001"))
    return repo


@pytest.mark.graph
@pytest.mark.asyncio
async def test_graph_exact_auto_approved(mock_graph_repo, mock_translate_entry):
    mock_graph_repo.find_exact.return_value = mock_translate_entry

    def repo_factory(_session):
        return mock_graph_repo

    session = AsyncMock()
    with patch(
        "app.graph.pre_translate.nodes.features.io.retrieve_similar.TermRepository",
        repo_factory,
    ), patch(
        "app.graph.pre_translate.nodes.features.io.write_result.TermRepository",
        repo_factory,
    ):
        final = await PreTranslateGraph().run(
            source_text="正在查询第 %1/%2 个路径的OID...",
            target_lang="俄文",
            department="通用平台部",
            confidence_threshold=0.8,
            session=session,
        )

    assert final["retrieval_method"] == "exact"
    assert final["review_status"] == "auto_approved"
    assert final["llm_reasoning"].startswith("基于术语")
    mock_graph_repo.create_pretranslate_audit.assert_not_called()


@pytest.mark.graph
@pytest.mark.asyncio
async def test_graph_no_match_llm_path(mock_graph_repo):
    async def fake_translate(state):
        state["suggested_translation"] = "Новый перевод LLM"
        state["confidence"] = 0.65
        state["llm_detail"] = "mock llm"
        state["trace"] = [{"stage": "translate_suggest", "ok": True}]
        return state

    def repo_factory(_session):
        return mock_graph_repo

    session = AsyncMock()
    with patch(
        "app.graph.pre_translate.nodes.features.io.retrieve_similar.TermRepository",
        repo_factory,
    ), patch(
        "app.graph.pre_translate.nodes.features.io.write_result.TermRepository",
        repo_factory,
    ), patch(
        "app.graph.pre_translate.builder.translate_suggest_node",
        fake_translate,
    ):
        final = await PreTranslateGraph().run(
            source_text="全新未收录词条",
            target_lang="俄文",
            department=None,
            confidence_threshold=0.8,
            session=session,
        )

    assert final["retrieval_method"] == "none"
    assert final["translation_source"] == "llm"
    assert final["suggested_translation"] == "Новый перевод LLM"
    assert final["llm_reasoning"].startswith("基于LLM机翻")
    assert final["review_status"] == "needs_human"
    mock_graph_repo.create_pretranslate_audit.assert_called_once()
