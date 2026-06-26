"""功能节点：retrieve_similar — 术语库 exact + fuzzy 检索。"""

from __future__ import annotations

from langgraph.types import RunnableConfig

from app.graph.pre_translate.state import PreTranslateState
from app.repository.term_repo import TermRepository


async def retrieve_similar_node(
    state: PreTranslateState,
    config: RunnableConfig,
) -> PreTranslateState:
    """查术语库，写入 retrieval_method / similar_terms / 候选译文（exact 直出）。"""
    session = config["configurable"]["session"]
    repo = TermRepository(session)
    source_entry = state["source_text"]
    target_lang = state.get("target_lang")
    department = state.get("department")

    exact = await repo.find_exact(source_entry, target_lang, department)
    if exact and exact.translate:
        state["retrieval_method"] = "exact"
        state["retrieval_confidence"] = 1.0
        state["confidence"] = 1.0
        state["exact_hit"] = True
        state["fuzzy_hit"] = False
        state["suggested_translation"] = exact.translate
        state["similar_terms"] = [
            {
                "entry": exact.entry or source_entry,
                "translate": exact.translate,
                "score": 1.0,
            }
        ]
        state["trace"] = [{"stage": "retrieve_similar", "retrieval_method": "exact"}]
        return state

    fuzzy_matches = await repo.find_fuzzy(
        source_entry, target_lang, department, limit=5
    )
    raw_similar: list[dict] = []
    for match in fuzzy_matches:
        if not match.translate:
            continue
        raw_similar.append(
            {
                "entry": match.entry or "",
                "translate": match.translate,
                "entry_raw": match.entry or "",
            }
        )

    state["exact_hit"] = False
    state["similar_terms"] = raw_similar
    if raw_similar:
        state["retrieval_method"] = "fuzzy"
        state["fuzzy_hit"] = True
    else:
        state["retrieval_method"] = "none"
        state["fuzzy_hit"] = False
        state["retrieval_confidence"] = 0.0
        state["confidence"] = 0.0
        state["suggested_translation"] = None
        state["similar_terms"] = []

    state["trace"] = [
        {"stage": "retrieve_similar", "retrieval_method": state["retrieval_method"]}
    ]
    return state
