"""目标语种 → t_entry_info 外键列映射。"""

from __future__ import annotations

from app.core.exceptions import ApiError
from app.shared.term_word.etl.constants import LANG_TRANS_ID_ATTR


def resolve_trans_id_attr(target_lang: str | None) -> str:
    """将 target_lang（如「俄文」）解析为 entry_info 上的 trans_id 列名。

    Args:
        target_lang: 目标语种中文名或别名。

    Returns:
        ORM 属性名，如 ``ru_trans_id``。

    Raises:
        ApiError: 语种为空或不支持。
    """
    if not target_lang or not str(target_lang).strip():
        raise ApiError("目标语种为空，无法回写工作台翻译")
    attr = LANG_TRANS_ID_ATTR.get(str(target_lang).strip())
    if not attr:
        raise ApiError(f"不支持的目标语种「{target_lang}」，无法回写工作台翻译")
    return attr
