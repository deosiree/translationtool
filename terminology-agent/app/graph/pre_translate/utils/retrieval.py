"""预翻译检索纯函数 — 相似度、占位符处理与语种解析。"""

from __future__ import annotations

import re
from difflib import SequenceMatcher


def strip_placeholders(text: str) -> str:
    """去掉 %1/%2 等占位符后再做相似度比较，避免工业 i18n 词条误判。

    Args:
        text: 原始词条文本。

    Returns:
        去除占位符并 strip 后的文本。
    """
    return re.sub(r"%\d+", "", text or "").strip()


def similarity(a: str, b: str) -> float:
    """基于 SequenceMatcher 的字符级相似度，范围 0~1。

    Args:
        a: 比较文本 A。
        b: 比较文本 B。

    Returns:
        相似度分数；任一为空则返回 0.0。
    """
    if not a or not b:
        return 0.0
    return SequenceMatcher(None, a, b).ratio()


def fuzzy_confidence(best_score: float) -> float:
    """模糊匹配经验公式：基础分 0.55 + 相似度贡献，上限 0.95。

    Args:
        best_score: 最佳候选相似度（0~1）。

    Returns:
        映射后的置信度，保留三位小数。
    """
    return round(min(0.55 + best_score * 0.4, 0.95), 3)


def parse_target_lang(translate_type: str | None) -> str:
    """从任务 translateType（如「中文-俄文」）解析目标语种。

    Args:
        translate_type: 工作台任务 translateType 字段。

    Returns:
        目标语种字符串；无法解析时返回空串或原值。
    """
    if not translate_type:
        return ""
    parts = str(translate_type).split("-")
    return parts[1] if len(parts) > 1 else translate_type
