"""全量建 term_word 索引 — 从 t_translate + t_entry_info 离线导入。

用法（terminology-agent 根目录）：
  python -m devtools.build_word_index --dry-run
  python -m devtools.build_word_index --rebuild
  python -m devtools.build_word_index --lang 俄文
"""

from __future__ import annotations

import argparse
import asyncio
import sys
from pathlib import Path


def _find_agent_root() -> Path:
    candidate = Path(__file__).resolve().parents[1]
    if (candidate / "config" / "settings.py").is_file():
        return candidate
    raise RuntimeError("找不到 terminology-agent 根目录")


_ROOT = _find_agent_root()
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from sqlalchemy import select

from app.models.database import AsyncSessionLocal
from app.models.term import TranslateEntry, EntryInfo
from app.repository.word_repo import WordRepository
from app.word.conflict import detect_translate_mismatches
from app.word.join_entry_info import (
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


def main() -> None:
    parser = argparse.ArgumentParser(description="Build term_word index from t_translate")
    parser.add_argument("--dry-run", action="store_true", help="只统计，不写库")
    parser.add_argument("--rebuild", action="store_true", help="清空后重建")
    parser.add_argument("--lang", dest="target_lang", default=None, help="限定目标语种")
    args = parser.parse_args()

    stats = asyncio.run(
        build_word_index(
            dry_run=args.dry_run,
            rebuild=args.rebuild,
            target_lang=args.target_lang,
        )
    )
    print("term_word build stats:")
    for key, value in stats.items():
        print(f"  {key}: {value}")


if __name__ == "__main__":
    main()
