"""comment_rule 仓储 — CRUD / 按 key 查询 / 导入 upsert。"""

from __future__ import annotations

from datetime import datetime
from typing import Any, Sequence

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.comment_rule import CommentRule


class CommentRuleRepository:
    """comment_rule 表访问。"""

    def __init__(self, session: AsyncSession):
        self._session = session

    async def list_rules(
        self,
        *,
        page: int = 1,
        page_size: int = 20,
        comment_key: str | None = None,
        prefer_abbr: bool | None = None,
        scene: str | None = None,
        rule_text: str | None = None,
    ) -> tuple[list[CommentRule], int]:
        """分页列表。"""
        conditions = []
        if comment_key:
            conditions.append(CommentRule.comment_key.like(f"%{comment_key}%"))
        if prefer_abbr is not None:
            conditions.append(CommentRule.prefer_abbr.is_(prefer_abbr))
        if scene:
            conditions.append(CommentRule.scene.like(f"%{scene}%"))
        if rule_text:
            conditions.append(CommentRule.rule_text.like(f"%{rule_text}%"))

        count_stmt = select(func.count()).select_from(CommentRule)
        if conditions:
            count_stmt = count_stmt.where(*conditions)
        total = (await self._session.execute(count_stmt)).scalar_one()

        offset = max(page - 1, 0) * page_size
        stmt = select(CommentRule)
        if conditions:
            stmt = stmt.where(*conditions)
        stmt = (
            stmt.order_by(CommentRule.comment_key.asc(), CommentRule.updated_at.desc())
            .offset(offset)
            .limit(page_size)
        )
        result = await self._session.execute(stmt)
        return list(result.scalars().all()), int(total)

    async def get_by_id(self, rule_id: str) -> CommentRule | None:
        return await self._session.get(CommentRule, rule_id)

    async def find_by_comment_keys(self, keys: Sequence[str]) -> list[CommentRule]:
        """按 comment key 列表取全部匹配行（同 key 可多行）。"""
        cleaned = [str(k).strip() for k in keys if str(k).strip()]
        if not cleaned:
            return []
        stmt = (
            select(CommentRule)
            .where(CommentRule.comment_key.in_(cleaned))
            .order_by(CommentRule.comment_key.asc(), CommentRule.id.asc())
        )
        result = await self._session.execute(stmt)
        return list(result.scalars().all())

    async def find_by_key_and_scene(
        self, comment_key: str, scene: str | None
    ) -> CommentRule | None:
        """upsert 用：comment_key + scene（空 scene 视为 ''）。"""
        scene_norm = (scene or "").strip()
        stmt = select(CommentRule).where(CommentRule.comment_key == comment_key)
        result = await self._session.execute(stmt)
        rows = list(result.scalars().all())
        for row in rows:
            if (row.scene or "").strip() == scene_norm:
                return row
        return None

    async def create_rule(self, payload: dict[str, Any]) -> CommentRule:
        row = CommentRule(**payload)
        self._session.add(row)
        await self._session.flush()
        return row

    async def update_rule(self, row: CommentRule, payload: dict[str, Any]) -> CommentRule:
        for key, value in payload.items():
            if hasattr(row, key):
                setattr(row, key, value)
        row.updated_at = datetime.now()
        await self._session.flush()
        return row

    async def delete_by_id(self, rule_id: str) -> bool:
        row = await self.get_by_id(rule_id)
        if row is None:
            return False
        await self._session.delete(row)
        await self._session.flush()
        return True

    async def commit(self) -> None:
        await self._session.commit()
