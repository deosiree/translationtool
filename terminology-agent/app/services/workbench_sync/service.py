"""工作台翻译同步 — 将 Agent 建议译文写入任务 translate 并推进到待审核。"""

from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiError
from app.repository.entry_repo import EntryRepository
from app.shared.lang_mapping import resolve_trans_id_attr


class WorkbenchEntrySyncService:
    """对齐 Java EntryTempServiceImpl.updateTrans：state=1 表示待翻译审核。"""

    _FINALIZED_STATE = "3"

    def __init__(self, session: AsyncSession) -> None:
        self._repo = EntryRepository(session)

    async def sync_translation_to_pending_audit(
        self,
        *,
        entry_info_id: str | None,
        target_lang: str | None,
        translate: str | None,
        audit_suggest: str | None = None,
        department: str | None = None,
        entry_text: str | None = None,
    ) -> None:
        """将译文回写工作台 entry_info 挂载的 translate，并置 translate_state=1。

        Args:
            entry_info_id: 工作台词条 id。
            target_lang: 目标语种。
            translate: 建议译文。
            audit_suggest: 审核意见（通常来自 llm_reasoning）。
            department: 部门/可视范围。
            entry_text: entry 原文回退（entry_info.entry 为空时使用）。

        Raises:
            ApiError: 参数缺失、entry 不存在或 translate 为空。
        """
        if not entry_info_id:
            raise ApiError("缺少 entry_info_id，无法回写工作台翻译")
        if not translate or not str(translate).strip():
            raise ApiError("译文为空，无法回写工作台翻译")

        trans_id_attr = resolve_trans_id_attr(target_lang)
        entry_info = await self._repo.get_entry_info(entry_info_id)
        if entry_info is None:
            raise ApiError(f"工作台词条 {entry_info_id} 不存在或已删除")

        entry_value = (entry_info.entry or entry_text or "").strip()
        if not entry_value:
            raise ApiError(f"工作台词条 {entry_info_id} 缺少 entry 原文")

        trans_id = getattr(entry_info, trans_id_attr, None)
        normalized_translate = str(translate).strip()

        if trans_id:
            existing = await self._repo.get_translate(trans_id)
            if existing is None:
                raise ApiError(f"翻译记录 {trans_id} 不存在")
            if str(existing.translate_state or "") == self._FINALIZED_STATE:
                await self._repo.update_workbench_translate(
                    existing,
                    translate=normalized_translate,
                    department=department,
                    audit_suggest=audit_suggest,
                    translate_state=self._FINALIZED_STATE,
                )
            else:
                await self._repo.update_workbench_translate(
                    existing,
                    translate=normalized_translate,
                    department=department,
                    audit_suggest=audit_suggest,
                    translate_state="1",
                )
        else:
            created = await self._repo.insert_workbench_translate(
                entry=entry_value,
                translate=normalized_translate,
                target_lang=str(target_lang).strip(),
                department=department,
                audit_suggest=audit_suggest,
            )
            await self._repo.set_entry_trans_id(
                entry_info, trans_id_attr, created.id
            )

        await self._repo.commit()
