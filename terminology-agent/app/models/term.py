"""SQLAlchemy ORM 模型 — 术语学习 Agent。

表分两类：
  1. 只读映射：对接 Java 现有表（t_translate、t_entry_info）
  2. Agent 自有表：term_agent_audit，存预翻译待审核与工作流状态
"""

import uuid
from datetime import datetime

from sqlalchemy import Integer, String, DateTime, Text, Float, JSON
from sqlalchemy.orm import Mapped, mapped_column

from app.models.database import Base


def _new_id() -> str:
    """生成 audit 主键（16 位 hex，与现有 UUID 风格一致）。"""
    return uuid.uuid4().hex[:16]


# ──────────────────────────────────────────────
# Agent 自有表：术语学习 / 预翻译待审核
# ──────────────────────────────────────────────


class TermAgentAudit(Base):
    """term_agent_audit — 每条待审核或已处理的 Agent 预翻译记录。

    工作台「Agent翻译」低于置信度阈值时写入此表；
    术语学习页读取 review_status='pending' 的记录供人工确认。
    """

    __tablename__ = "term_agent_audit"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, default=_new_id)
    source_text: Mapped[str] = mapped_column(String(1024), nullable=False, comment="词条原文")
    context: Mapped[str | None] = mapped_column(Text, nullable=True, comment="可选上下文")

    # ── LangGraph 术语发现字段（阶段二合并前保留列）──
    matched_term: Mapped[str | None] = mapped_column(String(1024), nullable=True, comment="术语库已有翻译")
    match_confidence: Mapped[float | None] = mapped_column(Float, nullable=True, comment="匹配置信度")
    is_new_term: Mapped[bool] = mapped_column(Integer, default=False, comment="是否为新术语（ORM 存 0/1）")

    # ── Agent 预翻译产出 ──
    suggested_translation: Mapped[str | None] = mapped_column(String(1024), nullable=True, comment="建议译文")
    llm_reasoning: Mapped[str | None] = mapped_column(Text, nullable=True, comment="Agent 推理说明")

    # ── 人工审核 ──
    review_status: Mapped[str] = mapped_column(String(16), default="pending", comment="pending | approved | rejected")
    review_comment: Mapped[str | None] = mapped_column(String(512), nullable=True, comment="审核人备注")

    # ── 工作台上下文（与 translateModal / PreTranslateModal 对齐）──
    entry_info_id: Mapped[str | None] = mapped_column(String(64), nullable=True, comment="工作台词条 id")
    task_id: Mapped[str | None] = mapped_column(String(64), nullable=True, comment="翻译任务 id")
    task_name: Mapped[str | None] = mapped_column(String(255), nullable=True, comment="任务名称")
    product_name: Mapped[str | None] = mapped_column(String(255), nullable=True, comment="产品名称")
    target_lang: Mapped[str | None] = mapped_column(String(64), nullable=True, comment="目标语种，如「俄文」")
    department: Mapped[str | None] = mapped_column(String(128), nullable=True, comment="部门所属，对应 t_translate.visual_range")
    confidence: Mapped[float | None] = mapped_column(Float, nullable=True, comment="预翻译置信度 0~1")
    similar_terms: Mapped[list | None] = mapped_column(JSON, nullable=True, comment="RAG 参考术语 [{entry, translate, score}]")
    retrieval_method: Mapped[str | None] = mapped_column(String(32), nullable=True, comment="检索策略 exact|fuzzy|hybrid")
    source_type: Mapped[str] = mapped_column(String(16), default="workbench_agent", comment="记录来源标识")

    error: Mapped[str | None] = mapped_column(Text, nullable=True, comment="节点异常信息")

    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, comment="创建时间")
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.now, onupdate=datetime.now, comment="更新时间")


# ──────────────────────────────────────────────
# 只读映射：Java 生产库现有表
# ──────────────────────────────────────────────


class TranslateEntry(Base):
    """t_translate — 术语库词条对（中文 entry ↔ 目标语 translate）。

    Agent 预翻译的 RetrieveSimilar 节点读此表；
    审核通过后 MergeToStore 也写入此表（translate_state='3'）。
    """

    __tablename__ = "t_translate"
    __table_args__ = {"extend_existing": True}

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    entry: Mapped[str | None] = mapped_column(String(1024), comment="中文词条")
    translate: Mapped[str | None] = mapped_column(String(1024), comment="译文")
    type: Mapped[str | None] = mapped_column(String(255), comment="语种，如「俄文」")
    translate_state: Mapped[str | None] = mapped_column(String(64), comment="3=已审核通过")
    remark: Mapped[str | None] = mapped_column(String(255), comment="备注")
    delete_state: Mapped[int | None] = mapped_column(Integer, default=0, comment="0=有效")
    visual_range: Mapped[str | None] = mapped_column(String(255), comment="部门可见范围")
    public_state: Mapped[int | None] = mapped_column(Integer, default=0, comment="0=未公开，1=公开")
    last_use_time: Mapped[datetime | None] = mapped_column(DateTime, nullable=True, comment="最近使用时间，精确匹配时取最新")


class EntryInfo(Base):
    """t_entry_info — 工作台词条主表（只读，后续可扩展关联查询）。"""

    __tablename__ = "t_entry_info"
    __table_args__ = {"extend_existing": True}

    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    entry: Mapped[str | None] = mapped_column(String(1024), comment="词条")
    entry_state: Mapped[int | None] = mapped_column(Integer, comment="词条状态")
    en_trans_id: Mapped[str | None] = mapped_column(String(64), comment="英文翻译 id")
    product_id: Mapped[str | None] = mapped_column(String(255), comment="产品 id")
    remark: Mapped[str | None] = mapped_column(Text, comment="备注")
    is_delete: Mapped[int] = mapped_column(Integer, default=0, comment="0=有效")
