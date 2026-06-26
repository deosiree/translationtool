"""术语学习 LangGraph 工作流的状态定义。"""

from typing import Optional, Literal

from langgraph.graph import add_messages
from typing_extensions import TypedDict, Annotated


class TermState(TypedDict):
    """术语学习状态图在各节点间传递的共享状态。

    每个字段由某一个或多个节点写入或读取。
    """

    # ── 输入 ──
    source_text: str
    """待检查的中文词条原文。"""

    context: Optional[str]
    """可选上下文（整句、产品域、界面位置等），用于消歧。"""

    # ── 术语发现 ──
    matched_term: Optional[str]
    """若在术语库中命中，则为已有译文。"""

    match_confidence: Optional[float]
    """匹配置信度（1.0 = 精确匹配，<1.0 = 模糊匹配）。"""

    is_new_term: bool
    """是否为术语库中尚未收录的新词。"""

    # ── LLM 建议 ──
    suggested_translation: Optional[str]
    """LLM 提出的译文（通常仅在新词场景下赋值）。"""

    llm_reasoning: Optional[str]
    """LLM 或规则节点的说明 / 推理摘要。"""

    # ── 人工审核 ──
    review_status: Literal["pending", "approved", "rejected"]
    """人工审核结果：待审 / 通过 / 拒绝。"""

    review_comment: Optional[str]
    """审核人填写的备注。"""

    # ── 错误处理 ──
    error: Optional[str]
    """任一节点失败时的错误信息。"""

    # ── 路由 ──
    next_node: Optional[str]
    """条件边使用的内部路由提示（下一节点名）。"""
