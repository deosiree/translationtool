"""节点：Review — 将建议译文标记为待人工审核。"""

from app.graph.state import TermState


async def review_node(state: TermState) -> TermState:
    """把 LLM 建议译文置为 ``pending``，等待人工确认。

    实际审核通过 REST API 完成（POST /agent/term-learning/{id}/review）。
    本节点只负责状态流转，便于工作流持久化后挂起等待。
    """
    state["review_status"] = "pending"

    if state.get("error") or state.get("suggested_translation") is None:
        state["review_status"] = "rejected"
        state["error"] = state.get("error") or "No translation suggestion available."

    state["next_node"] = "update_termstore"
    return state
