"""节点：Analyze Context — 从上下文中提取有助于消歧的线索。"""

from typing import Optional
import re

from app.graph.state import TermState


# 常见 i18n 上下文关键词 → 界面类型
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
    """根据上下文关键词推断界面 / 业务类型。"""
    if not context:
        return None
    for marker, ctype in _CONTEXT_MARKERS.items():
        if marker in context:
            return ctype
    return None


def _extract_keywords(context: Optional[str], source_text: str) -> list[str]:
    """从上下文中提取词条附近的短中文词，辅助 LLM 消歧。"""
    if not context:
        return []
    cleaned = context.replace(source_text, "")
    tokens = re.findall(r"[\u4e00-\u9fff]{2,4}", cleaned)
    return tokens[:5]


async def analyze_context_node(state: TermState) -> TermState:
    """分析 ``context`` 字段，向状态中写入结构化线索。

    本节点采用规则引擎（快、无 LLM 成本）：
      - context_type：界面区域分类（按钮、菜单等）
      - keywords：供下一节点 llm_suggest 消歧用
    """
    context = state.get("context")

    context_type = _infer_context_type(context)
    keywords = _extract_keywords(context, state["source_text"])

    analysis = {
        "context_type": context_type,
        "keywords": keywords,
        "has_context": context is not None and len(context.strip()) > 0,
    }
    state["llm_reasoning"] = str(analysis)

    state["next_node"] = "llm_suggest"
    return state
