"""ADM 数据与 jieba 切分验收（同步，不跑完整 LangGraph）。"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

import pymysql
from app.shared.term_word.segment import segment_source_text

DEPARTMENT = "通用平台部"
TARGET_LANG = "英文"

CHECKS = [
    ("ADM/R01-RAG精确", "t_translate exact"),
    ("ADM/R04-RAGGREP一致", "t_translate exact"),
    ("ADM/S03-文件", "term_word lexeme"),
    ("ADM/S03-系统", "term_word lexeme"),
    ("ADM/3B-文件", "term_word 3b lexeme"),
    ("ADM/3B-系统", "term_word 3b lexeme"),
]

SEGMENT_CASES = [
    (
        "文件、系统、资源",
        ["文件", "系统", "资源"],
    ),
    ("文件与系统", ["文件", "系统"]),
]

LEXEME_UNIQUENESS = ["文件", "系统", "资源"]


def main(*, strict: bool) -> int:
    from config.settings import settings

    conn = pymysql.connect(
        host=settings.mysql_host,
        port=settings.mysql_port,
        user=settings.mysql_user,
        password=settings.mysql_password,
        database=settings.mysql_database,
        charset="utf8mb4",
    )
    cur = conn.cursor()
    failures = 0

    print("=== t_translate / term_word 存在性 ===")
    for entry, label in CHECKS:
        cur.execute(
            "SELECT COUNT(*) FROM t_translate WHERE delete_state=0 "
            "AND translate_state='3' AND type=%s AND entry=%s",
            (TARGET_LANG, entry),
        )
        tr = cur.fetchone()[0]
        cur.execute(
            "SELECT COUNT(*) FROM term_word WHERE status='approved' "
            "AND target_lang=%s AND word=%s",
            (TARGET_LANG, entry),
        )
        tw = cur.fetchone()[0]
        ok = tr > 0 or tw > 0
        if not ok:
            failures += 1
        print(f"  {'OK' if ok else 'MISS'} {entry:<30} translate={tr} term_word={tw}  ({label})")

    print("\n=== ADM 词片 comment='' department 唯一性 ===")
    for word in LEXEME_UNIQUENESS:
        cur.execute(
            "SELECT COUNT(*) FROM term_word WHERE status='approved' "
            "AND target_lang=%s AND word=%s AND comment='' AND department=%s",
            (TARGET_LANG, word, DEPARTMENT),
        )
        cnt = cur.fetchone()[0]
        ok = cnt == 1
        if not ok:
            failures += 1
        print(f"  {'OK' if ok else 'FAIL'} {word}: approved rows with dept={DEPARTMENT!r}: {cnt} (expect 1)")

    print("\n=== jieba 切分（segment_source_text）===")
    for text, expected_parts in SEGMENT_CASES:
        segs = [t for t, _, _ in segment_source_text(text)]
        hit = all(p in segs for p in expected_parts)
        if not hit:
            failures += 1
        print(f"  {'OK' if hit else 'FAIL'} {text}")
        print(f"       segments={segs}")
        print(f"       expect parts={expected_parts}")

    conn.close()
    if strict and failures:
        print(f"\n{failures} check(s) failed.")
        return 1
    if strict:
        print("\nAll ADM data checks passed.")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()
    raise SystemExit(main(strict=args.strict))
