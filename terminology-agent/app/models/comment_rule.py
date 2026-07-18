"""Agent 自有表 — comment_rule（Comment 场景规则）。"""

from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.database import Base


def _new_id() -> str:
    return uuid.uuid4().hex[:16]


class CommentRule(Base):
    """comment_rule — 词条 comment 键对应的场景与翻译规则。"""

    __tablename__ = "comment_rule"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=_new_id)
    comment_key: Mapped[str] = mapped_column(
        String(128), nullable=False, comment="comment 键，如 tabBarTitle"
    )
    entry_source: Mapped[str | None] = mapped_column(
        String(255), nullable=True, comment="词条来源"
    )
    scene: Mapped[str | None] = mapped_column(Text, nullable=True, comment="场景")
    rule_text: Mapped[str | None] = mapped_column(Text, nullable=True, comment="规则")
    prefer_abbr: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False, comment="优先缩写"
    )
    case_type: Mapped[str | None] = mapped_column(
        String(32), nullable=True, comment="SentenceCase|TitleCase"
    )
    related_id: Mapped[str | None] = mapped_column(
        String(64), nullable=True, comment="对应 comment_rule.id"
    )
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.now, onupdate=datetime.now
    )
