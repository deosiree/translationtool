"""全量建 term_word 索引 — CLI 薄壳。

用法（terminology-agent 根目录）：
  python -m scripts.build_word_index --dry-run
  python -m scripts.build_word_index --rebuild
  python -m scripts.build_word_index --lang 俄文
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

from app.shared.term_word.etl.build import build_word_index


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
