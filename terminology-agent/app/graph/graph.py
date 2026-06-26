"""术语学习 LangGraph 状态图定义。

流程：
  discover ──👉 (已存在?) ──是──➤ [结束]
                    │
                    否
                    ▼
            analyze_context ──➤ llm_suggest ──➤ review ──➤ update_termstore ──➤ 结束
"""

from langgraph.graph import StateGraph, END
from sqlalchemy.ext.asyncio import AsyncSession

from app.graph.nodes import (
    analyze_context_node,
    discover_node,
    llm_suggest_node,
    review_node,
    update_termstore_node,
)
from app.graph.routes import route_after_discover
from app.graph.state import TermState


class TermLearningGraph:
    """术语学习 LangGraph 工作流封装。

    负责注册节点、连边、编译图，并提供 ``run()`` 一次性执行入口。
    """

    def __init__(self):
        builder = StateGraph(TermState)

        builder.add_node("discover", discover_node)
        builder.add_node("analyze_context", analyze_context_node)
        builder.add_node("llm_suggest", llm_suggest_node)
        builder.add_node("review", review_node)
        builder.add_node("update_termstore", update_termstore_node)

        builder.set_entry_point("discover")

        builder.add_conditional_edges("discover", route_after_discover)
        builder.add_edge("analyze_context", "llm_suggest")
        builder.add_edge("llm_suggest", "review")
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
        """执行完整的术语学习工作流。

        Args:
            source_text: 待检查的中文词条。
            context: 可选上下文，供 analyze_context / llm_suggest 消歧。
            audit_id: 已创建的 term_agent_audit 记录 id。
            session: 异步 SQLAlchemy 会话，供 discover / update_termstore 访问数据库。

        Returns:
            所有节点执行完毕后的最终 TermState。
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

        config = {
            "configurable": {
                "session": session,
                "audit_id": audit_id,
            }
        }

        final_state = await self.graph.ainvoke(initial_state, config)
        return final_state
