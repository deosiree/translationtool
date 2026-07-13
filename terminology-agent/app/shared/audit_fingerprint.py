"""待审核 audit 写入去重指纹 — DB 与 localStorage 共用规则。"""

from __future__ import annotations

from typing import Any


def _norm(value: str | None) -> str:
    return (value or "").strip()


def _norm_confidence(confidence: float | None) -> float | None:
    if confidence is None:
        return None
    return round(float(confidence), 3)


def audit_write_fingerprint(
    *,
    source_text: str,
    entry_comment: str | None = None,
    suggested_translation: str | None = None,
    target_lang: str | None = None,
    department: str | None = None,
    retrieval_method: str | None = None,
    confidence: float | None = None,
) -> tuple[Any, ...]:
    """生成写入去重指纹；字段完全一致则视为重复、跳过 INSERT。"""
    return (
        _norm(source_text),
        _norm(entry_comment),
        _norm(suggested_translation),
        _norm(target_lang),
        _norm(department),
        _norm(retrieval_method),
        _norm_confidence(confidence),
    )


def fingerprint_from_audit_record(record: Any) -> tuple[Any, ...]:
    """从 ORM 或 dict 提取指纹字段。"""
    if isinstance(record, dict):
        getter = record.get
    else:
        getter = lambda k, default=None: getattr(record, k, default)
    return audit_write_fingerprint(
        source_text=getter("source_text") or "",
        entry_comment=getter("entry_comment"),
        suggested_translation=getter("suggested_translation"),
        target_lang=getter("target_lang"),
        department=getter("department"),
        retrieval_method=getter("retrieval_method"),
        confidence=getter("confidence"),
    )
