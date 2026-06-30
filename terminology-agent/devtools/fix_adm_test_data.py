"""ADM 手动测试数据修复 — visual_range、触发词条对齐、3B 种子校验。

用法（terminology-agent 根目录）：
  python -m devtools.fix_adm_test_data --dry-run
  python -m devtools.fix_adm_test_data --apply
"""

from __future__ import annotations

import argparse
import asyncio
import sys
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
    "ADM/S03-文件系统资源": "ADM/S03-文件系统资源",
    "ADM/S03 文件系统资源": "ADM/S03-文件系统资源",
    # S03 词级：去掉「词-」使 Trie 能从复合句切分
    "ADM/S03-词-文件": "ADM/S03-文件",
    "ADM/S03-词-系统": "ADM/S03-系统",
    "ADM/S03-词-资源": "ADM/S03-资源",
    # 复合触发：种子 token 直接拼接，避免全局词「系统/资源」抢匹配
    "ADM/S03-文件系统资源": "ADM/S03-文件ADM/S03-系统ADM/S03-资源",
    "ADM/3B-文件系统": "ADM/3B-文件ADM/3B-系统",
}

# 3B 专测：种子 + 触发（仅校验是否存在，不自动插入 entry_info）
REQUIRED_3B_SEEDS = ("ADM/3B-文件", "ADM/3B-系统")
REQUIRED_3B_TRIGGER = "ADM/3B-文件ADM/3B-系统"


async def main(*, apply: bool, dry_run: bool) -> None:
    from sqlalchemy import select, update, text
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

        print("\n=== 2c. term_word word 同步（Grep Trie）===")
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

        print("\n=== 3. 3B 种子/触发校验 ===")
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

        tq = select(EntryInfo).where(
            EntryInfo.entry == REQUIRED_3B_TRIGGER,
            EntryInfo.is_delete == 0,
        )
        trigger = (await session.execute(tq)).scalars().first()
        print(
            f"  trigger {REQUIRED_3B_TRIGGER}: "
            f"{'OK' if trigger else 'MISSING — 请在 admin-proj 创建触发词条（译文留空）'}"
        )

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
