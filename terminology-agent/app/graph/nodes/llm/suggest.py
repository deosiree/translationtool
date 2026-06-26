"""节点：LLM Suggest — 调用大模型为新词提出译文建议。"""

import json

from langchain_core.messages import HumanMessage, SystemMessage

from app.graph.prompts.suggest import SUGGEST_SYSTEM_PROMPT, build_suggest_user_message
from app.graph.state import TermState
from config.settings import settings


async def llm_suggest_node(state: TermState) -> TermState:
    """调用大模型为新词提出英文译文建议。

    若非新词则跳过 LLM 直接结束；否则读取 .env 中的 LLM 配置，
    结合 analyze_context 写入的上下文线索构造 prompt，异步调用模型，
    解析 JSON 结果写入 ``suggested_translation`` / ``llm_reasoning``，
    下一步固定指向 ``review`` 节点。

    Args:
        state: 当前工作流状态。

    Returns:
        更新后的 state（含译文建议、推理说明或 error）。
    """
    if not state.get("is_new_term"):
        state["next_node"] = "end"
        return state

    api_key = settings.llm_api_key
    base_url = settings.llm_base_url
    model = settings.llm_model
    temperature = settings.llm_temperature

    if not api_key:
        state["suggested_translation"] = "[LLM 未配置 — 请在 .env 中设置 LLM_API_KEY]"
        state["llm_reasoning"] = "LLM service not available (api_key not set)"
        state["next_node"] = "review"
        return state

    user_text = build_suggest_user_message(state)

    try:
        from langchain_openai import ChatOpenAI

        llm = ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            model=model,
            temperature=temperature,
        )

        response = await llm.ainvoke([
            SystemMessage(content=SUGGEST_SYSTEM_PROMPT),
            HumanMessage(content=user_text),
        ])

        content = response.content.strip()

        try:
            parsed = json.loads(content)
            state["suggested_translation"] = parsed.get("translation", content)
            state["llm_reasoning"] = parsed.get("reasoning", content)
        except (json.JSONDecodeError, TypeError):
            state["suggested_translation"] = content.split("\n")[0].strip()
            state["llm_reasoning"] = content
    except Exception as exc:
        state["error"] = f"LLM call failed: {exc}"
        state["suggested_translation"] = None

    state["next_node"] = "review"
    return state
