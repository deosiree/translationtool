"""t_translate ↔ t_entry_info 关联 — comment 仅来自 entry_info.comment。"""

from __future__ import annotations

from typing import Any

from app.models.word_constants import (
    WORD_STATUS_PENDING,
    WORD_STATUS_UNTRANSLATED,
    WORD_STATUS_VALUES,
)
from app.shared.term_word.etl.constants import LANG_TRANS_ID_ATTR


def trans_id_attr_for_lang(target_lang: str | None) -> str | None:
    """目标语种 → entry_info 外键列名。"""
    if not target_lang:
        return None
    return LANG_TRANS_ID_ATTR.get(target_lang.strip())


def normalize_comment(comment: str | None) -> str:
    """消歧 comment；无则空串（不用 t_translate.remark）。"""
    return (comment or "").strip()


def entry_info_links_translate(
    entry_info: Any,
    translate_id: str,
    target_lang: str | None,
) -> bool:
    """entry_info 是否通过语种外键指向该 translate。"""
    attr = trans_id_attr_for_lang(target_lang)
    if not attr:
        return False
    trans_id = getattr(entry_info, attr, None)
    return trans_id is not None and str(trans_id) == str(translate_id)


def word_status_from_translate_state(translate_state: str | None) -> str:
    """透传 translate_state（0/1/2/3）；非法或空 → 待审核。"""
    raw = str(translate_state or "").strip()
    if raw in WORD_STATUS_VALUES:
        return raw
    if raw == "":
        return WORD_STATUS_UNTRANSLATED
    return WORD_STATUS_PENDING


def build_term_word_payload(
    *,
    translate: Any,
    entry_info: Any,
) -> dict[str, Any] | None:
    """从 translate + entry_info 构造 term_word 写入字段。

    P2 规则：word == entry 整句；必须有 entry_info；comment 仅 entry_info.comment。
    """
    entry = (getattr(translate, "entry", None) or "").strip()
    translation = (getattr(translate, "translate", None) or "").strip()
    target_lang = (getattr(translate, "type", None) or "").strip()
    if not entry or not translation or not target_lang:
        return None

    word = entry

    return {
        "word": word,
        "comment": normalize_comment(getattr(entry_info, "comment", None)),
        "translate": translation,
        "target_lang": target_lang,
        "department": getattr(translate, "visual_range", None),
        "source_translate_id": str(getattr(translate, "id", "")),
        "source_entry_info_id": str(getattr(entry_info, "id", "")),
        "task_id": getattr(entry_info, "task_id", None),
        "product_id": getattr(entry_info, "product_id", None),
        "category": None,
        "abbr": None,
        "usage_notes": None,
        "remark3": None,
        "status": word_status_from_translate_state(getattr(translate, "translate_state", None)),
    }


def index_entry_infos_by_trans_id(entry_infos: list[Any]) -> dict[tuple[str, str], list[Any]]:
    """(trans_id, target_lang) → 关联的 entry_info 列表。"""
    index: dict[tuple[str, str], list[Any]] = {}
    for ei in entry_infos:
        if getattr(ei, "is_delete", 0) != 0:
            continue
        for lang, attr in LANG_TRANS_ID_ATTR.items():
            trans_id = getattr(ei, attr, None)
            if trans_id:
                key = (str(trans_id), lang)
                index.setdefault(key, []).append(ei)
    return index
