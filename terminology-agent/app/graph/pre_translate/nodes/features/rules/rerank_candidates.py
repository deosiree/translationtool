"""功能节点：rerank_candidates — 模糊候选重排与置信度计算。"""

from __future__ import annotations

from app.graph.pre_translate.utils.retrieval import fuzzy_confidence, similarity, strip_placeholders
from app.graph.pre_translate.state import PreTranslateState


async def rerank_candidates_node(state: PreTranslateState) -> PreTranslateState:
    """对 fuzzy 候选做相似度重排；exact / none 路径透传。"""
    if state.get("exact_hit"):
        state["trace"] = [{"stage": "rerank_candidates", "skipped": True}]
        return state

    raw = state.get("similar_terms") or []
    if not raw:
        state["trace"] = [{"stage": "rerank_candidates", "candidates": 0}]
        return state

    source_entry = state["source_text"]
    core = strip_placeholders(source_entry)
    ranked: list[dict] = []
    best_score = 0.0
    best_translate: str | None = None

    for item in raw:
        entry_text = item.get("entry_raw") or item.get("entry") or ""
        score = similarity(core, strip_placeholders(entry_text))
        ranked.append(
            {
                "entry": item.get("entry") or entry_text,
                "translate": item.get("translate"),
                "score": round(score, 3),
            }
        )
        if score > best_score:
            best_score = score
            best_translate = item.get("translate")

    ranked.sort(key=lambda x: x["score"], reverse=True)
    state["similar_terms"] = ranked[:3]

    if best_translate:
        conf = fuzzy_confidence(best_score)
        state["retrieval_confidence"] = conf
        state["confidence"] = conf
        state["suggested_translation"] = best_translate
    else:
        state["retrieval_confidence"] = 0.0
        state["confidence"] = 0.0
        state["suggested_translation"] = None

    state["trace"] = [
        {
            "stage": "rerank_candidates",
            "best_score": round(best_score, 3),
            "confidence": state.get("confidence"),
        }
    ]
    return state
