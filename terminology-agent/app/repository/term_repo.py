"""Data access layer for terminology store and agent audit records."""

from typing import Sequence

from sqlalchemy import select, or_
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.term import TermAgentAudit, TranslateEntry


class TermRepository:
    """Read-write access to term store.

    - Reads from the existing `t_translate` table for term discovery.
    - Writes agent workflow state to the agent-owned `term_agent_audit` table.
    """

    def __init__(self, session: AsyncSession):
        self._session = session

    # ── Discovery (read from existing term store) ──

    async def find_by_chinese(self, chinese: str) -> list[TranslateEntry]:
        """Search `t_translate` by Chinese term (exact and fuzzy)."""
        stmt = (
            select(TranslateEntry)
            .where(
                TranslateEntry.entry == chinese,
                TranslateEntry.delete_state == 0,
                or_(
                    TranslateEntry.translate_state == "3",  # approved
                    TranslateEntry.translate_state.is_(None),
                ),
            )
            .limit(20)
        )
        result = await self._session.execute(stmt)
        return list(result.scalars().all())

    async def search_by_keyword(self, keyword: str, limit: int = 20) -> list[TranslateEntry]:
        """Fuzzy search by Chinese term containing keyword."""
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

    # ── Audit records (agent-owned table) ──

    async def create_audit(self, *, source_text: str, context: str | None = None) -> TermAgentAudit:
        """Create a new audit record for the learning workflow."""
        record = TermAgentAudit(
            source_text=source_text,
            context=context,
        )
        self._session.add(record)
        await self._session.commit()
        await self._session.refresh(record)
        return record

    async def update_audit(self, audit_id: str, **fields) -> TermAgentAudit | None:
        """Update fields on an audit record."""
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
        """Fetch a single audit record by ID."""
        return await self._session.get(TermAgentAudit, audit_id)

    async def list_pending_audits(self, limit: int = 50) -> Sequence[TermAgentAudit]:
        """List audits awaiting human review."""
        stmt = (
            select(TermAgentAudit)
            .where(TermAgentAudit.review_status == "pending")
            .order_by(TermAgentAudit.created_at.desc())
            .limit(limit)
        )
        result = await self._session.execute(stmt)
        return result.scalars().all()
