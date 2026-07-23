"""Chat API 端点 — 智能助手对话接口。"""

from __future__ import annotations

import uuid

from fastapi import APIRouter

from app.core.response import fail, ok, ResponseCode
from app.schemas.chat import ChatData, ChatRequest
from config.settings import settings

router = APIRouter()

_SYSTEM_PROMPT = (
    "你是思源电气翻译工具（Translation Tool）的智能使用助手。"
    "你的职责是帮助用户了解和使用该翻译管理平台，包括：\n"
    "- 解释平台各模块功能（工作台、词条管理、术语库、文件管理、配置等）\n"
    "- 指导用户如何执行翻译、审核、导入导出等操作\n"
    "- 解答平台使用过程中的常见问题\n"
    "- 介绍术语学习 Agent 的工作原理（预翻译、分词、术语审核）\n\n"
    "回答要求：\n"
    "- 使用简体中文，语气专业友好\n"
    "- 如果用户问的问题超出翻译工具范围，礼貌引导回平台相关话题\n"
    "- 回答简洁实用，避免冗长\n"
    "- 不知道具体细节时，诚实说明并建议查看官方文档"
)


def _build_langchain_messages(messages: list) -> list:
    """将 ChatMessage 列表转为 LangChain 消息格式。"""
    from langchain_core.messages import HumanMessage, SystemMessage, AIMessage

    result = [SystemMessage(content=_SYSTEM_PROMPT)]
    for msg in messages:
        if msg.role == "user":
            result.append(HumanMessage(content=msg.content))
        elif msg.role == "assistant":
            result.append(AIMessage(content=msg.content))
        # system role from client is ignored (we use our own system prompt)
    return result


@router.post("/chat", summary="智能助手对话")
async def chat(body: ChatRequest):
    """接收对话历史，返回助手回复。

    首次调用不传 session_id，服务端生成并返回；
    后续调用传入同一 session_id 以维持会话连续性（服务端暂不持久化，由前端管理历史）。
    """
    api_key = settings.llm_api_key
    if not api_key:
        return fail("LLM 未配置，请在 .env 设置 LLM_API_KEY", code=ResponseCode.SERVICE_ERROR)

    session_id = body.session_id or uuid.uuid4().hex[:12]
    lc_messages = _build_langchain_messages(body.messages)

    try:
        from langchain_openai import ChatOpenAI

        llm = ChatOpenAI(
            api_key=api_key,
            base_url=settings.llm_base_url,
            model=settings.llm_model,
            temperature=settings.llm_temperature,
        )
        response = await llm.ainvoke(lc_messages)
        reply = response.content.strip()
    except Exception as exc:
        return fail(f"LLM 调用失败: {exc}", code=ResponseCode.SERVICE_ERROR)

    return ok(ChatData(reply=reply, session_id=session_id))
