"""LangGraph State definition for the terminology learning workflow."""

from typing import Optional, Literal

from langgraph.graph import add_messages
from typing_extensions import TypedDict, Annotated


class TermState(TypedDict):
    """State carried through the terminology-learning state graph.

    Each field represents data produced or consumed by one or more nodes.
    """

    # ── Input ──
    source_text: str
    """The source text (Chinese term) to check."""

    context: Optional[str]
    """Optional surrounding context (sentence, product area, etc.)."""

    # ── Discovery ──
    matched_term: Optional[str]
    """The existing English translation if found in the term store."""

    match_confidence: Optional[float]
    """Confidence of the match (1.0 = exact, <1.0 = fuzzy)."""

    is_new_term: bool
    """Whether this is a truly new term not yet in the store."""

    # ── LLM Suggestion ──
    suggested_translation: Optional[str]
    """Translation proposed by the LLM (only set for new terms)."""

    llm_reasoning: Optional[str]
    """Explanation or reasoning from the LLM."""

    # ── Review ──
    review_status: Literal["pending", "approved", "rejected"]
    """Human review outcome."""

    review_comment: Optional[str]
    """Optional comment left by the reviewer."""

    # ── Error handling ──
    error: Optional[str]
    """Error message if any node failed."""

    # ── Routing ──
    next_node: Optional[str]
    """Internal routing hint set by conditional edges."""
