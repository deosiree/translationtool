"""术语学习图 — 条件路由函数。"""

from typing import Literal

from langgraph.graph import END

from app.graph.state import TermState


def route_after_discover(state: TermState) -> Literal["analyze_context", END]:
    """根据术语发现结果决定 discover 节点之后的条件路由。

    Args:
        state: 当前工作流状态，``is_new_term`` 表示是否为术语库新词。

    Returns:
        新词 → ``"analyze_context"`` 继续分析；已有词 → ``END`` 结束。
    """
    if state.get("is_new_term"):
        return "analyze_context"
    return END
