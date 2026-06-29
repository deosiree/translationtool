"""term_word 矛盾检测 — scope = (word, comment, target_lang)，不含 department。"""

from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field
from typing import Any

from app.models.word_constants import CONFLICT_RESOLUTION_OPEN, CONFLICT_TYPE_TRANSLATE_MISMATCH


@dataclass
class ConflictGroup:
    """同一消歧键下 translate 不一致的一组 term_word 行。"""

    word: str
    comment: str
    target_lang: str
    records: list[Any] = field(default_factory=list)

    @property
    def distinct_translates(self) -> set[str]:
        return {_field(r, "translate", "") for r in self.records}

    def to_conflict_payload(self) -> dict[str, Any]:
        """构造 term_word_conflict 写入字段。"""
        word_ids: list[str] = []
        task_ids: set[str] = set()
        product_ids: set[str] = set()
        entry_info_ids: set[str] = set()

        for rec in self.records:
            wid = _field(rec, "id", None)
            if wid:
                word_ids.append(str(wid))
            tid = _field(rec, "task_id", None)
            if tid:
                task_ids.add(str(tid))
            pid = _field(rec, "product_id", None)
            if pid:
                product_ids.add(str(pid))
            eid = _field(rec, "source_entry_info_id", None)
            if eid:
                entry_info_ids.add(str(eid))

        return {
            "word": self.word,
            "comment": self.comment,
            "target_lang": self.target_lang,
            "word_ids": word_ids,
            "conflict_type": CONFLICT_TYPE_TRANSLATE_MISMATCH,
            "resolution": CONFLICT_RESOLUTION_OPEN,
            "task_ids": sorted(task_ids),
            "product_ids": sorted(product_ids),
            "source_entry_info_ids": sorted(entry_info_ids),
        }


def _field(record: Any, name: str, default: Any = "") -> Any:
    if isinstance(record, dict):
        return record.get(name, default)
    return getattr(record, name, default)


def disambiguation_key(record: Any) -> tuple[str, str, str]:
    """消歧键 — 不含 department。"""
    word = _field(record, "word", "")
    comment = _field(record, "comment", "")
    target_lang = _field(record, "target_lang", "")
    return (str(word), str(comment or ""), str(target_lang))


def detect_translate_mismatches(records: list[Any]) -> list[ConflictGroup]:
    """按 (word, comment, target_lang) 分组；组内 translate 去重 >1 则矛盾。"""
    groups: dict[tuple[str, str, str], list[Any]] = defaultdict(list)
    for rec in records:
        groups[disambiguation_key(rec)].append(rec)

    conflicts: list[ConflictGroup] = []
    for (word, comment, target_lang), items in groups.items():
        translates = {_field(r, "translate", "") for r in items}
        if len(translates) > 1:
            conflicts.append(
                ConflictGroup(word=word, comment=comment, target_lang=target_lang, records=items)
            )
    return conflicts
