"""功能节点：retrieve_similar — RAG ∥ Grep 并行检索 + merge。

Phase 3a：在单节点内 ``asyncio.gather`` 并行执行 RAG（t_translate）与 Grep（term_word），
再经 ``merge_candidates`` 合并候选并写入 ``PreTranslateState``。

并发注意：SQLAlchemy ``AsyncSession`` 不可跨协程共享；RAG/Grep 各分支使用独立
``AsyncSessionLocal()``，请求级 session 仅用于 ``_resolve_comment`` 与后续写节点。
"""

from __future__ import annotations

import asyncio

from langgraph.types import RunnableConfig

from app.graph.pre_translate.constants import ENABLE_GREP_RETRIEVE
from app.graph.pre_translate.state import PreTranslateState
from app.graph.pre_translate.utils.entry_context import resolve_entry_comment
from app.graph.pre_translate.utils.grep_retrieve import GrepRetrieveResult, grep_retrieve_candidates
from app.graph.pre_translate.utils.merge_candidates import merge_candidates, RagRetrieveResult
from app.graph.pre_translate.utils.rag_retrieve import rag_retrieve
from app.models.database import AsyncSessionLocal
from app.models.term import EntryInfo
from app.repository.term_repo import TermRepository
from app.repository.word_repo import WordRepository
from app.shared.term_word.extract import unique_words


async def _resolve_comment(state: PreTranslateState, session) -> str:
    """解析 Grep 消歧 comment：优先 state.entry_comment，否则查 entry_info。

    Args:
        state: 当前图状态，含 ``entry_comment`` / ``entry_info_id``。
        session: 异步 DB 会话，用于 ``EntryInfo`` 回退查询。

    Returns:
        规范化后的 comment 字符串；无则 ``''``（与 term_word 建库一致）。
    """
    direct = state.get("entry_comment")
    if direct is not None and str(direct).strip():
        return resolve_entry_comment(entry_comment=str(direct))

    entry_info_id = state.get("entry_info_id")
    if entry_info_id:
        row = await session.get(EntryInfo, entry_info_id)
        if row is not None:
            raw = getattr(row, "comment", None)
            if isinstance(raw, str):
                return resolve_entry_comment(entry_info_comment=raw)
    return ""


async def _grep_retrieve(
    session,
    *,
    source_text: str,
    target_lang: str | None,
    department: str | None,
    entry_comment: str,
) -> GrepRetrieveResult:
    """Grep 线检索：jieba 切词 + 批量 ``WordRepository.find_by_word``。

    Args:
        session: 异步 DB 会话。
        source_text: 待译词条原文。
        target_lang: 目标语种；空则跳过 Grep。
        department: 部门过滤（非消歧键）。
        entry_comment: 消歧 comment，传入 find_by_word。

    Returns:
        ``GrepRetrieveResult``；``ENABLE_GREP_RETRIEVE=False`` 或无语种时为空结果。
    """
    if not ENABLE_GREP_RETRIEVE or not target_lang:
        return GrepRetrieveResult([], False, None, [])

    word_repo = WordRepository(session)

    async def lookup(word: str):
        return await word_repo.find_by_word(
            word,
            target_lang=target_lang,
            comment=entry_comment,
            department=department,
        )

    stripped = source_text.strip()
    words_to_check: set[str] = {stripped}
    for w in unique_words(stripped):
        words_to_check.add(w)

    lookup_cache: dict[str, list] = {}
    for w in words_to_check:
        lookup_cache[w] = await lookup(w)

    def sync_lookup(word: str) -> list:
        return lookup_cache.get(word, [])

    return grep_retrieve_candidates(
        source_text=source_text,
        lookup=sync_lookup,
    )


async def _rag_branch(
    *,
    source_text: str,
    target_lang: str | None,
    department: str | None,
) -> RagRetrieveResult:
    """RAG 分支：独立 AsyncSession，供 gather 并行调用。"""
    async with AsyncSessionLocal() as session:
        return await rag_retrieve(
            TermRepository(session),
            source_text=source_text,
            target_lang=target_lang,
            department=department,
        )


async def _grep_branch(
    *,
    source_text: str,
    target_lang: str | None,
    department: str | None,
    entry_comment: str,
) -> GrepRetrieveResult:
    """Grep 分支：独立 AsyncSession，供 gather 并行调用。"""
    async with AsyncSessionLocal() as session:
        return await _grep_retrieve(
            session,
            source_text=source_text,
            target_lang=target_lang,
            department=department,
            entry_comment=entry_comment,
        )


async def retrieve_similar_node(
    state: PreTranslateState,
    config: RunnableConfig,
) -> PreTranslateState:
    """RAG + Grep 并行检索，merge 后写入 state。

    Args:
        state: 输入含 ``source_text`` / ``target_lang`` / ``department`` 等。
        config: LangGraph 配置；``configurable.session`` 注入各 I/O 节点。

    Returns:
        更新 ``retrieval_method``、``similar_terms``（含 ``retrieval_source``）、
        ``grep_hits``、``rag_grep_conflict`` 等检索字段的 state。
    """
    session = config["configurable"]["session"]
    source_entry = state["source_text"]
    target_lang = state.get("target_lang")
    department = state.get("department")
    entry_comment = await _resolve_comment(state, session)
    state["entry_comment"] = entry_comment

    rag_result, grep_result = await asyncio.gather(
        _rag_branch(
            source_text=source_entry,
            target_lang=target_lang,
            department=department,
        ),
        _grep_branch(
            source_text=source_entry,
            target_lang=target_lang,
            department=department,
            entry_comment=entry_comment,
        ),
    )

    merged = merge_candidates(rag_result, grep_result)

    state["retrieval_method"] = merged.retrieval_method
    state["exact_hit"] = merged.exact_hit
    state["fuzzy_hit"] = merged.fuzzy_hit
    state["grep_hit"] = merged.grep_hit
    state["grep_hits"] = merged.grep_hits
    state["rag_grep_conflict"] = merged.rag_grep_conflict
    state["similar_terms"] = merged.similar_terms
    state["suggested_translation"] = merged.suggested_translation
    state["retrieval_confidence"] = merged.retrieval_confidence
    state["confidence"] = merged.confidence

    if grep_result.ambiguous_words:
        state["error"] = f"Grep 歧义: {', '.join(grep_result.ambiguous_words)}"

    state["trace"] = [
        {
            "stage": "retrieve_similar",
            "retrieval_method": merged.retrieval_method,
            "grep_hit_count": len(merged.grep_hits),
            "rag_grep_conflict": merged.rag_grep_conflict,
        }
    ]
    return state
