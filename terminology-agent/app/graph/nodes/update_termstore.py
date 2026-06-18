"""Node: Update Term Store — persist an approved term back to the agent audit table.

In the current MVP, we update the `term_agent_audit` record to reflect the
approved status. A future enhancement can write into `t_translate` directly.
"""

from langgraph.types import RunnableConfig

from app.graph.state import TermState
from app.repository.term_repo import TermRepository


async def update_termstore_node(state: TermState, config: RunnableConfig) -> TermState:
    """Persist the final approved (or rejected) state to the database."""
    session = config["configurable"]["session"]
    audit_id = config["configurable"]["audit_id"]
    repo = TermRepository(session)

    update_fields = {
        "matched_term": state.get("matched_term"),
        "match_confidence": state.get("match_confidence"),
        "is_new_term": state.get("is_new_term", False),
        "suggested_translation": state.get("suggested_translation"),
        "llm_reasoning": state.get("llm_reasoning"),
        "review_status": state.get("review_status", "pending"),
        "error": state.get("error"),
    }

    await repo.update_audit(audit_id, **update_fields)
    return state
