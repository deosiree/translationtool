"""ADM 预翻译矩阵验收 — 对关键词条跑 PreTranslateGraph 并打印/断言检索路径。

用法：
  python -m devtools.verify_adm_pretranslate
  python -m devtools.verify_adm_pretranslate --strict
"""

from __future__ import annotations

import argparse
import asyncio
import re
import sys
from pathlib import Path

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

# (source_text, entry_comment, expected_retrieval_regex, expected_source_regex, expected_review)
CASES = [
    ("ADM/R01-RAG精确", "", r"^exact$", r"^term$", r"^auto_approved$"),
    ("ADM/R04-RAGGREP一致", "", r"^exact$", r"^term$", r"^auto_approved$"),
    ("ADM/S02-RAG模糊-用户管理系统", "ADM-S02", r"^(fuzzy|none)$", r"^(llm|term)$", r"^needs_human$"),
    ("文件、系统、资源", "", r"^decomposed$", r"^hybrid$", r"^(needs_human|auto_approved)$"),
    ("文件与系统", "", r"^decomposed$", r"^hybrid$", r"^(needs_human|auto_approved)$"),
    ("T99-全新未收录", "ADM-T99", r"^none$", r"^llm$", r"^needs_human$"),
]

DEPARTMENT = "通用平台部"
TARGET_LANG = "英文"
THRESHOLD = 0.8


def _matches(value: str | None, pattern: str) -> bool:
    return bool(re.match(pattern, value or ""))


async def main(*, strict: bool) -> int:
    from app.graph.pre_translate.runner import PreTranslateGraph
    from app.models.database import AsyncSessionLocal

    graph = PreTranslateGraph()
    print(f"{'词条':<36} {'comment':<10} {'retrieval':<12} {'source':<8} {'review':<14} {'conf':<6} OK")
    print("-" * 120)

    failures: list[str] = []

    for entry, comment, exp_ret, exp_src, exp_review in CASES:
        async with AsyncSessionLocal() as session:
            final = await graph.run(
                source_text=entry,
                target_lang=TARGET_LANG,
                department=DEPARTMENT,
                confidence_threshold=THRESHOLD,
                entry_comment=comment,
                session=session,
            )
        retrieval = final.get("retrieval_method", "-")
        source = final.get("translation_source", "-")
        review = final.get("review_status", "-")
        conf = final.get("confidence", 0)
        ok = (
            _matches(retrieval, exp_ret)
            and _matches(source, exp_src)
            and _matches(review, exp_review)
        )
        if not ok:
            failures.append(
                f"{entry}: retrieval={retrieval} source={source} review={review} "
                f"(expected ret={exp_ret} src={exp_src} review={exp_review})"
            )
        mark = "OK" if ok else "FAIL"
        print(
            f"{entry:<36} {comment or '-':<10} {retrieval:<12} {source:<8} {review:<14} "
            f"{conf:<6.2f} {mark}"
        )

    if failures:
        print("\n=== FAILURES ===")
        for line in failures:
            print(f"  {line}")
        if strict:
            return 1
    elif strict:
        print("\nAll ADM matrix cases passed.")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--strict",
        action="store_true",
        help="期望不匹配时 exit 1",
    )
    args = parser.parse_args()
    raise SystemExit(asyncio.run(main(strict=args.strict)))
