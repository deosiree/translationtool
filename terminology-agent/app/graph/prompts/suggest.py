"""术语译文建议 — LLM prompt 模板。"""

from app.graph.state import TermState

SUGGEST_SYSTEM_PROMPT = """
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


def build_suggest_user_message(state: TermState) -> str:
    """根据 state 构造发给 LLM 的用户消息。"""
    context_hints = state.get("llm_reasoning") or "No additional context."
    return (
        f"Chinese term: {state['source_text']}\n"
        f"Context analysis: {context_hints}\n"
        f"Propose the best English translation."
    )
