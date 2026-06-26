"""PreTranslate LangGraph 工作流域 — 单条预翻译流水线。"""

from __future__ import annotations

from app.graph.pre_translate.runner import PreTranslateGraph
from app.graph.pre_translate.state import PreTranslateState, TermState

__all__ = [
    "PreTranslateGraph",
    "PreTranslateState",
    "TermState",
]


def build_pre_translate_graph():
    """延迟导入 builder，避免测试收集时的循环依赖。

    Returns:
        ``build_pre_translate_graph()`` 编译后的 LangGraph 对象。
    """
    from app.graph.pre_translate.builder import build_pre_translate_graph as _build

    return _build()
