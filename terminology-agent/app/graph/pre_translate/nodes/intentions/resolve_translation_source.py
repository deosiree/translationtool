"""意图节点：resolve_translation_source — 判定 term / llm / hybrid。"""

from __future__ import annotations

from app.graph.pre_translate.constants import FUZZY_AUTO_FLOOR
from app.graph.pre_translate.state import PreTranslateState
from app.graph.pre_translate.domain.translation_source import TranslationSource


async def resolve_translation_source_node(state: PreTranslateState) -> PreTranslateState:
    """读检索结果，写入 translation_source（P1 仅 term / llm）。"""
    trace_entry = {
        "stage": "resolve_translation_source",
        "retrieval_method": state.get("retrieval_method"),
        "retrieval_confidence": state.get("retrieval_confidence"),
    }

    if state.get("exact_hit"):
        state["translation_source"] = TranslationSource.TERM.value
        state["llm_detail"] = "精确匹配术语库"
        trace_entry["translation_source"] = TranslationSource.TERM.value
    elif state.get("fuzzy_hit") and (state.get("retrieval_confidence") or 0) >= FUZZY_AUTO_FLOOR:
        state["translation_source"] = TranslationSource.TERM.value
        state["llm_detail"] = "术语库模糊匹配，整句取自术语"
        trace_entry["translation_source"] = TranslationSource.TERM.value
    else:
        state["translation_source"] = TranslationSource.LLM.value
        if state.get("retrieval_method") == "none":
            state["llm_detail"] = "术语库未命中，LLM 机翻生成"
        else:
            state["llm_detail"] = "模糊匹配置信度不足，LLM 整句机翻"
        trace_entry["translation_source"] = TranslationSource.LLM.value

    state["trace"] = [trace_entry]
    return state
