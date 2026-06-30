"""功能节点：decompose_compose — Trie 拆解 + lexeme lookup + 拼装 + coverage。"""

from __future__ import annotations

from langgraph.types import RunnableConfig

from app.graph.pre_translate.constants import COVERAGE_FLOOR, TRIE_CACHE_TTL_SEC
from app.graph.pre_translate.nodes.features.io.lookup_lexemes import lookup_lexeme_spans
from app.graph.pre_translate.state import PreTranslateState
from app.graph.pre_translate.utils.compose import compose_translation
from app.graph.pre_translate.utils.coverage import (
    compute_coverage,
    coverage_to_confidence,
    meets_coverage_floor,
)
from app.graph.pre_translate.utils.decompose import decompose_to_spans
from app.repository.trie_cache import load_trie_for_lang
from app.repository.word_repo import WordRepository


async def decompose_compose_node(
    state: PreTranslateState,
    config: RunnableConfig,
) -> PreTranslateState:
    """hybrid 路径：词级拆解拼装；coverage 达标则写入 decomposed 译文。

    Args:
        state: 须含 ``source_text`` / ``target_lang`` / ``entry_comment``。
        config: ``configurable.session`` 注入 DB。

    Returns:
        写入 ``spans``、``coverage``、``decomposed_translation``；
        达标时更新 ``suggested_translation``、``retrieval_method``、``confidence``。
    """
    session = config["configurable"]["session"]
    source_text = state["source_text"]
    target_lang = state.get("target_lang")
    department = state.get("department")
    entry_comment = state.get("entry_comment") or ""

    if not target_lang:
        state["coverage"] = 0.0
        state["spans"] = []
        state["decomposed_translation"] = None
        state["trace"] = [{"stage": "decompose_compose", "skipped": "no_target_lang"}]
        return state

    trie = await load_trie_for_lang(session, target_lang, ttl_sec=TRIE_CACHE_TTL_SEC)
    spans = decompose_to_spans(source_text, trie)
    word_repo = WordRepository(session)
    enriched = await lookup_lexeme_spans(
        word_repo,
        spans=spans,
        target_lang=target_lang,
        comment=entry_comment,
        department=department,
    )

    decomposed = compose_translation(enriched)
    coverage = compute_coverage(enriched, source_text)
    confidence = coverage_to_confidence(coverage)

    state["spans"] = [s.to_dict() for s in enriched]
    state["coverage"] = coverage
    state["decomposed_translation"] = decomposed

    if meets_coverage_floor(coverage) and decomposed.strip():
        state["suggested_translation"] = decomposed
        state["retrieval_method"] = "decomposed"
        state["confidence"] = confidence
        state["retrieval_confidence"] = confidence
        state["llm_detail"] = f"词级拼装 coverage={coverage:.0%}"
    else:
        state["llm_detail"] = (
            f"词级拼装 coverage={coverage:.0%} 未达 {COVERAGE_FLOOR:.0%}，回退 LLM"
        )

    state["trace"] = [
        {
            "stage": "decompose_compose",
            "coverage": coverage,
            "span_count": len(enriched),
            "decomposed_ok": meets_coverage_floor(coverage),
        }
    ]
    return state
