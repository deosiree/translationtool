"""功能节点：decompose_compose — jieba 切界 + lexeme lookup + trace compose + coverage。"""

from __future__ import annotations

from langgraph.types import RunnableConfig

from app.graph.pre_translate.constants import COVERAGE_FLOOR
from app.graph.pre_translate.nodes.features.io.lookup_lexemes import lookup_lexeme_spans
from app.graph.pre_translate.state import PreTranslateState
from app.graph.pre_translate.utils.compose import compose_translation
from app.graph.pre_translate.utils.coverage import compute_coverage, meets_coverage_floor
from app.graph.pre_translate.utils.decompose import decompose_to_spans
from app.repository.word_repo import WordRepository


async def decompose_compose_node(
    state: PreTranslateState,
    config: RunnableConfig,
) -> PreTranslateState:
    """hybrid 路径：jieba 切界 + 词片 lookup + trace 拼装；coverage 达标则待 LLM 拼装。

    Args:
        state: 须含 ``source_text`` / ``target_lang`` / ``entry_comment``。
        config: ``configurable.session`` 注入 DB。

    Returns:
        写入 ``spans``、``coverage``、``decomposed_translation``；
        达标时更新 ``retrieval_method``，最终译文由 ``compose_suggest`` 写入。
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

    spans = decompose_to_spans(source_text)
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

    state["spans"] = [s.to_dict() for s in enriched]
    state["coverage"] = coverage
    state["decomposed_translation"] = decomposed

    if meets_coverage_floor(coverage) and decomposed.strip():
        state["retrieval_method"] = "decomposed"
        state["llm_detail"] = f"词片覆盖 coverage={coverage:.0%}，待 LLM 受约束拼装"
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
