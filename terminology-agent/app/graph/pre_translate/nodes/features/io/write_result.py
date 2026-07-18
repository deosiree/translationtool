"""功能节点：write_result — 格式化 Agent 说明并写入 audit / 切分轨迹。"""

from __future__ import annotations

from langgraph.types import RunnableConfig

from app.graph.pre_translate.state import PreTranslateState
from app.graph.pre_translate.domain.translation_source import (
    TranslationSource,
    format_agent_reasoning_with_meta,
)
from app.graph.pre_translate.utils.segment_trace import (
    build_segment_trace,
    used_segmentation,
)
from app.repository.entry_repo import EntryRepository
from app.repository.term_repo import TermRepository


async def write_result_node(
    state: PreTranslateState,
    config: RunnableConfig,
) -> PreTranslateState:
    """组装 llm_reasoning / segment_trace；needs_human 或走过切分时落库。"""
    source_value = state.get("translation_source") or TranslationSource.TERM.value
    try:
        source = TranslationSource(source_value)
    except ValueError:
        source = TranslationSource.TERM

    detail = state.get("llm_detail")
    reasoning, audit_fallback = format_agent_reasoning_with_meta(source, detail)
    state["llm_reasoning"] = reasoning

    segment_trace = build_segment_trace(state)
    state["segment_trace"] = segment_trace

    review_status = state.get("review_status")
    should_persist = review_status == "needs_human" or (
        used_segmentation(state) and segment_trace is not None
    )

    if should_persist:
        session = config["configurable"]["session"]
        repo = TermRepository(session)
        audit_status = (
            "pending" if review_status == "needs_human" else (review_status or "auto_approved")
        )
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
            segment_trace=segment_trace,
            review_status=audit_status,
        )
        entry_info_id = state.get("entry_info_id")
        if entry_info_id and segment_trace is not None:
            entry_repo = EntryRepository(session)
            await entry_repo.update_entry_segment_trace(entry_info_id, segment_trace)
            await entry_repo.commit()

    state["trace"] = [
        {
            "stage": "write_result",
            "review_status": state.get("review_status"),
            "translation_source": source_value,
            "audit_suggest_fallback": audit_fallback,
            "segment_persisted": bool(should_persist and segment_trace),
        }
    ]
    return state
