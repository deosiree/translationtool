"""功能节点：assess_route — 置信度阈值分流 auto_approved / needs_human。"""

from __future__ import annotations

from app.graph.pre_translate.state import PreTranslateState


async def assess_route_node(state: PreTranslateState) -> PreTranslateState:
    """对比 confidence 与用户阈值，写入 review_status。"""
    threshold = state.get("confidence_threshold") or 0.8
    confidence = state.get("confidence") or 0.0
    has_translation = bool(state.get("suggested_translation"))

    if has_translation and confidence >= threshold:
        review_status = "auto_approved"
    else:
        review_status = "needs_human"

    state["review_status"] = review_status
    state["trace"] = [
        {
            "stage": "assess_route",
            "confidence": confidence,
            "threshold": threshold,
            "review_status": review_status,
        }
    ]
    return state
