"""term_word 数据访问 — Grep 线 live keyword lookup。"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Any, Sequence

from sqlalchemy import delete, func, select, String
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.word import TermWord, TermWordConflict
from app.models.term import TaskInfo, Product
from app.models.word_constants import (
    CONFLICT_RESOLUTION_OPEN,
    WORD_STATUS_APPROVED,
    WORD_STATUS_PENDING,
)


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
        """Grep lookup — ``comment`` 为消歧键，``department`` 为运行时过滤。

        Args:
            word: 关键字（整句或 Trie 词级片段）。
            target_lang: 目标语种。
            comment: 消歧 comment；``None`` 时不按 comment 过滤。
            department: 部门可见范围；空则不过滤。
            status: 行状态，默认 ``"3"``（已审核）。

        Returns:
            匹配的 ``TermWord`` 行列表；多行表示 ambiguous。
        """
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

    async def list_distinct_words(
        self,
        target_lang: str,
        *,
        status: str = WORD_STATUS_APPROVED,
    ) -> list[str]:
        """按语种列出 DISTINCT ``word``（已审核），供 Grep Trie 构建。

        Args:
            target_lang: 目标语种。
            status: 行状态，Agent Grep 默认 ``"3"``。

        Returns:
            去重后的 word 文本列表。
        """
        stmt = (
            select(TermWord.word)
            .where(
                TermWord.target_lang == target_lang,
                TermWord.status == status,
            )
            .distinct()
        )
        result = await self._session.execute(stmt)
        return list(result.scalars().all())

    async def list_words(
        self,
        *,
        page: int = 1,
        page_size: int = 20,
        word: str | None = None,
        translate: str | None = None,
        target_lang: str | None = None,
        department: str | None = None,
        status: str | None = None,
    ) -> tuple[list[TermWord], int]:
        """分页列出 term_word（术语库「术语字典」Tab）。"""
        conditions = []
        if word:
            conditions.append(TermWord.word.like(f"%{word}%"))
        if translate:
            conditions.append(TermWord.translate.like(f"%{translate}%"))
        if target_lang:
            conditions.append(TermWord.target_lang == target_lang)
        if department:
            conditions.append(TermWord.department == department)
        if status:
            conditions.append(TermWord.status == status)

        count_stmt = select(func.count()).select_from(TermWord)
        if conditions:
            count_stmt = count_stmt.where(*conditions)
        total = (await self._session.execute(count_stmt)).scalar_one()

        offset = max(page - 1, 0) * page_size
        stmt = select(TermWord)
        if conditions:
            stmt = stmt.where(*conditions)
        stmt = (
            stmt.order_by(TermWord.updated_at.desc(), TermWord.word.asc())
            .offset(offset)
            .limit(page_size)
        )
        result = await self._session.execute(stmt)
        return list(result.scalars().all()), int(total)

    async def get_by_id(self, word_id: str) -> TermWord | None:
        return await self._session.get(TermWord, word_id)

    async def list_by_ids(self, ids: Sequence[str]) -> list[TermWord]:
        """按 id 列表取行（导出已选）。"""
        if not ids:
            return []
        stmt = select(TermWord).where(TermWord.id.in_(list(ids)))
        result = await self._session.execute(stmt)
        return list(result.scalars().all())

    async def find_by_key(
        self,
        word: str,
        *,
        comment: str,
        target_lang: str,
    ) -> TermWord | None:
        """按消歧键 (word, comment, target_lang) 查唯一行。"""
        stmt = select(TermWord).where(
            TermWord.word == word,
            TermWord.comment == (comment or ""),
            TermWord.target_lang == target_lang,
        ).limit(1)
        result = await self._session.execute(stmt)
        return result.scalars().first()

    async def build_lexicon(
        self,
        *,
        target_lang: str,
        words: Sequence[str],
    ) -> dict[str, str]:
        """为拆分对齐构建 word→translate 词典（任意状态，优先已审核）。"""
        uniq = [w for w in dict.fromkeys(words) if w]
        if not uniq:
            return {}
        stmt = select(TermWord).where(
            TermWord.target_lang == target_lang,
            TermWord.word.in_(uniq),
        )
        result = await self._session.execute(stmt)
        rows = list(result.scalars().all())
        # 优先 status=3
        rows.sort(key=lambda r: 0 if r.status == WORD_STATUS_APPROVED else 1)
        out: dict[str, str] = {}
        for r in rows:
            tr = (r.translate or "").strip()
            if tr and r.word not in out:
                out[r.word] = tr
        return out

    async def create_word(self, payload: dict[str, Any]) -> TermWord:
        """新建 term_word 行（术语字典 CRUD）。"""
        data = {
            "comment": "",
            "source_translate_id": "",
            "status": WORD_STATUS_PENDING,
            **payload,
        }
        row = TermWord(**data)
        self._session.add(row)
        await self._session.flush()
        return row

    async def update_word(self, row: TermWord, payload: dict[str, Any]) -> TermWord:
        """更新已有 term_word 行。"""
        for key, value in payload.items():
            if value is not None and hasattr(row, key):
                setattr(row, key, value)
        row.updated_at = datetime.now()
        await self._session.flush()
        return row

    async def delete_by_ids(self, ids: Sequence[str]) -> int:
        """按 id 物理删除；返回删除行数。"""
        if not ids:
            return 0
        stmt = delete(TermWord).where(TermWord.id.in_(list(ids)))
        result = await self._session.execute(stmt)
        await self._session.flush()
        return int(result.rowcount or 0)

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
