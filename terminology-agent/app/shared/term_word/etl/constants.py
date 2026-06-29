"""ETL 侧常量 — t_translate.type → t_entry_info 外键列映射。"""

from typing import Final

LANG_TRANS_ID_ATTR: Final[dict[str, str]] = {
    "中文": "zh_trans_id",
    "英文": "en_trans_id",
    "en": "en_trans_id",
    "俄文": "ru_trans_id",
    "法文": "fra_trans_id",
    "西文": "spa_trans_id",
}
