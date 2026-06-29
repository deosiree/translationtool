"""意图节点：resolve_translation_source — 判定 term / llm / hybrid。"""

from __future__ import annotations

from app.graph.pre_translate.constants import FUZZY_AUTO_FLOOR
from app.graph.pre_translate.state import PreTranslateState
from app.graph.pre_translate.domain.translation_source import TranslationSource


async def resolve_translation_source_node(state: PreTranslateState) -> PreTranslateState:
    """读检索结果，写入 ``translation_source`` 与 ``llm_detail``。

    分支：RAG/Grep 冲突 → llm；exact（含 grep 整句）→ term；
    fuzzy 高置信 → term；hybrid（RAG+Grep 词级）→ hybrid；否则 → llm。

    Args:
        state: 须含 ``retrieval_method`` / ``exact_hit`` / ``grep_hits`` 等检索字段。

    Returns:
        写入 ``translation_source``、``llm_detail`` 与 trace 的 state。
    """
    trace_entry = {
        "stage": "resolve_translation_source",
        "retrieval_method": state.get("retrieval_method"),
        "retrieval_confidence": state.get("retrieval_confidence"),
    }

    if state.get("rag_grep_conflict"):
        state["translation_source"] = TranslationSource.LLM.value
        state["llm_detail"] = "RAG 与 Grep 整句译法冲突，需人工裁定"
        trace_entry["translation_source"] = TranslationSource.LLM.value
    elif state.get("exact_hit"):
        state["translation_source"] = TranslationSource.TERM.value
        method = state.get("retrieval_method")
        if method == "grep":
            state["llm_detail"] = "Grep 关键字整句命中 term_word"
        elif method == "hybrid":
            state["llm_detail"] = "RAG 与 Grep 整句一致"
        else:
            state["llm_detail"] = "精确匹配术语库"
        trace_entry["translation_source"] = TranslationSource.TERM.value
    elif state.get("fuzzy_hit") and (state.get("retrieval_confidence") or 0) >= FUZZY_AUTO_FLOOR:
        state["translation_source"] = TranslationSource.TERM.value
        state["llm_detail"] = "术语库模糊匹配，整句取自术语"
        trace_entry["translation_source"] = TranslationSource.TERM.value
    elif state.get("retrieval_method") == "hybrid":
        state["translation_source"] = TranslationSource.HYBRID.value
        grep_n = len(state.get("grep_hits") or [])
        state["llm_detail"] = f"RAG 模糊 + Grep 命中 {grep_n} 条，LLM 整句机翻"
        trace_entry["translation_source"] = TranslationSource.HYBRID.value
    elif state.get("grep_hit") and state.get("retrieval_method") == "grep":
        state["translation_source"] = TranslationSource.HYBRID.value
        grep_n = len(state.get("grep_hits") or [])
        state["llm_detail"] = f"Grep 词级命中 {grep_n} 条，术语库无整句，LLM 机翻"
        trace_entry["translation_source"] = TranslationSource.HYBRID.value
    else:
        state["translation_source"] = TranslationSource.LLM.value
        if state.get("retrieval_method") == "none":
            state["llm_detail"] = "术语库未命中，LLM 机翻生成"
        else:
            state["llm_detail"] = "模糊匹配置信度不足，LLM 整句机翻"
        trace_entry["translation_source"] = TranslationSource.LLM.value

    state["trace"] = [trace_entry]
    return state
