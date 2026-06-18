"""Node: LLM Suggest — call an LLM to propose a translation for a new term."""

import os

from dotenv import load_dotenv
from langchain_core.messages import HumanMessage, SystemMessage

from app.graph.state import TermState

# Load .env once at module level
load_dotenv()

_SYSTEM_PROMPT = """You are an expert i18n technical translator for industrial automation software (Chinese → English).

Your task: Given a new Chinese term (not yet in the terminology store), propose ONE best English translation.

Guidelines:
1. Be concise — output ONLY the translation and a brief one-line reason.
2. Prefer short, precise technical terms over long phrases.
3. Consider the context category (button, menu, tooltip, etc.) if provided.
4. Never invent terms — use standard software UI/UX vocabulary.
5. If the term is ambiguous, note alternatives in parentheses.

Output format (JSON):
{"translation": "...", "reasoning": "..."}
"""


async def llm_suggest_node(state: TermState) -> TermState:
    """Call the configured LLM (DeepSeek / OpenAI-compatible) to propose a translation."""
    if not state.get("is_new_term"):
        state["next_node"] = "end"
        return state

    api_key = os.environ.get("LLM_API_KEY")
    base_url = os.environ.get("LLM_BASE_URL", "https://api.deepseek.com")
    model = os.environ.get("LLM_MODEL", "deepseek-chat")

    if not api_key:
        state["suggested_translation"] = "[LLM 未配置 — 请在 .env 中设置 LLM_API_KEY]"
        state["llm_reasoning"] = "LLM service not available (api_key not set)"
        state["next_node"] = "review"
        return state

    context_hints = state.get("llm_reasoning") or "No additional context."
    user_text = (
        f"Chinese term: {state['source_text']}\n"
        f"Context analysis: {context_hints}\n"
        f"Propose the best English translation."
    )

    try:
        from langchain_openai import ChatOpenAI

        llm = ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            model=model,
            temperature=0.3,
        )
        response = await llm.ainvoke([
            SystemMessage(content=_SYSTEM_PROMPT),
            HumanMessage(content=user_text),
        ])

        content = response.content.strip()
        import json
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
