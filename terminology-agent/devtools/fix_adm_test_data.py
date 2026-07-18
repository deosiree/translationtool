"""ADM 手动测试数据修复 — visual_range、触发词条对齐、词片种子、comment 隔离。

用法（terminology-agent 根目录）：
  python -m devtools.fix_adm_test_data --dry-run
  python -m devtools.fix_adm_test_data --apply
"""

from __future__ import annotations

import argparse
import asyncio
import sys
import uuid
from pathlib import Path

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

DEPARTMENT = "通用平台部"
TARGET_LANG = "英文"

# 触发词条 entry 应修正为与种子逐字一致（旧文本 → 新文本）
ENTRY_RENAMES: dict[str, str] = {
    "ADM/S01-RAG精确": "ADM/R01-RAG精确",
    "ADM/S04-RAGGREP一致": "ADM/R04-RAGGREP一致",
    "ADM/S04-RAGGREP—一致": "ADM/R04-RAGGREP一致",
    "ADM/S02-RAG模糊-用户管理系统": "ADM/S02-RAG模糊-用户管理系统",
    "ADM/S02-RAG模糊 用户管理系统": "ADM/S02-RAG模糊-用户管理系统",
    "ADM/S03 文件系统资源": "文件、系统、资源",
    "ADM/S03-文件系统资源": "文件、系统、资源",
    "ADM/S03-文件与系统资源": "文件、系统、资源",
    "ADM/S03-文件、系统、资源": "文件、系统、资源",
    "ADM/S03-文件ADM/S03-系统ADM/S03-资源": "文件、系统、资源",
    "ADM/3B-文件系统": "文件与系统",
    "ADM/3B-文件ADM/3B-系统": "文件与系统",
    "ADM/3B-文件与系统": "文件与系统",
    "ADM/T99-全新未收录": "T99-全新未收录",
}

REQUIRED_3B_SEEDS = ("ADM/3B-文件", "ADM/3B-系统")
REQUIRED_3B_TRIGGER = "文件与系统"
REQUIRED_S03_TRIGGER = "文件、系统、资源"
REQUIRED_T99_TRIGGER = "T99-全新未收录"

# comment="" 词片种子 — decomposed / 3B 路径（department 过滤后唯一 approved）
ADM_LEXEME_SEEDS: tuple[tuple[str, str, str], ...] = (
    ("文件", "File", "adm-lexeme-file-en"),
    ("系统", "System", "adm-lexeme-system-en"),
    ("资源", "Resource", "adm-lexeme-resource-en"),
)

# 整句 Grep 种子（R04 RAG+Grep 一致场景，comment=""）
ADM_WHOLE_GREP_SEEDS: tuple[tuple[str, str, str], ...] = (
    ("ADM/R04-RAGGREP一致", "ADM RAG Grep Same", "adm-grep-r04-en"),
)


def _new_translate_id() -> str:
    return uuid.uuid4().hex[:32]


async def _seed_lexeme(
    session,
    *,
    word: str,
    translate: str,
    seed_id: str,
    apply: bool,
) -> None:
    from sqlalchemy import select

    from app.models.word import TermWord
    from app.models.word_constants import WORD_STATUS_APPROVED, WORD_STATUS_REJECTED

    wq = select(TermWord).where(
        TermWord.word == word,
        TermWord.target_lang == TARGET_LANG,
        TermWord.comment == "",
        TermWord.department == DEPARTMENT,
        TermWord.status == WORD_STATUS_APPROVED,
        TermWord.id != seed_id,
    )
    dupes = (await session.execute(wq)).scalars().all()
    for row in dupes:
        print(f"  deprecate duplicate lexeme: {word!r} id={row.id}")
        if apply:
            row.status = WORD_STATUS_REJECTED

    existing = await session.get(TermWord, seed_id)
    if existing is None:
        print(f"  insert lexeme: {word!r} -> {translate!r}")
        if apply:
            session.add(
                TermWord(
                    id=seed_id,
                    word=word,
                    comment="",
                    translate=translate,
                    target_lang=TARGET_LANG,
                    department=DEPARTMENT,
                    source_translate_id=_new_translate_id(),
                    status=WORD_STATUS_APPROVED,
                )
            )
    else:
        print(f"  update lexeme: {word!r} -> {translate!r}")
        if apply:
            existing.word = word
            existing.translate = translate
            existing.comment = ""
            existing.target_lang = TARGET_LANG
            existing.department = DEPARTMENT
            existing.status = WORD_STATUS_APPROVED


async def _seed_whole_grep(
    session,
    *,
    word: str,
    translate: str,
    seed_id: str,
    apply: bool,
) -> None:
    await _seed_lexeme(session, word=word, translate=translate, seed_id=seed_id, apply=apply)


async def _set_entry_comment_for_triggers(session, *, apply: bool) -> None:
    """S02/T99 用专用 comment 隔离 Grep，避免命中全局 term_word。"""
    from sqlalchemy import select

    from app.models.term import EntryInfo

    comment_map = {
        "ADM/S02-RAG模糊-用户管理系统": "ADM-S02",
        REQUIRED_T99_TRIGGER: "ADM-T99",
    }
    for entry, comment in comment_map.items():
        rows = (
            await session.execute(
                select(EntryInfo).where(
                    EntryInfo.entry == entry,
                    EntryInfo.is_delete == 0,
                )
            )
        ).scalars().all()
        for ei in rows:
            if (ei.comment or "") != comment:
                print(f"  set entry_info comment: {entry!r} -> {comment!r}")
                if apply:
                    ei.comment = comment


async def main(*, apply: bool, dry_run: bool) -> None:
    from sqlalchemy import select

    from app.models.database import AsyncSessionLocal
    from app.models.term import EntryInfo, TranslateEntry

    async with AsyncSessionLocal() as session:
        print("=== 1. visual_range 修复（ADM/% 已审定英文）===")
        q = (
            select(TranslateEntry)
            .where(
                TranslateEntry.delete_state == 0,
                TranslateEntry.entry.like("ADM/%"),
                TranslateEntry.type == TARGET_LANG,
                TranslateEntry.translate_state == "3",
            )
        )
        rows = (await session.execute(q)).scalars().all()
        vr_fix = 0
        for row in rows:
            if row.visual_range != DEPARTMENT:
                print(f"  fix visual_range: {row.entry!r} {row.visual_range!r} -> {DEPARTMENT!r}")
                if apply:
                    row.visual_range = DEPARTMENT
                vr_fix += 1
        print(f"  visual_range 待修复: {vr_fix}")

        print("\n=== 2. 触发词条 entry 对齐（t_entry_info）===")
        rename_count = 0
        for old_entry, new_entry in ENTRY_RENAMES.items():
            if old_entry == new_entry:
                continue
            eq = select(EntryInfo).where(
                EntryInfo.entry == old_entry,
                EntryInfo.is_delete == 0,
            )
            entries = (await session.execute(eq)).scalars().all()
            for ei in entries:
                print(f"  rename entry_info: {old_entry!r} -> {new_entry!r} (id={ei.id})")
                if apply:
                    ei.entry = new_entry
                rename_count += 1
        print(f"  entry 待重命名: {rename_count}")

        print("\n=== 2b. t_translate entry 同步（与 entry_info 对齐）===")
        tr_rename = 0
        for old_entry, new_entry in ENTRY_RENAMES.items():
            if old_entry == new_entry:
                continue
            tq = select(TranslateEntry).where(
                TranslateEntry.entry == old_entry,
                TranslateEntry.delete_state == 0,
                TranslateEntry.type == TARGET_LANG,
            )
            trans_rows = (await session.execute(tq)).scalars().all()
            for tr in trans_rows:
                print(f"  rename t_translate: {old_entry!r} -> {new_entry!r} (id={tr.id})")
                if apply:
                    tr.entry = new_entry
                tr_rename += 1
        print(f"  t_translate 待重命名: {tr_rename}")

        print("\n=== 2c. term_word word 同步（Grep 整句）===")
        from app.models.word import TermWord

        tw_rename = 0
        for old_entry, new_entry in ENTRY_RENAMES.items():
            if old_entry == new_entry:
                continue
            wq = select(TermWord).where(
                TermWord.word == old_entry,
                TermWord.target_lang == TARGET_LANG,
            )
            word_rows = (await session.execute(wq)).scalars().all()
            for tw in word_rows:
                print(f"  rename term_word: {old_entry!r} -> {new_entry!r} (id={tw.id})")
                if apply:
                    tw.word = new_entry
                tw_rename += 1
        print(f"  term_word 待重命名: {tw_rename}")

        print("\n=== 3. ADM 词片 term_word 种子（comment='' + department 唯一）===")
        for word, translate, seed_id in ADM_LEXEME_SEEDS:
            await _seed_lexeme(session, word=word, translate=translate, seed_id=seed_id, apply=apply)

        print("\n=== 4. ADM 整句 Grep 种子 ===")
        for word, translate, seed_id in ADM_WHOLE_GREP_SEEDS:
            await _seed_whole_grep(session, word=word, translate=translate, seed_id=seed_id, apply=apply)

        print("\n=== 5. S02/T99 entry comment 隔离 Grep ===")
        await _set_entry_comment_for_triggers(session, apply=apply)

        print("\n=== 6. 触发/种子校验 ===")
        for seed in REQUIRED_3B_SEEDS:
            tq = select(TranslateEntry).where(
                TranslateEntry.entry == seed,
                TranslateEntry.delete_state == 0,
                TranslateEntry.translate_state == "3",
                TranslateEntry.type == TARGET_LANG,
            )
            hit = (await session.execute(tq)).scalars().first()
            status = "OK" if hit else "MISSING — 请在前端 admin/dev 创建并审定"
            print(f"  seed {seed}: {status}")

        for trigger in (REQUIRED_3B_TRIGGER, REQUIRED_S03_TRIGGER, REQUIRED_T99_TRIGGER):
            tq = select(EntryInfo).where(
                EntryInfo.entry == trigger,
                EntryInfo.is_delete == 0,
            )
            hit = (await session.execute(tq)).scalars().first()
            print(f"  trigger {trigger}: {'OK' if hit else 'MISSING'}")

        if apply and not dry_run:
            await session.commit()
            print("\n已提交。")
        else:
            await session.rollback()
            print("\n(dry-run，未写入)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="写入数据库")
    parser.add_argument("--dry-run", action="store_true", help="仅预览（默认）")
    args = parser.parse_args()
    apply = args.apply and not args.dry_run
    asyncio.run(main(apply=apply, dry_run=not apply))
