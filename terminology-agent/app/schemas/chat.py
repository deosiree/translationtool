"""Chat API 请求/响应模型。"""

from __future__ import annotations

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    """单条对话消息。"""
    role: str = Field(..., pattern="^(user|assistant|system)$", description="角色: user / assistant / system")
    content: str = Field(..., min_length=1, max_length=4096, description="消息内容")


class ChatRequest(BaseModel):
    """POST /agent/chat — 对话请求。"""
    messages: list[ChatMessage] = Field(..., min_length=1, max_length=50, description="对话历史（含当前用户消息）")
    session_id: str | None = Field(None, max_length=64, description="会话 ID（无则新开会话）")


class ChatData(BaseModel):
    """对话响应 data 字段。"""
    reply: str = Field(..., description="助手回复")
    session_id: str = Field(..., description="会话 ID")
