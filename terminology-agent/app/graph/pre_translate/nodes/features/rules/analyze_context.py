"""功能节点：Analyze Context — 从上下文中提取有助于消歧的线索（Phase 3+ 复用）。"""

from __future__ import annotations

import re
from typing import Optional

from app.graph.pre_translate.state import PreTranslateState

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
    """从上下文字符串推断 UI/业务类型标签。

    Args:
        context: 原始上下文字符串。

    Returns:
        匹配到的 context_type；无匹配时返回 None。
    """
    if not context:
        return None
    for marker, ctype in _CONTEXT_MARKERS.items():
        if marker in context:
            return ctype
    return None


def _extract_keywords(context: Optional[str], source_text: str) -> list[str]:
    """从上下文中提取与源词条无关的中文关键词。

    Args:
        context: 原始上下文字符串。
        source_text: 源词条，会从 context 中剔除后再分词。

    Returns:
        最多 5 个 2~4 字中文 token。
    """
    if not context:
        return []
    cleaned = context.replace(source_text, "")
    tokens = re.findall(r"[\u4e00-\u9fff]{2,4}", cleaned)
    return tokens[:5]


async def analyze_context_node(state: PreTranslateState) -> PreTranslateState:
    """规则引擎分析 context，写入结构化线索（当前主图未接入）。"""
    context = state.get("context")  # type: ignore[attr-defined]

    context_type = _infer_context_type(context)
    keywords = _extract_keywords(context, state["source_text"])

    analysis = {
        "context_type": context_type,
        "keywords": keywords,
        "has_context": context is not None and len(str(context).strip()) > 0,
    }
    state["llm_detail"] = str(analysis)
    return state
