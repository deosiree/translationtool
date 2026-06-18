"""Node: LLM Suggest — call an LLM to propose a translation for a new term."""

import os

from dotenv import load_dotenv
from langchain_core.messages import HumanMessage, SystemMessage

from app.graph.state import TermState

# Load .env once at module level
load_dotenv()

_SYSTEM_PROMPT = """
您是工业自动化软件（中文→英文）的国际化技术翻译专家。

你的任务：给定一个新的中文术语（尚未在术语库中），提出一个最好的英文翻译。

Guidelines:
1.简洁-只输出翻译和简短的一行原因。
2.比起长短语，更喜欢简短、精确的技术术语。
3.如果提供，考虑上下文类别（按钮、菜单、工具提示等）。
4.永远不要发明术语——使用标准软件用户界面/用户体验词汇。
5.如果术语含糊不清，请在括号中注明替代方案。

Output format (JSON):
{"translation": "...", "reasoning": "..."}
"""


async def llm_suggest_node(state: TermState) -> TermState:
    """
    调用配置的大语言模型（DeepSeek 或兼容 OpenAI 接口的模型）为术语提出翻译建议。

    该函数检查状态以确定是否需要处理新术语，配置 LLM 客户端，
    构建包含源文本和上下文提示的 prompt，并异步调用模型获取翻译及推理过程。
    结果会被解析并更新到状态对象中，同时决定下一个工作流节点。

    Args:
        state (TermState): 当前工作流的状态字典，包含源文本、是否为 new term 标记、
                           以及用于存储翻译建议、推理过程和下一步节点指向的字段。

    Returns:
        TermState: 更新后的状态字典，包含 suggested_translation, llm_reasoning,
                   next_node，以及在发生错误时的 error 字段。
    """
    # 如果当前术语不是新术语，则跳过 LLM 调用，直接结束流程
    if not state.get("is_new_term"):
        state["next_node"] = "end"
        return state

    # 从环境变量中加载 LLM 的配置信息，包括 API Key、基础 URL 和模型名称
    api_key = os.environ.get("LLM_API_KEY")
    base_url = os.environ.get("LLM_BASE_URL", "https://api.deepseek.com")
    model = os.environ.get("LLM_MODEL", "deepseek-chat")

    # 若未配置 API Key，则记录错误信息并转向人工审核节点
    if not api_key:
        state["suggested_translation"] = "[LLM 未配置 — 请在 .env 中设置 LLM_API_KEY]"
        state["llm_reasoning"] = "LLM service not available (api_key not set)"
        state["next_node"] = "review"
        return state

    # 构建发送给 LLM 的用户提示文本，包含中文术语和之前的上下文分析
    context_hints = state.get("llm_reasoning") or "No additional context."
    user_text = (
        f"Chinese term: {state['source_text']}\n"
        f"Context analysis: {context_hints}\n"
        f"Propose the best English translation."
    )

    try:
        from langchain_openai import ChatOpenAI

        # 初始化 LangChain 的 ChatOpenAI 客户端，配置较低的 temperature 以获得更确定的输出
        llm = ChatOpenAI(
            api_key=api_key,
            base_url=base_url,
            model=model,
            temperature=0.3,
        )
        
        # 异步调用 LLM，传入系统提示词和用户构建的提示文本
        response = await llm.ainvoke([
            SystemMessage(content=_SYSTEM_PROMPT),
            HumanMessage(content=user_text),
        ])

        content = response.content.strip()
        import json
        
        # 尝试将 LLM 返回的内容解析为 JSON，提取翻译建议和推理过程
        try:
            parsed = json.loads(content)
            state["suggested_translation"] = parsed.get("translation", content)
            state["llm_reasoning"] = parsed.get("reasoning", content)
        except (json.JSONDecodeError, TypeError):
            # 如果 JSON 解析失败，则回退到纯文本处理模式
            state["suggested_translation"] = content.split("\n")[0].strip()
            state["llm_reasoning"] = content
    except Exception as exc:
        # 捕获 LLM 调用过程中的异常，记录错误信息并将翻译建议置空
        state["error"] = f"LLM call failed: {exc}"
        state["suggested_translation"] = None

    # 无论成功与否，下一步都转向人工审核节点
    state["next_node"] = "review"
    return state
