"""功能节点：write_result — 格式化 Agent 说明并写入 audit。"""

from __future__ import annotations

from langgraph.types import RunnableConfig

from app.graph.pre_translate.state import PreTranslateState
from app.graph.pre_translate.domain.translation_source import (
    TranslationSource,
    format_agent_reasoning_with_meta,
)
from app.repository.term_repo import TermRepository


async def write_result_node(
    state: PreTranslateState,
    config: RunnableConfig,
) -> PreTranslateState:
    """组装 llm_reasoning；needs_human 时持久化 term_agent_audit。"""
    source_value = state.get("translation_source") or TranslationSource.TERM.value
    try:
        source = TranslationSource(source_value)
    except ValueError:
        source = TranslationSource.TERM

    detail = state.get("llm_detail")
    reasoning, audit_fallback = format_agent_reasoning_with_meta(source, detail)
    state["llm_reasoning"] = reasoning

    if state.get("review_status") == "needs_human":
        session = config["configurable"]["session"]
        repo = TermRepository(session)
        await repo.create_pretranslate_audit(
            entry_info_id=state.get("entry_info_id"),
            task_id=state.get("task_id"),
            task_name=state.get("task_name"),
            product_name=state.get("product_name"),
            target_lang=state.get("target_lang"),
            department=state.get("department"),
            source_text=state["source_text"],
            entry_comment=state.get("entry_comment"),
            suggested_translation=state.get("suggested_translation"),
            confidence=state.get("confidence"),
            similar_terms=state.get("similar_terms") or [],
            retrieval_method=state.get("retrieval_method"),
            llm_reasoning=reasoning,
        )

    state["trace"] = [
        {
            "stage": "write_result",
            "review_status": state.get("review_status"),
            "translation_source": source_value,
            "audit_suggest_fallback": audit_fallback,
        }
    ]
    return state
