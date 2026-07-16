"""术语学习 pending 列表软去重 — 同词条键仅保留最新一条。"""

from __future__ import annotations

from typing import Any, Sequence


def pending_entry_key(record: Any) -> tuple[str, str, str, str]:
    """去重键：源词条 + comment + 目标语 + 部门（忽略 LLM 译文抖动）。"""
    return (
        (getattr(record, "source_text", None) or "").strip(),
        (getattr(record, "entry_comment", None) or "").strip(),
        (getattr(record, "target_lang", None) or "").strip(),
        (getattr(record, "department", None) or "").strip(),
    )


def dedupe_pending_by_entry_key(records: Sequence[Any]) -> list[Any]:
    """按 pending_entry_key 去重；输入须已按 created_at 降序，保留首次出现（最新）。"""
    seen: set[tuple[str, str, str, str]] = set()
    out: list[Any] = []
    for record in records:
        key = pending_entry_key(record)
        if key in seen:
            continue
        seen.add(key)
        out.append(record)
    return out
