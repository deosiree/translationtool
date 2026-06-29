"""Agent 自有表 — term_word 元词词典（Grep 线）与矛盾工单。"""

import uuid
from datetime import datetime

from sqlalchemy import String, DateTime, Text, JSON
from sqlalchemy.orm import Mapped, mapped_column

from app.models.database import Base


def _new_word_id() -> str:
    return uuid.uuid4().hex[:16]


class TermWord(Base):
    """term_word — Grep 线 live keyword lookup 语料。

    消歧键：(word, comment, target_lang)，不含 department。
    """

    __tablename__ = "term_word"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=_new_word_id)
    word: Mapped[str] = mapped_column(String(255), nullable=False, comment="单词文本")
    comment: Mapped[str] = mapped_column(String(512), nullable=False, default="", comment="消歧 comment")
    translate: Mapped[str] = mapped_column(String(1024), nullable=False, comment="译法")
    target_lang: Mapped[str] = mapped_column(String(64), nullable=False, comment="目标语种")
    department: Mapped[str | None] = mapped_column(String(128), nullable=True, comment="部门过滤")
    source_translate_id: Mapped[str] = mapped_column(String(64), nullable=False, comment="溯源 translate")
    source_entry_info_id: Mapped[str | None] = mapped_column(String(64), nullable=True, comment="溯源 entry_info")
    task_id: Mapped[str | None] = mapped_column(String(255), nullable=True, comment="溯源任务")
    product_id: Mapped[str | None] = mapped_column(String(255), nullable=True, comment="溯源产品")
    description: Mapped[str | None] = mapped_column(Text, nullable=True, comment="描述")
    remark1: Mapped[str | None] = mapped_column(String(512), nullable=True, comment="备注1")
    remark2: Mapped[str | None] = mapped_column(String(512), nullable=True, comment="备注2")
    remark3: Mapped[str | None] = mapped_column(String(512), nullable=True, comment="备注3")
    status: Mapped[str] = mapped_column(String(16), default="pending", comment="approved|pending|deprecated")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.now, onupdate=datetime.now
    )


class TermWordConflict(Base):
    """term_word_conflict — 同 (word, comment, target_lang) 下多译法矛盾。"""

    __tablename__ = "term_word_conflict"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=_new_word_id)
    word: Mapped[str] = mapped_column(String(255), nullable=False)
    comment: Mapped[str] = mapped_column(String(512), nullable=False, default="")
    target_lang: Mapped[str] = mapped_column(String(64), nullable=False)
    word_ids: Mapped[list] = mapped_column(JSON, nullable=False, comment="冲突 term_word.id")
    conflict_type: Mapped[str] = mapped_column(String(32), default="translate_mismatch")
    resolution: Mapped[str] = mapped_column(String(32), default="open")
    task_ids: Mapped[list | None] = mapped_column(JSON, nullable=True)
    product_ids: Mapped[list | None] = mapped_column(JSON, nullable=True)
    source_entry_info_ids: Mapped[list | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now)
