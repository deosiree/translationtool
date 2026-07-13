"""功能节点：compose_suggest — LLM 受约束拼装（hybrid compose_ok 路径）。"""

from __future__ import annotations

from langchain_core.messages import HumanMessage, SystemMessage

from app.graph.pre_translate.constants import LLM_COMPOSE_CAP, LLM_COMPOSE_FALLBACK_CONF
from app.graph.pre_translate.prompts.compose_suggest import (
    build_compose_suggest_system_prompt,
    build_compose_suggest_user_message,
    spans_from_state,
)
from app.graph.pre_translate.schemas.llm_output import ComposeSuggestLlmOutput
from app.graph.pre_translate.state import PreTranslateState
from app.graph.pre_translate.utils.compose_validate import validate_mandatory_terms
from app.graph.pre_translate.utils.coverage import coverage_to_confidence
from app.graph.pre_translate.utils.llm_json import parse_llm_output
from config.settings import settings


async def compose_suggest_node(state: PreTranslateState) -> PreTranslateState:
    """在词片术语约束下 LLM 拼装自然目标语短语。"""
    decomposed = (state.get("decomposed_translation") or "").strip()
    coverage = state.get("coverage") or 0.0
    spans = spans_from_state(state.get("spans"))

    def apply_fallback(reason: str) -> PreTranslateState:
        state["suggested_translation"] = decomposed or None
        state["confidence"] = LLM_COMPOSE_FALLBACK_CONF
        state["retrieval_confidence"] = LLM_COMPOSE_FALLBACK_CONF
        state["llm_detail"] = reason
        state["trace"] = [{"stage": "compose_suggest", "fallback": True, "reason": reason}]
        return state

    if not decomposed:
        return apply_fallback("无 decomposed_translation，无法拼装")

    api_key = settings.llm_api_key
    if not api_key:
        return apply_fallback("LLM 未配置，使用 trace 拼装结果")

    system_prompt = build_compose_suggest_system_prompt(state.get("target_lang"))
    user_text = build_compose_suggest_user_message(
        source_text=state["source_text"],
        target_lang=state.get("target_lang"),
        coverage=coverage,
        spans=state.get("spans") or [],
        decomposed_translation=decomposed,
    )

    try:
        from langchain_openai import ChatOpenAI

        llm = ChatOpenAI(
            api_key=api_key,
            base_url=settings.llm_base_url,
            model=settings.llm_model,
            temperature=settings.llm_temperature,
        )
        response = await llm.ainvoke(
            [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_text),
            ]
        )
        content = (response.content or "").strip()
        parsed = parse_llm_output(content, ComposeSuggestLlmOutput)
        if parsed is None:
            return apply_fallback("LLM 输出格式或长度不符合 Schema")

        translation = parsed.translation
        reasoning = parsed.reasoning

        ok, missing = validate_mandatory_terms(translation, spans)
        if not translation or not ok:
            miss = ", ".join(missing) if missing else "empty"
            return apply_fallback(f"词片术语校验失败({miss})，回退 trace 拼装")

        conf = min(coverage_to_confidence(coverage), LLM_COMPOSE_CAP)
        state["suggested_translation"] = translation
        state["confidence"] = conf
        state["retrieval_confidence"] = conf
        state["llm_detail"] = reasoning or f"LLM 受约束拼装 coverage={coverage:.0%}"
        state["trace"] = [{"stage": "compose_suggest", "ok": True}]
    except Exception as exc:
        return apply_fallback(f"LLM 调用失败: {exc}")

    return state
