"""ADM 数据与 Trie 切分验收（同步，不跑完整 LangGraph）。"""

from __future__ import annotations

import sys
from pathlib import Path

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

import pymysql
from app.shared.term_word.trie import Trie

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
        "ADM/S03-文件ADM/S03-系统ADM/S03-资源",
        ["ADM/S03-文件", "ADM/S03-系统", "ADM/S03-资源"],
    ),
    ("ADM/3B-文件ADM/3B-系统", ["ADM/3B-文件", "ADM/3B-系统"]),
]


def main() -> None:
    conn = pymysql.connect(
        host="127.0.0.1",
        port=3306,
        user="root",
        password="123456",
        database="translationtool",
        charset="utf8mb4",
    )
    cur = conn.cursor()

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
        print(f"  {'OK' if ok else 'MISS'} {entry:<30} translate={tr} term_word={tw}  ({label})")

    print("\n=== Trie 切分 ===")
    cur.execute(
        "SELECT DISTINCT word FROM term_word WHERE status='approved' AND target_lang=%s",
        (TARGET_LANG,),
    )
    words = [r[0] for r in cur.fetchall()]
    trie = Trie()
    trie.build_from_entries(words)
    for text, expected_parts in SEGMENT_CASES:
        segs = trie.segment(text)
        hit = all(p in segs for p in expected_parts)
        print(f"  {'OK' if hit else 'FAIL'} {text}")
        print(f"       segments={segs}")
        print(f"       expect parts={expected_parts}")

    conn.close()


if __name__ == "__main__":
    main()
