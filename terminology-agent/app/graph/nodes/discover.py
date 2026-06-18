"""Node: Discover — check if the term exists in the terminology store."""

from langgraph.types import RunnableConfig

from app.graph.state import TermState
from app.repository.term_repo import TermRepository


async def discover_node(state: TermState, config: RunnableConfig) -> TermState:
    """Query the term store to see whether `source_text` already exists.

    If found:
      - matched_term ← the existing translation
      - is_new_term ← False
      - next_node ← "end" (no need to continue)
    If NOT found:
      - is_new_term ← True
      - next_node ← "analyze_context" (continue the workflow)
    """
    session = config["configurable"]["session"]
    repo = TermRepository(session)
    matches = await repo.find_by_chinese(state["source_text"])

    if matches:
        best = matches[0]
        state["matched_term"] = best.translate
        state["match_confidence"] = 1.0
        state["is_new_term"] = False
        state["next_node"] = "end"
    else:
        state["matched_term"] = None
        state["match_confidence"] = None
        state["is_new_term"] = True
        state["next_node"] = "analyze_context"

    return state
