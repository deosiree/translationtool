"""工作台词条（t_entry_info）与任务译文（t_translate）写入。"""

from __future__ import annotations

from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.term import EntryInfo, TranslateEntry


class EntryRepository:
    """异步读写工作台 entry_info 及其挂载的 translate 行。"""

    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def get_entry_info(self, entry_info_id: str) -> EntryInfo | None:
        """按 id 查询未删除的 entry_info。"""
        stmt = select(EntryInfo).where(
            EntryInfo.id == entry_info_id,
            EntryInfo.is_delete == 0,
        )
        result = await self._session.execute(stmt)
        return result.scalars().first()

    async def get_translate(self, trans_id: str) -> TranslateEntry | None:
        """按 id 查询 translate 行。"""
        return await self._session.get(TranslateEntry, trans_id)

    async def insert_workbench_translate(
        self,
        *,
        entry: str,
        translate: str,
        target_lang: str,
        department: str | None,
        audit_suggest: str | None,
    ) -> TranslateEntry:
        """新建任务译文行，translate_state=1（待审核）。"""
        record = TranslateEntry(
            id=self._new_translate_id(),
            entry=entry,
            translate=translate,
            type=target_lang,
            visual_range=department,
            translate_state="1",
            audit_suggest=audit_suggest,
            delete_state=0,
            public_state=0,
            last_use_time=datetime.now(),
        )
        self._session.add(record)
        await self._session.flush()
        return record

    async def update_workbench_translate(
        self,
        record: TranslateEntry,
        *,
        translate: str,
        department: str | None,
        audit_suggest: str | None,
        translate_state: str,
    ) -> TranslateEntry:
        """更新已有 translate 行。"""
        record.translate = translate
        record.audit_suggest = audit_suggest
        record.translate_state = translate_state
        if department is not None:
            record.visual_range = department
        record.last_use_time = datetime.now()
        await self._session.flush()
        return record

    async def set_entry_trans_id(
        self,
        entry_info: EntryInfo,
        trans_id_attr: str,
        trans_id: str,
    ) -> None:
        """更新 entry_info 上对应语种的外键。"""
        setattr(entry_info, trans_id_attr, trans_id)
        await self._session.flush()

    async def commit(self) -> None:
        """提交当前事务。"""
        await self._session.commit()

    @staticmethod
    def _new_translate_id() -> str:
        import uuid

        return uuid.uuid4().hex[:32]
