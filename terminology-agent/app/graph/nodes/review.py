"""Node: Review — placeholder that sets review_status to "pending" for human review."""

from app.graph.state import TermState


async def review_node(state: TermState) -> TermState:
    """Mark the suggested translation as pending human review.

    The actual review happens via the REST API (POST /agent/term-learning/{id}/review).
    This node simply transitions the state so the workflow can persist and wait.
    """
    state["review_status"] = "pending"

    # If no suggestion was generated (e.g. LLM failure), mark as error
    if state.get("error") or state.get("suggested_translation") is None:
        state["review_status"] = "rejected"
        state["error"] = state.get("error") or "No translation suggestion available."

    state["next_node"] = "update_termstore"
    return state
