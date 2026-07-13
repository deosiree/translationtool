"""术语库与 Agent 审核表的数据访问层。

职责划分：
  - 读 t_translate：预翻译 RAG 检索（精确 / 模糊）
  - 写 term_agent_audit：低于阈值的 needs_human 记录
  - 写 t_translate：人工审核通过后 MergeToStore
"""

from typing import Sequence

from sqlalchemy import func, select, or_, desc
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.term import TermAgentAudit, TranslateEntry
from app.schemas.agent import TermAuditListFilters
from app.shared.audit_fingerprint import audit_write_fingerprint, fingerprint_from_audit_record


class TermRepository:
    """异步 SQLAlchemy 仓储，供 PreTranslateService 与 router 使用。"""

    def __init__(self, session: AsyncSession):
        """注入异步数据库会话。"""
        self._session = session

    # ── 术语库检索（只读 t_translate）──

    async def find_exact(
        self,
        entry: str,
        target_lang: str | None,
        department: str | None,
    ) -> TranslateEntry | None:
        """精确匹配 — 对齐 Java TranslateMapper.getVersionSuggestTrans。

        条件：entry 完全一致 + translate_state=3 + 可选 type/visual_range。
        排序：last_use_time DESC，取最新一条。
        """
        conditions = [
            TranslateEntry.entry == entry,
            TranslateEntry.delete_state == 0,
            TranslateEntry.translate_state == "3",
        ]
        if target_lang:
            conditions.append(TranslateEntry.type == target_lang)
        if department:
            conditions.append(TranslateEntry.visual_range == department)

        stmt = (
            select(TranslateEntry)
            .where(*conditions)
            .order_by(desc(TranslateEntry.last_use_time))
            .limit(1)
        )
        result = await self._session.execute(stmt)
        return result.scalars().first()

    async def find_fuzzy(
        self,
        entry: str,
        target_lang: str | None,
        department: str | None,
        limit: int = 10,
    ) -> list[TranslateEntry]:
        """模糊匹配 — 去掉 % 占位符后取前 20 字做 LIKE 检索。"""
        core = entry.replace("%", "").strip()
        keyword = core[: min(len(core), 20)] if core else entry[:20]
        conditions = [
            TranslateEntry.entry.like(f"%{keyword}%"),
            TranslateEntry.delete_state == 0,
            TranslateEntry.translate_state == "3",
        ]
        if target_lang:
            conditions.append(TranslateEntry.type == target_lang)
        if department:
            conditions.append(TranslateEntry.visual_range == department)

        stmt = select(TranslateEntry).where(*conditions).limit(limit)
        result = await self._session.execute(stmt)
        return list(result.scalars().all())

    async def search_by_keyword(self, keyword: str, limit: int = 20) -> list[TranslateEntry]:
        """通用关键词搜索 — 预留扩展。"""
        stmt = (
            select(TranslateEntry)
            .where(
                TranslateEntry.entry.like(f"%{keyword}%"),
                TranslateEntry.delete_state == 0,
            )
            .limit(limit)
        )
        result = await self._session.execute(stmt)
        return list(result.scalars().all())

    # ── term_agent_audit 审核记录 ──

    async def find_pending_by_fingerprint(
        self,
        *,
        source_text: str,
        entry_comment: str | None,
        suggested_translation: str | None,
        target_lang: str | None,
        department: str | None,
        retrieval_method: str | None,
        confidence: float | None,
    ) -> TermAgentAudit | None:
        """pending 队列中是否已有相同写入指纹的记录。"""
        fp = audit_write_fingerprint(
            source_text=source_text,
            entry_comment=entry_comment,
            suggested_translation=suggested_translation,
            target_lang=target_lang,
            department=department,
            retrieval_method=retrieval_method,
            confidence=confidence,
        )
        stmt = select(TermAgentAudit).where(
            TermAgentAudit.review_status == "pending",
            TermAgentAudit.source_text == source_text,
        )
        result = await self._session.execute(stmt)
        for record in result.scalars().all():
            if fingerprint_from_audit_record(record) == fp:
                return record
        return None

    async def create_pretranslate_audit(
        self,
        *,
        entry_info_id: str | None,
        task_id: str | None,
        task_name: str | None,
        product_name: str | None,
        target_lang: str | None,
        department: str | None,
        source_text: str,
        entry_comment: str | None = None,
        suggested_translation: str | None,
        confidence: float | None,
        similar_terms: list | None,
        retrieval_method: str | None,
        llm_reasoning: str | None,
    ) -> TermAgentAudit:
        """工作台 Agent 预翻译 needs_human 时写入待审核队列。

        全字段指纹与已有 pending 完全一致时跳过 INSERT，返回已有记录。
        """
        existing = await self.find_pending_by_fingerprint(
            source_text=source_text,
            entry_comment=entry_comment,
            suggested_translation=suggested_translation,
            target_lang=target_lang,
            department=department,
            retrieval_method=retrieval_method,
            confidence=confidence,
        )
        if existing is not None:
            return existing

        record = TermAgentAudit(
            source_text=source_text,
            entry_comment=entry_comment,
            suggested_translation=suggested_translation,
            llm_reasoning=llm_reasoning,
            review_status="pending",
            is_new_term=True,
            entry_info_id=entry_info_id,
            task_id=task_id,
            task_name=task_name,
            product_name=product_name,
            target_lang=target_lang,
            department=department,
            confidence=confidence,
            similar_terms=similar_terms or [],
            retrieval_method=retrieval_method,
            source_type="workbench_agent",
        )
        self._session.add(record)
        await self._session.commit()
        await self._session.refresh(record)
        return record

    async def update_audit(self, audit_id: str, **fields) -> TermAgentAudit | None:
        """按 id 更新 audit 字段（审核状态、备注等）。"""
        record = await self._session.get(TermAgentAudit, audit_id)
        if record is None:
            return None
        for key, value in fields.items():
            if hasattr(record, key):
                setattr(record, key, value)
        await self._session.commit()
        await self._session.refresh(record)
        return record

    async def get_audit(self, audit_id: str) -> TermAgentAudit | None:
        """按 id 查询单条 audit 记录。"""
        return await self._session.get(TermAgentAudit, audit_id)

    async def list_pending_audits(
        self,
        *,
        page: int = 1,
        page_size: int = 20,
        filters: TermAuditListFilters | None = None,
    ) -> tuple[Sequence[TermAgentAudit], int]:
        """术语学习页数据源 — 分页返回 review_status=pending 记录及总数。"""
        conditions = self._build_pending_filter_conditions(filters)

        count_stmt = (
            select(func.count())
            .select_from(TermAgentAudit)
            .where(*conditions)
        )
        total = (await self._session.execute(count_stmt)).scalar_one()

        offset = (page - 1) * page_size
        stmt = (
            select(TermAgentAudit)
            .where(*conditions)
            .order_by(TermAgentAudit.created_at.desc())
            .offset(offset)
            .limit(page_size)
        )
        result = await self._session.execute(stmt)
        return result.scalars().all(), total

    @staticmethod
    def _build_pending_filter_conditions(
        filters: TermAuditListFilters | None,
    ) -> list:
        """pending 列表基础条件 + 可选筛选。"""
        conditions = [TermAgentAudit.review_status == "pending"]
        if filters is None:
            return conditions

        if filters.source_text:
            conditions.append(TermAgentAudit.source_text.like(f"%{filters.source_text}%"))
        if filters.target_lang:
            conditions.append(TermAgentAudit.target_lang == filters.target_lang)
        if filters.task_name:
            conditions.append(TermAgentAudit.task_name.like(f"%{filters.task_name}%"))
        if filters.product_name:
            conditions.append(TermAgentAudit.product_name.like(f"%{filters.product_name}%"))
        if filters.department:
            conditions.append(TermAgentAudit.department == filters.department)
        if filters.confidence_min is not None:
            conditions.append(TermAgentAudit.confidence >= filters.confidence_min)
        if filters.confidence_max is not None:
            conditions.append(TermAgentAudit.confidence <= filters.confidence_max)
        if filters.retrieval_method:
            conditions.append(TermAgentAudit.retrieval_method == filters.retrieval_method)
        if filters.entry_comment:
            conditions.append(TermAgentAudit.entry_comment.like(f"%{filters.entry_comment}%"))
        return conditions

    # ── 术语库写入（MergeToStore）──

    async def insert_translate(
        self,
        *,
        entry: str,
        translate: str,
        target_lang: str | None,
        department: str | None,
    ) -> TranslateEntry:
        """审核通过后写入 t_translate。

        translate_state='3' 表示已审核；与 Java 术语库入库语义一致。
        若 find_exact 已存在则跳过（由 router.review_term 调用方判断）。
        """
        from datetime import datetime

        record = TranslateEntry(
            id=self._new_translate_id(),
            entry=entry,
            translate=translate,
            type=target_lang,
            visual_range=department,
            translate_state="3",
            delete_state=0,
            public_state=0,
            last_use_time=datetime.now(),
        )
        self._session.add(record)
        await self._session.commit()
        await self._session.refresh(record)
        return record

    @staticmethod
    def _new_translate_id() -> str:
        """生成 t_translate 主键（32 位 hex）。"""
        import uuid
        return uuid.uuid4().hex[:32]
