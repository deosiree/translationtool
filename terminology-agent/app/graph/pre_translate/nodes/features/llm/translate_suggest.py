"""功能节点：translate_suggest — LLM 整句机翻（仅 llm 路径）。"""

from __future__ import annotations

import json

from langchain_core.messages import HumanMessage, SystemMessage

from app.graph.pre_translate.constants import LLM_DEFAULT_CONFIDENCE
from app.graph.pre_translate.prompts.pre_translate_suggest import (
    build_pre_translate_system_prompt,
    build_pre_translate_user_message,
)
from app.graph.pre_translate.state import PreTranslateState
from config.settings import settings


async def translate_suggest_node(state: PreTranslateState) -> PreTranslateState:
    """调用 LLM 生成整句机翻；无 Key 时降级 needs_human、无假译文。"""
    api_key = settings.llm_api_key

    if not api_key:
        state["suggested_translation"] = None
        state["confidence"] = 0.0
        state["llm_detail"] = "LLM 未配置，请在 .env 设置 LLM_API_KEY"
        state["error"] = "LLM not configured"
        state["trace"] = [{"stage": "translate_suggest", "skipped": "no_api_key"}]
        return state

    system_prompt = build_pre_translate_system_prompt(state.get("target_lang"))
    user_text = build_pre_translate_user_message(state)

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
        content = response.content.strip()
        try:
            parsed = json.loads(content)
            translation = parsed.get("translation", content)
            reasoning = parsed.get("reasoning", "")
        except (json.JSONDecodeError, TypeError):
            translation = content.split("\n")[0].strip()
            reasoning = content

        state["suggested_translation"] = translation
        if reasoning:
            state["llm_detail"] = reasoning
        state["confidence"] = LLM_DEFAULT_CONFIDENCE
        state["trace"] = [{"stage": "translate_suggest", "ok": True}]
    except Exception as exc:
        state["error"] = f"LLM call failed: {exc}"
        state["suggested_translation"] = None
        state["confidence"] = 0.0
        state["llm_detail"] = f"LLM 调用失败: {exc}"
        state["trace"] = [{"stage": "translate_suggest", "error": str(exc)}]

    return state
