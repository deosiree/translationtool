"""Node: Analyze context — extract useful hints from the surrounding context."""

from typing import Optional
import re

from app.graph.state import TermState


# Simple Chinese sentence-segmentation patterns
_SENTENCE_SPLIT = re.compile(r"[。！？\n]")

# Common i18n-related context markers
_CONTEXT_MARKERS = {
    "button": "ui_button",
    "菜单": "ui_menu",
    "提示": "ui_tooltip",
    "标题": "ui_title",
    "标签": "ui_label",
    "表格": "ui_table",
    "错误": "error_message",
    "配置": "config",
    "产品": "product",
    "功能": "feature",
}


def _infer_context_type(context: Optional[str]) -> Optional[str]:
    if not context:
        return None
    for marker, ctype in _CONTEXT_MARKERS.items():
        if marker in context:
            return ctype
    return None


def _extract_keywords(context: Optional[str], source_text: str) -> list[str]:
    """Extract nearby Chinese words that may help disambiguate `source_text`."""
    if not context:
        return []
    # Remove the source_text itself, then grab short tokens around it
    cleaned = context.replace(source_text, "")
    # Keep 2-4 char Chinese sequences as candidate keywords
    tokens = re.findall(r"[\u4e00-\u9fff]{2,4}", cleaned)
    return tokens[:5]


async def analyze_context_node(state: TermState) -> TermState:
    """Analyze the context field and enrich state with hints.

    This node is intentionally rule-based and fast:
      - context_type classifies the UI area (button, menu, etc.)
      - keywords helps the LLM node disambiguate
    """
    context = state.get("context")

    context_type = _infer_context_type(context)
    keywords = _extract_keywords(context, state["source_text"])

    # Store analysis as structured metadata in llm_reasoning (used by next node)
    analysis = {
        "context_type": context_type,
        "keywords": keywords,
        "has_context": context is not None and len(context.strip()) > 0,
    }
    state["llm_reasoning"] = str(analysis)

    state["next_node"] = "llm_suggest"
    return state
