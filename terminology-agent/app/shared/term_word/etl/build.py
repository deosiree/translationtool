"""term_word 全量建库编排 — 从 t_translate + t_entry_info 导入。"""

from __future__ import annotations

from sqlalchemy import select

from app.models.database import AsyncSessionLocal
from app.models.term import EntryInfo, TranslateEntry
from app.repository.word_repo import WordRepository
from app.shared.term_word.etl.conflict import detect_translate_mismatches
from app.shared.term_word.etl.join_entry_info import (
    build_term_word_payload,
    index_entry_infos_by_trans_id,
)


async def build_word_index(
    *,
    dry_run: bool = False,
    rebuild: bool = False,
    target_lang: str | None = None,
) -> dict:
    """扫描 t_translate，join entry_info，写入 term_word 并检测矛盾。"""
    async with AsyncSessionLocal() as session:
        repo = WordRepository(session)

        translate_conditions = [TranslateEntry.delete_state == 0]
        if target_lang:
            translate_conditions.append(TranslateEntry.type == target_lang)

        trans_result = await session.execute(
            select(TranslateEntry).where(*translate_conditions)
        )
        translates = list(trans_result.scalars().all())

        ei_result = await session.execute(
            select(EntryInfo).where(EntryInfo.is_delete == 0)
        )
        entry_infos = list(ei_result.scalars().all())
        ei_index = index_entry_infos_by_trans_id(entry_infos)

        payloads: list[dict] = []
        skipped_no_link = 0

        for tr in translates:
            lang = (tr.type or "").strip()
            if not lang or not tr.id:
                continue
            linked = ei_index.get((str(tr.id), lang), [])
            if not linked:
                skipped_no_link += 1
                continue
            for ei in linked:
                payload = build_term_word_payload(translate=tr, entry_info=ei)
                if payload:
                    payloads.append(payload)

        stats = {
            "translates_scanned": len(translates),
            "entry_infos_loaded": len(entry_infos),
            "words_to_write": len(payloads),
            "skipped_no_entry_info": skipped_no_link,
            "conflicts": 0,
            "dry_run": dry_run,
        }

        if dry_run:
            conflicts = detect_translate_mismatches(payloads)
            stats["conflicts"] = len(conflicts)
            return stats

        if rebuild:
            await repo.truncate_index()

        rows = await repo.insert_words(payloads)
        conflicts = detect_translate_mismatches(rows)
        conflict_payloads = [c.to_conflict_payload() for c in conflicts]
        await repo.insert_conflicts(conflict_payloads)
        await repo.commit()

        stats["conflicts"] = len(conflicts)
        stats["words_written"] = len(rows)
        return stats
