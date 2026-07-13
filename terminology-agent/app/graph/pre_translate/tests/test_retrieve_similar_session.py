"""retrieve_similar 并发 session 隔离回归测试。"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.graph.pre_translate.nodes.features.io import retrieve_similar
from app.graph.pre_translate.utils.grep_retrieve import GrepRetrieveResult
from app.graph.pre_translate.utils.merge_candidates import RagRetrieveResult


@pytest.mark.unit
@pytest.mark.asyncio
async def test_gather_uses_separate_sessions():
    """RAG/Grep gather 分支应各自创建 AsyncSessionLocal，不共享请求 session。"""
    created_sessions: list[object] = []

    class FakeSession:
        pass

    def fake_session_factory():
        ctx = MagicMock()
        session = FakeSession()
        created_sessions.append(session)
        ctx.__aenter__ = AsyncMock(return_value=session)
        ctx.__aexit__ = AsyncMock(return_value=None)
        return ctx

    rag_result = RagRetrieveResult(
        retrieval_method="none",
        exact_hit=False,
        fuzzy_hit=False,
        suggested_translation=None,
        similar_terms=[],
        retrieval_confidence=0.0,
        confidence=0.0,
    )
    grep_result = GrepRetrieveResult([], False, None, [])

    request_session = AsyncMock()

    with patch.object(
        retrieve_similar,
        "AsyncSessionLocal",
        side_effect=fake_session_factory,
    ), patch.object(
        retrieve_similar,
        "rag_retrieve",
        AsyncMock(return_value=rag_result),
    ) as mock_rag, patch.object(
        retrieve_similar,
        "_grep_retrieve",
        AsyncMock(return_value=grep_result),
    ) as mock_grep:
        state = {
            "source_text": "ADM",
            "target_lang": "英文",
            "department": "通用平台部",
            "entry_comment": "c1",
        }
        config = {"configurable": {"session": request_session}}

        result = await retrieve_similar.retrieve_similar_node(state, config)

    assert len(created_sessions) == 2
    assert created_sessions[0] is not created_sessions[1]
    mock_rag.assert_awaited_once()
    mock_grep.assert_awaited_once()
    rag_session = mock_rag.await_args.args[0]._session
    grep_session = mock_grep.await_args.args[0]
    assert rag_session is created_sessions[0]
    assert grep_session is created_sessions[1]
    assert result["retrieval_method"] == "none"
    assert result["entry_comment"] == "c1"


@pytest.mark.unit
@pytest.mark.asyncio
async def test_retrieve_similar_node_merges_rag_and_grep():
    """gather 后 merge 字段应正确写入 state。"""
    rag_result = RagRetrieveResult(
        retrieval_method="fuzzy",
        exact_hit=False,
        fuzzy_hit=True,
        suggested_translation=None,
        similar_terms=[{"entry": "ADM", "translate": "ADM trans", "score": 0.5}],
        retrieval_confidence=0.0,
        confidence=0.0,
    )
    grep_result = GrepRetrieveResult(
        hits=[],
        whole_sentence_exact=True,
        whole_sentence_translate="Grep trans",
        ambiguous_words=[],
    )

    request_session = AsyncMock()

    with patch.object(
        retrieve_similar,
        "_rag_branch",
        AsyncMock(return_value=rag_result),
    ), patch.object(
        retrieve_similar,
        "_grep_branch",
        AsyncMock(return_value=grep_result),
    ):
        state = {
            "source_text": "ADM",
            "target_lang": "英文",
            "department": "通用平台部",
            "entry_comment": "",
        }
        config = {"configurable": {"session": request_session}}

        result = await retrieve_similar.retrieve_similar_node(state, config)

    assert result["exact_hit"] is True
    assert result["suggested_translation"] == "Grep trans"
    assert result["retrieval_method"] == "grep"
