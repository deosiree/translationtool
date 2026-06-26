"""节点：Update Term Store — 将审核结果写回 term_agent_audit 表。

当前 MVP 仅更新 ``term_agent_audit`` 记录。
后续可增强为审核通过后直接写入 ``t_translate``。
"""

from langgraph.types import RunnableConfig

from app.graph.state import TermState
from app.repository.term_repo import TermRepository


async def update_termstore_node(state: TermState, config: RunnableConfig) -> TermState:
    """把最终状态（通过 / 拒绝 / 待审）持久化到数据库。"""
    session = config["configurable"]["session"]
    audit_id = config["configurable"]["audit_id"]
    repo = TermRepository(session)

    update_fields = {
        "matched_term": state.get("matched_term"),
        "match_confidence": state.get("match_confidence"),
        "is_new_term": state.get("is_new_term", False),
        "suggested_translation": state.get("suggested_translation"),
        "llm_reasoning": state.get("llm_reasoning"),
        "review_status": state.get("review_status", "pending"),
        "error": state.get("error"),
    }

    await repo.update_audit(audit_id, **update_fields)
    return state
