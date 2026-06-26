"""LangGraph 可视化工具 — Mermaid PNG 导出。"""

from __future__ import annotations


def render_graph_png(compiled_graph) -> bytes:
    """将 LangGraph 编译图导出为 Mermaid PNG 字节流。

    Args:
        compiled_graph: ``builder.compile()`` 返回的已编译图对象。

    Returns:
        PNG 图像二进制数据。
    """
    return compiled_graph.get_graph().draw_mermaid_png()
