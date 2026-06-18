"""SQLAlchemy ORM models for terminology learning agent.

Two categories of tables:
  1. Read-only models mapping to existing production tables (t_translate, t_entry_info)
  2. Agent-owned tables for the learning workflow (term_agent_audit)
"""

import uuid
from datetime import datetime

from sqlalchemy import Column, String, Integer, DateTime, Text, Float
from sqlalchemy.orm import Mapped, mapped_column

from app.models.database import Base


def _new_id() -> str:
    return uuid.uuid4().hex[:16]


# ──────────────────────────────────────────────
# Agent-owned table: terminology learning audit
# ──────────────────────────────────────────────


class TermAgentAudit(Base):
    """Persists each terminology-learning workflow invocation for human review."""

    __tablename__ = "term_agent_audit"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=_new_id)
    source_text: Mapped[str] = mapped_column(String(1024), nullable=False, comment="The source Chinese term")
    context: Mapped[str | None] = mapped_column(Text, nullable=True, comment="Optional context")

    # Discovery result
    matched_term: Mapped[str | None] = mapped_column(String(1024), nullable=True, comment="Existing translation if found")
    match_confidence: Mapped[float | None] = mapped_column(Float, nullable=True, comment="Confidence of the match")
    is_new_term: Mapped[bool] = mapped_column(Integer, default=False, comment="Whether it's a genuinely new term")

    # LLM suggestion
    suggested_translation: Mapped[str | None] = mapped_column(String(1024), nullable=True, comment="LLM-proposed translation")
    llm_reasoning: Mapped[str | None] = mapped_column(Text, nullable=True, comment="LLM explanation")

    # Review
    review_status: Mapped[str] = mapped_column(String(16), default="pending", comment="pending | approved | rejected")
    review_comment: Mapped[str | None] = mapped_column(String(512), nullable=True, comment="Reviewer comment")

    # Error
    error: Mapped[str | None] = mapped_column(Text, nullable=True, comment="Error message if any")

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, comment="Creation time")
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, onupdate=datetime.now, comment="Last update time")


# ──────────────────────────────────────────────
# Read-only models: existing production tables
# ──────────────────────────────────────────────


class TranslateEntry(Base):
    """Maps to `t_translate` — existing term translation pairs."""

    __tablename__ = "t_translate"
    __table_args__ = {"extend_existing": True}

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    entry: Mapped[str | None] = mapped_column(String(1024), comment="Chinese term")
    translate: Mapped[str | None] = mapped_column(String(1024), comment="Translation")
    type: Mapped[str | None] = mapped_column(String(255), comment="Translation type / language code")
    translate_state: Mapped[str | None] = mapped_column(String(64), comment="0=untranslated, 1=translated, 2=rejected, 3=approved")
    remark: Mapped[str | None] = mapped_column(String(255), comment="Notes / forbidden terms")
    delete_state: Mapped[int | None] = mapped_column(Integer, default=0, comment="0=active, 1=deleted")


class EntryInfo(Base):
    """Maps to `t_entry_info` — main entry table with product/task context."""

    __tablename__ = "t_entry_info"
    __table_args__ = {"extend_existing": True}

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    entry: Mapped[str | None] = mapped_column(String(1024), comment="Chinese term")
    entry_state: Mapped[int | None] = mapped_column(Integer, comment="0=new, 1=audit-pending, 2=rejected, 3=approved, 4=archived")
    en_trans_id: Mapped[str | None] = mapped_column(String(64), comment="English translation ID → t_translate.id")
    product_id: Mapped[str | None] = mapped_column(String(255), comment="Product ID")
    remark: Mapped[str | None] = mapped_column(Text, comment="Notes")
    is_delete: Mapped[int] = mapped_column(Integer, default=0, comment="0=active, 1=deleted")
