"""term_word 常量 — 语种映射与 status 枚举。"""

from typing import Final

# t_translate.type → t_entry_info 外键列
LANG_TRANS_ID_ATTR: Final[dict[str, str]] = {
    "中文": "zh_trans_id",
    "英文": "en_trans_id",
    "en": "en_trans_id",
    "俄文": "ru_trans_id",
    "法文": "fra_trans_id",
    "西文": "spa_trans_id",
}

WORD_STATUS_APPROVED: Final[str] = "approved"
WORD_STATUS_PENDING: Final[str] = "pending"
WORD_STATUS_DEPRECATED: Final[str] = "deprecated"

CONFLICT_TYPE_TRANSLATE_MISMATCH: Final[str] = "translate_mismatch"
CONFLICT_RESOLUTION_OPEN: Final[str] = "open"
