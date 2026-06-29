"""term_word 数据访问 — Grep 线 live keyword lookup。"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Sequence

from sqlalchemy import delete, func, select, String
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.word import TermWord, TermWordConflict
from app.models.term import TaskInfo, Product
from app.word.constants import CONFLICT_RESOLUTION_OPEN, WORD_STATUS_APPROVED


@dataclass
class ConflictWithProvenance:
    """开放矛盾 + 首条 task/product 名称（治理页列表用）。"""

    conflict: TermWordConflict
    task_name: str | None
    product_name: str | None


class WordRepository:
    """term_word / term_word_conflict 异步仓储。"""

    def __init__(self, session: AsyncSession):
        self._session = session

    async def find_by_word(
        self,
        word: str,
        *,
        target_lang: str,
        comment: str | None = None,
        department: str | None = None,
        status: str = WORD_STATUS_APPROVED,
    ) -> list[TermWord]:
        """Grep lookup — department 为运行时过滤，非消歧键。"""
        conditions = [
            TermWord.word == word,
            TermWord.target_lang == target_lang,
            TermWord.status == status,
        ]
        if comment is not None:
            conditions.append(TermWord.comment == comment)
        if department:
            conditions.append(TermWord.department == department)

        stmt = select(TermWord).where(*conditions)
        result = await self._session.execute(stmt)
        return list(result.scalars().all())

    async def insert_words(self, payloads: list[dict[str, Any]]) -> list[TermWord]:
        """批量插入 term_word 行。"""
        rows = [TermWord(**payload) for payload in payloads]
        self._session.add_all(rows)
        await self._session.flush()
        return rows

    async def truncate_index(self) -> None:
        """清空 term_word 与 term_word_conflict（--rebuild 用）。"""
        await self._session.execute(delete(TermWordConflict))
        await self._session.execute(delete(TermWord))
        await self._session.flush()

    async def insert_conflicts(self, payloads: list[dict[str, Any]]) -> list[TermWordConflict]:
        """批量插入矛盾工单。"""
        rows = [TermWordConflict(**payload) for payload in payloads]
        self._session.add_all(rows)
        await self._session.flush()
        return rows

    async def list_open_conflicts(
        self,
        page: int = 1,
        page_size: int = 20,
        *,
        target_lang: str | None = None,
        task_id: str | None = None,
        product_id: str | None = None,
    ) -> tuple[Sequence[ConflictWithProvenance], int]:
        """分页列出 resolution=open 的矛盾，附带首条 task/product 名。"""
        base = TermWordConflict.resolution == CONFLICT_RESOLUTION_OPEN
        conditions = [base]
        if target_lang:
            conditions.append(TermWordConflict.target_lang == target_lang)
        if task_id:
            conditions.append(
                func.cast(TermWordConflict.task_ids, String).like(f'%"{task_id}"%')
            )
        if product_id:
            conditions.append(
                func.cast(TermWordConflict.product_ids, String).like(f'%"{product_id}"%')
            )

        count_stmt = (
            select(func.count())
            .select_from(TermWordConflict)
            .where(*conditions)
        )
        total = (await self._session.execute(count_stmt)).scalar_one()

        offset = (page - 1) * page_size
        stmt = (
            select(TermWordConflict)
            .where(*conditions)
            .order_by(TermWordConflict.created_at.desc())
            .offset(offset)
            .limit(page_size)
        )
        result = await self._session.execute(stmt)
        conflicts = list(result.scalars().all())

        enriched: list[ConflictWithProvenance] = []
        for conflict in conflicts:
            task_name = await self._first_task_name(conflict.task_ids)
            product_name = await self._first_product_name(conflict.product_ids)
            enriched.append(
                ConflictWithProvenance(
                    conflict=conflict,
                    task_name=task_name,
                    product_name=product_name,
                )
            )
        return enriched, total

    async def _first_task_name(self, task_ids: list | None) -> str | None:
        if not task_ids:
            return None
        task_id = str(task_ids[0])
        row = await self._session.get(TaskInfo, task_id)
        return row.name if row else None

    async def _first_product_name(self, product_ids: list | None) -> str | None:
        if not product_ids:
            return None
        product_id = str(product_ids[0])
        row = await self._session.get(Product, product_id)
        return row.name if row else None

    async def commit(self) -> None:
        await self._session.commit()
