"""PreTranslateGraph 集成测试 — mock Term/Word Repo + LLM。

Phase 3a：``mock_word_repo`` 与 ``test_graph_grep_whole_sentence_auto`` 覆盖 Grep 整句路径。
"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.graph.pre_translate.runner import PreTranslateGraph


@pytest.fixture
def mock_graph_repo(mock_translate_entry):
    """AsyncMock TermRepository — 默认无 RAG 命中。"""
    repo = AsyncMock()
    repo.find_exact = AsyncMock(return_value=None)
    repo.find_fuzzy = AsyncMock(return_value=[])
    repo.create_pretranslate_audit = AsyncMock(return_value=SimpleNamespace(id="audit-001"))
    return repo


@pytest.fixture
def mock_word_repo():
    """AsyncMock WordRepository — Grep Trie / find_by_word 默认空。"""
    repo = AsyncMock()
    repo.list_distinct_words = AsyncMock(return_value=[])
    repo.find_by_word = AsyncMock(return_value=[])
    return repo


def _patch_repos(mock_graph_repo, mock_word_repo):
    """patch Term/WordRepository 与 write_result，供图集成测注入 mock。"""
    def term_factory(_session):
        return mock_graph_repo

    def word_factory(_session):
        return mock_word_repo

    return (
        patch(
            "app.graph.pre_translate.nodes.features.io.retrieve_similar.TermRepository",
            term_factory,
        ),
        patch(
            "app.graph.pre_translate.nodes.features.io.retrieve_similar.WordRepository",
            word_factory,
        ),
        patch(
            "app.graph.pre_translate.nodes.features.workflow.decompose_compose.WordRepository",
            word_factory,
        ),
        patch(
            "app.graph.pre_translate.nodes.features.io.write_result.TermRepository",
            term_factory,
        ),
    )


@pytest.mark.graph
@pytest.mark.asyncio
async def test_graph_exact_auto_approved(mock_graph_repo, mock_word_repo, mock_translate_entry):
    mock_graph_repo.find_exact.return_value = mock_translate_entry

    session = AsyncMock()
    patches = _patch_repos(mock_graph_repo, mock_word_repo)
    with patches[0], patches[1], patches[2], patches[3]:
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
async def test_graph_no_match_llm_path(mock_graph_repo, mock_word_repo):
    async def fake_translate(state):
        state["suggested_translation"] = "Новый перевод LLM"
        state["confidence"] = 0.65
        state["llm_detail"] = "mock llm"
        state["trace"] = [{"stage": "translate_suggest", "ok": True}]
        return state

    session = AsyncMock()
    patches = _patch_repos(mock_graph_repo, mock_word_repo)
    with patches[0], patches[1], patches[2], patches[3], patch(
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


@pytest.mark.graph
@pytest.mark.asyncio
async def test_graph_grep_whole_sentence_auto(mock_graph_repo, mock_word_repo):
    """Grep 整句唯一命中 → retrieval_method=grep、auto_approved。"""
    mock_graph_repo.find_exact.return_value = None
    mock_graph_repo.find_fuzzy.return_value = []
    mock_word_repo.list_distinct_words.return_value = ["按钮"]
    mock_word_repo.find_by_word.return_value = [
        SimpleNamespace(word="按钮", translate="Кнопка", comment="")
    ]

    session = AsyncMock()
    patches = _patch_repos(mock_graph_repo, mock_word_repo)
    with patches[0], patches[1], patches[2], patches[3]:
        final = await PreTranslateGraph().run(
            source_text="按钮",
            target_lang="俄文",
            department=None,
            confidence_threshold=0.8,
            entry_comment="",
            session=session,
        )

    assert final["retrieval_method"] == "grep"
    assert final["exact_hit"] is True
    assert final["review_status"] == "auto_approved"
    assert final["suggested_translation"] == "Кнопка"
    assert final["similar_terms"][0]["retrieval_source"] == "grep"


@pytest.mark.graph
@pytest.mark.asyncio
async def test_graph_decomposed_word_level(mock_graph_repo, mock_word_repo):
    """Grep 词级命中 + 高 coverage → decomposed + compose_suggest auto_approved。"""
    mock_graph_repo.find_exact.return_value = None
    mock_graph_repo.find_fuzzy.return_value = []

    async def find_by_word(word, **kwargs):
        if word == "文件":
            return [SimpleNamespace(word="文件", translate="File", comment="")]
        if word == "/":
            return [SimpleNamespace(word="/", translate="/", comment="")]
        if word == "系统":
            return [SimpleNamespace(word="系统", translate="System", comment="")]
        return []

    mock_word_repo.find_by_word = AsyncMock(side_effect=find_by_word)

    async def fake_compose(state):
        state["suggested_translation"] = "File System"
        state["confidence"] = 0.88
        state["llm_detail"] = "mock compose"
        state["trace"] = [{"stage": "compose_suggest", "ok": True}]
        return state

    session = AsyncMock()
    patches = _patch_repos(mock_graph_repo, mock_word_repo)
    with patches[0], patches[1], patches[2], patches[3], patch(
        "app.graph.pre_translate.builder.compose_suggest_node",
        fake_compose,
    ):
        final = await PreTranslateGraph().run(
            source_text="文件/系统",
            target_lang="英文",
            department=None,
            confidence_threshold=0.8,
            entry_comment="",
            session=session,
        )

    assert final["translation_source"] == "hybrid"
    assert final["retrieval_method"] == "decomposed"
    assert final["suggested_translation"] == "File System"
    assert final["decomposed_translation"] == "File/System"
    assert final["coverage"] >= 0.85
    assert final["review_status"] == "auto_approved"


@pytest.mark.graph
@pytest.mark.asyncio
async def test_graph_decomposed_low_coverage_llm_fallback(mock_graph_repo, mock_word_repo):
    """词级 coverage 不足 → 回退 LLM。"""
    mock_graph_repo.find_exact.return_value = None
    mock_graph_repo.find_fuzzy.return_value = []

    async def find_by_word(word, **kwargs):
        if word == "文件":
            return [SimpleNamespace(word="文件", translate="File", comment="")]
        return []

    mock_word_repo.find_by_word = AsyncMock(side_effect=find_by_word)

    async def fake_translate(state):
        state["suggested_translation"] = "File and system resources"
        state["confidence"] = 0.65
        state["llm_detail"] = "mock llm after low coverage"
        state["trace"] = [{"stage": "translate_suggest", "ok": True}]
        return state

    session = AsyncMock()
    patches = _patch_repos(mock_graph_repo, mock_word_repo)
    with patches[0], patches[1], patches[2], patches[3], patch(
        "app.graph.pre_translate.builder.translate_suggest_node",
        fake_translate,
    ):
        final = await PreTranslateGraph().run(
            source_text="文件与系统资源的定义",
            target_lang="英文",
            department=None,
            confidence_threshold=0.8,
            entry_comment="",
            session=session,
        )

    assert final["translation_source"] == "hybrid"
    assert final["suggested_translation"] == "File and system resources"
    assert (final.get("coverage") or 0) < 0.85
    assert final["review_status"] == "needs_human"
