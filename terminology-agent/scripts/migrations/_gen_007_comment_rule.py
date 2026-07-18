"""Generate 007_comment_rule.sql from _comment_rule_seed.json."""
from __future__ import annotations

import json
import uuid
from pathlib import Path

HERE = Path(__file__).resolve().parent
SEED = HERE / "_comment_rule_seed.json"
OUT = HERE / "007_comment_rule.sql"


def esc(s: str | None) -> str:
    if s is None:
        return "NULL"
    return "'" + str(s).replace("\\", "\\\\").replace("'", "''") + "'"


def main() -> None:
    rows = json.loads(SEED.read_text(encoding="utf-8"))
    lines: list[str] = [
        "-- comment_rule：Comment 场景规则（低频配置）",
        "-- 种子来自 comment对应场景及规则.xlsx",
        "",
        "SET NAMES utf8mb4;",
        "",
        "CREATE TABLE IF NOT EXISTS comment_rule (",
        "  id VARCHAR(64) NOT NULL PRIMARY KEY,",
        "  comment_key VARCHAR(128) NOT NULL COMMENT 'comment 键，如 tabBarTitle',",
        "  entry_source VARCHAR(255) NULL COMMENT '词条来源',",
        "  scene TEXT NULL COMMENT '场景',",
        "  rule_text TEXT NULL COMMENT '规则',",
        "  prefer_abbr TINYINT(1) NOT NULL DEFAULT 0 COMMENT '优先缩写',",
        "  case_type VARCHAR(32) NULL COMMENT 'SentenceCase|TitleCase',",
        "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,",
        "  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,",
        "  KEY idx_comment_key (comment_key),",
        "  KEY idx_prefer_abbr (prefer_abbr)",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Comment 场景规则';",
        "",
        "-- 幂等种子：仅当表为空时插入",
        "INSERT INTO comment_rule (id, comment_key, entry_source, scene, rule_text, prefer_abbr, case_type)",
        "SELECT * FROM (",
    ]
    for i, r in enumerate(rows):
        rid = uuid.uuid4().hex[:16]
        case = esc(r["case_type"]) if r.get("case_type") else "NULL"
        src = esc(r["source"]) if r.get("source") else "NULL"
        scene = esc(r["scene"]) if r.get("scene") else "NULL"
        tips = esc(r["tips"]) if r.get("tips") else "NULL"
        sel = (
            f"  SELECT {esc(rid)} AS id, {esc(r['comment'])} AS comment_key, "
            f"{src} AS entry_source, {scene} AS scene, {tips} AS rule_text, "
            f"{int(r['prefer_abbr'])} AS prefer_abbr, {case} AS case_type"
        )
        if i < len(rows) - 1:
            sel += " UNION ALL"
        lines.append(sel)
    lines.extend(
        [
            ") AS seed",
            "WHERE (SELECT COUNT(*) FROM comment_rule) = 0;",
            "",
        ]
    )
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
