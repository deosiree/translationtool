"""术语学习审核服务 — 待审核列表、详情与确认入库（MergeToStore）。"""

from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiError
from app.repository.term_repo import TermRepository


class TermAuditService:
    """审核列表/详情/确认 — 与 PreTranslateService 同构。"""

    def __init__(self, session: AsyncSession):
        """注入异步数据库会话，内部创建 TermRepository。"""
        self._repo = TermRepository(session)

    async def list_pending(self, *, page: int, page_size: int) -> tuple[list, int]:
        """分页查询 review_status=pending 的 audit 记录。

        Args:
            page: 页码，从 1 开始。
            page_size: 每页条数。

        Returns:
            (records, total) — ORM 列表与总数。
        """
        return await self._repo.list_pending_audits(page=page, page_size=page_size)

    async def get_audit_or_raise(self, audit_id: str):
        """按 id 查询 audit，不存在时抛 ApiError。

        Args:
            audit_id: term_agent_audit 主键。

        Returns:
            TermAgentAudit ORM 对象。

        Raises:
            ApiError: 记录不存在。
        """
        record = await self._repo.get_audit(audit_id)
        if record is None:
            raise ApiError(f"审核记录 {audit_id} 不存在")
        return record

    async def review(self, audit_id: str, *, action: str, comment: str | None):
        """人工确认或拒绝 audit 记录。

        approved 且存在 suggested_translation 时，调用 _merge_to_store 写入术语库。

        Args:
            audit_id: term_agent_audit 主键。
            action: ``approved`` 或 ``rejected``。
            comment: 可选审核备注。

        Returns:
            更新后的 TermAgentAudit ORM 对象。

        Raises:
            ApiError: 记录不存在或已是终态。
        """
        record = await self.get_audit_or_raise(audit_id)
        if record.review_status != "pending":
            raise ApiError(
                f"审核记录 {audit_id} 已是终态「{record.review_status}」，无法重复审核"
            )

        await self._repo.update_audit(
            audit_id,
            review_status=action,
            review_comment=comment,
        )

        if action == "approved" and record.suggested_translation:
            await self._merge_to_store(record)

        return await self._repo.get_audit(audit_id)

    async def _merge_to_store(self, record) -> None:
        """审核通过后写入 t_translate，术语库已有精确匹配时跳过。"""
        existing = await self._repo.find_exact(
            record.source_text,
            record.target_lang,
            record.department,
        )
        if not existing:
            await self._repo.insert_translate(
                entry=record.source_text,
                translate=record.suggested_translation,
                target_lang=record.target_lang,
                department=record.department,
            )
