"""ADM 预翻译矩阵验收 — 对关键词条跑 PreTranslateGraph 并打印检索路径。

用法：
  python -m devtools.verify_adm_pretranslate
"""

from __future__ import annotations

import asyncio
import sys
from pathlib import Path

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

CASES = [
    ("ADM/R01-RAG精确", "exact", "term"),
    ("ADM/R04-RAGGREP一致", "exact", "term"),
    ("ADM/S02-RAG模糊-用户管理系统", "fuzzy|none", "llm|term"),
    ("ADM/S03-文件ADM/S03-系统ADM/S03-资源", "hybrid|decomposed|grep", "hybrid"),
    ("ADM/3B-文件ADM/3B-系统", "decomposed", "hybrid"),
    ("ADM/T99-全新未收录", "none", "llm"),
]

DEPARTMENT = "通用平台部"
TARGET_LANG = "英文"
THRESHOLD = 0.8


async def main() -> None:
    from app.graph.pre_translate.runner import PreTranslateGraph
    from app.models.database import AsyncSessionLocal

    graph = PreTranslateGraph()
    print(f"{'词条':<36} {'retrieval':<12} {'source':<8} {'review':<14} {'confidence':<6} 译文")
    print("-" * 110)

    for entry, _exp_ret, _exp_src in CASES:
        async with AsyncSessionLocal() as session:
            final = await graph.run(
                source_text=entry,
                target_lang=TARGET_LANG,
                department=DEPARTMENT,
                confidence_threshold=THRESHOLD,
                entry_comment="",
                session=session,
            )
            trans = (final.get("suggested_translation") or "")[:40]
            print(
                f"{entry:<36} "
                f"{final.get('retrieval_method', '-'):<12} "
                f"{final.get('translation_source', '-'):<8} "
                f"{final.get('review_status', '-'):<14} "
                f"{final.get('confidence', 0):<6.2f} "
                f"{trans}"
            )


if __name__ == "__main__":
    asyncio.run(main())
