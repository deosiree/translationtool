"""ADM 验收数据清理 — 移除多次预翻译/术语学习入库造成的「全变 exact」污染。

根因：auto_approved 回写工作台、术语学习「确认」会把整句写入 t_translate(state=3)；
ETL 或 Grep 又写入 term_word，下次预翻译全部走 RAG exact / Grep 整句命中。

用法（terminology-agent 根目录）：
  python -m devtools.cleanup_adm_test_data --dry-run
  python -m devtools.cleanup_adm_test_data --apply
  python -m devtools.fix_adm_test_data --apply
  python -m devtools.verify_adm_data --strict
  python -m devtools.verify_adm_pretranslate --strict

清理后 UI：术语学习页清空 ADM 相关 pending；工作台需重新点一次 Agent 预翻译观察路径。
浏览器 localStorage 可点「清除本地 Mock」或手动删 agent-pending-audits。
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

# 必须保留的 t_translate 已审定整句（RAG exact / Grep 种子）
KEEP_APPROVED_ENTRIES: frozenset[str] = frozenset(
    {
        "ADM/R01-RAG精确",
        "ADM/R04-RAGGREP一致",
        # compose 子句种子（verify_adm_data 校验项，非触发整句）
        "ADM/3B-文件",
        "ADM/3B-系统",
        "ADM/S03-文件",
        "ADM/S03-系统",
        "ADM/S03-资源",
    }
)

# 必须保留的 term_word.id（fix_adm_test_data 写入，勿删）
KEEP_TERM_WORD_IDS: frozenset[str] = frozenset(
    {
        "adm-lexeme-file-en",
        "adm-lexeme-system-en",
        "adm-lexeme-resource-en",
        "adm-grep-r04-en",
    }
)

# 触发句：不得存在 translate_state=3 的整句术语（否则全变 exact）
TRIGGER_ENTRIES: frozenset[str] = frozenset(
    {
        "ADM/S02-RAG模糊-用户管理系统",
        "文件、系统、资源",
        "文件与系统",
        "T99-全新未收录",
    }
)

# term_agent_audit / term_word 模糊匹配前缀
ADM_SOURCE_PREFIXES = ("ADM/", "T99-", "文件", "文件与", "文件、")


async def main(*, apply: bool) -> None:
    from sqlalchemy import delete, or_, select

    from app.models.database import AsyncSessionLocal
    from app.models.term import TermAgentAudit, TranslateEntry
    from app.models.word import TermWord
    from app.models.word_constants import WORD_STATUS_APPROVED, WORD_STATUS_REJECTED
    from app.repository.trie_cache import clear_trie_cache

    async with AsyncSessionLocal() as session:
        print("=== 1. term_agent_audit（术语学习 pending/历史）===")
        audit_cond = or_(
            TermAgentAudit.source_text.like("ADM/%"),
            TermAgentAudit.source_text.in_(TRIGGER_ENTRIES),
            TermAgentAudit.source_text.like("T99-%"),
        )
        audits = (await session.execute(select(TermAgentAudit).where(audit_cond))).scalars().all()
        print(f"  待删 audit 行: {len(audits)}")
        for row in audits[:20]:
            print(f"    id={row.id} src={row.source_text!r} ret={row.retrieval_method} status={row.review_status}")
        if len(audits) > 20:
            print(f"    ... 另有 {len(audits) - 20} 行")
        if apply and audits:
            await session.execute(delete(TermAgentAudit).where(audit_cond))

        print("\n=== 2. t_translate — 移除不应审定入库的整句 ===")
        # 2a. 触发句若已审定 → 软删
        trig_q = select(TranslateEntry).where(
            TranslateEntry.delete_state == 0,
            TranslateEntry.type == TARGET_LANG,
            TranslateEntry.entry.in_(TRIGGER_ENTRIES),
            TranslateEntry.translate_state == "3",
        )
        trig_rows = (await session.execute(trig_q)).scalars().all()
        for row in trig_rows:
            print(f"  soft-delete trigger approved: {row.entry!r} id={row.id}")
            if apply:
                row.delete_state = 1

        # 2b. ADM/% 中非保留种子的已审定行 → 软删（含重复 R01/R04、旧 Trie 拼接名）
        adm_q = select(TranslateEntry).where(
            TranslateEntry.delete_state == 0,
            TranslateEntry.type == TARGET_LANG,
            TranslateEntry.entry.like("ADM/%"),
            TranslateEntry.translate_state == "3",
        )
        adm_rows = (await session.execute(adm_q)).scalars().all()
        adm_removed = 0
        for row in adm_rows:
            entry = (row.entry or "").strip()
            if entry in KEEP_APPROVED_ENTRIES:
                continue
            print(f"  soft-delete ADM approved: {entry!r} id={row.id}")
            adm_removed += 1
            if apply:
                row.delete_state = 1
        print(f"  ADM 非保留审定行: {adm_removed}")

        # 2c. 触发句 decomposed 污染：有译文但未审定也重置，避免工作台显示旧 Agent 结果
        reset_q = select(TranslateEntry).where(
            TranslateEntry.delete_state == 0,
            TranslateEntry.type == TARGET_LANG,
            TranslateEntry.entry.in_(TRIGGER_ENTRIES),
            TranslateEntry.translate_state.in_(("1", "2")),
        )
        reset_rows = (await session.execute(reset_q)).scalars().all()
        for row in reset_rows:
            print(f"  reset workbench translate_state 0: {row.entry!r} id={row.id} was={row.translate_state}")
            if apply:
                row.translate_state = "0"
                row.audit_suggest = None

        print("\n=== 3. term_word — 整句 Grep 污染 deprecate ===")
        tw_q = select(TermWord).where(
            TermWord.target_lang == TARGET_LANG,
            TermWord.status == WORD_STATUS_APPROVED,
            TermWord.id.not_in(KEEP_TERM_WORD_IDS),
            or_(
                TermWord.word.in_(TRIGGER_ENTRIES),
                TermWord.word.like("ADM/%"),
                TermWord.word.in_(("文件、系统、资源", "文件与系统")),
            ),
        )
        tw_rows = (await session.execute(tw_q)).scalars().all()
        kept_whole = 0
        for row in tw_rows:
            word = (row.word or "").strip()
            if word in KEEP_APPROVED_ENTRIES:
                kept_whole += 1
                continue
            print(f"  reject term_word: {word!r} id={row.id} comment={row.comment!r}")
            if apply:
                row.status = WORD_STATUS_REJECTED
        print(f"  跳过保留种子 term_word: {kept_whole}")

        if apply:
            await session.commit()
            clear_trie_cache()
            print("\n已提交；Trie 进程缓存已清空。")
        else:
            await session.rollback()
            print("\n(dry-run，未写入)")

        print("\n=== 下一步 ===")
        print("  python -m devtools.fix_adm_test_data --apply")
        print("  python -m devtools.verify_adm_data --strict")
        print("  python -m devtools.verify_adm_pretranslate --strict")
        print("  UI: 术语学习「清除本地 Mock」→ 工作台对 6 条 ADM 词条重新 Agent 预翻译")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="清理 ADM 验收污染数据")
    parser.add_argument("--apply", action="store_true", help="执行写入")
    parser.add_argument("--dry-run", action="store_true", help="仅预览（默认）")
    args = parser.parse_args()
    apply = args.apply and not args.dry_run
    asyncio.run(main(apply=apply))
