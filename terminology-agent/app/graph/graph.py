"""LangGraph state graph definition for the terminology learning workflow.

Flow:
  discover ──👉 (found?) ──yes──➤ [end]
                    │
                    no
                    ▼
            analyze_context ──➤ llm_suggest ──➤ review ──➤ update_termstore ──➤ end
"""

from typing import Literal

from langgraph.graph import StateGraph, END
from sqlalchemy.ext.asyncio import AsyncSession

from app.graph.state import TermState
from app.graph.nodes.discover import discover_node
from app.graph.nodes.analyze_context import analyze_context_node
from app.graph.nodes.llm_suggest import llm_suggest_node
from app.graph.nodes.review import review_node
from app.graph.nodes.update_termstore import update_termstore_node


def _route_after_discover(state: TermState) -> Literal["analyze_context", END]:
    """If a match was found, end the workflow; otherwise continue."""
    if state.get("is_new_term"):
        return "analyze_context"
    return END


class TermLearningGraph:
    """Encapsulates the LangGraph state graph for term learning."""

    def __init__(self):
        builder = StateGraph(TermState)

        # Register nodes
        builder.add_node("discover", discover_node)
        builder.add_node("analyze_context", analyze_context_node)
        builder.add_node("llm_suggest", llm_suggest_node)
        builder.add_node("review", review_node)
        builder.add_node("update_termstore", update_termstore_node)

        # Entry point
        builder.set_entry_point("discover")

        # Edges
        builder.add_conditional_edges("discover", _route_after_discover)
        builder.add_edge("analyze_context", "llm_suggest")
        builder.add_edge("llm_suggest", "review")
        # review always goes to update_termstore to persist state
        builder.add_edge("review", "update_termstore")
        builder.add_edge("update_termstore", END)

        self.graph = builder.compile()

    async def run(
        self,
        *,
        source_text: str,
        context: str | None = None,
        audit_id: str,
        session: AsyncSession,
    ) -> TermState:
        """Execute the terminology-learning workflow.

        Args:
            source_text: The Chinese term to check.
            context: Optional surrounding text for disambiguation.
            audit_id: ID of the persisted audit record.
            session: Async SQLAlchemy session for DB access.

        Returns:
            The final TermState after all nodes have run.
        """
        initial_state: TermState = {
            "source_text": source_text,
            "context": context,
            "matched_term": None,
            "match_confidence": None,
            "is_new_term": False,
            "suggested_translation": None,
            "llm_reasoning": None,
            "review_status": "pending",
            "review_comment": None,
            "error": None,
            "next_node": None,
        }

        # Pass session + audit_id via RunnableConfig so nodes that need DB
        # access (discover_node, update_termstore_node) can read them.
        config = {
            "configurable": {
                "session": session,
                "audit_id": audit_id,
            }
        }

        final_state = await self.graph.ainvoke(initial_state, config)
        return final_state
