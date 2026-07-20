"""无意义词片 / 英文连接词过滤 — 拆分预览用。"""

from __future__ import annotations

import re
from typing import Final

# 中文侧：连接/虚词/标点（可扩展）
CN_STOPWORDS: Final[frozenset[str]] = frozenset(
    {
        "与",
        "的",
        "和",
        "及",
        "或",
        "等",
        "之",
        "并",
        "且",
        "／",
        "/",
        "、",
        "，",
        ",",
        "。",
        ".",
        "·",
        ":",
        "：",
        ";",
        "；",
        "(",
        ")",
        "（",
        "）",
        "[",
        "]",
        "【",
        "】",
        "-",
        "—",
        "_",
        " ",
        "\t",
    }
)

# 英文侧：对齐前去掉
EN_STOPWORDS: Final[frozenset[str]] = frozenset(
    {
        "and",
        "or",
        "of",
        "the",
        "a",
        "an",
        "to",
        "for",
        "in",
        "on",
        "at",
        "by",
        "&",
        "/",
        "-",
    }
)

_PUNCT_RE = re.compile(r"^[\W_]+$", re.UNICODE)
_EN_SPLIT_RE = re.compile(r"[\s/|,;]+")


def is_cn_stopword(token: str) -> bool:
    """判断中文切分 token 是否无意义（连接词/标点等）。"""
    t = (token or "").strip()
    if not t:
        return True
    if t in CN_STOPWORDS:
        return True
    if _PUNCT_RE.match(t):
        return True
    return False


def is_en_stopword(token: str) -> bool:
    """判断英文对齐 token 是否无意义。"""
    t = (token or "").strip()
    if not t:
        return True
    if t.lower() in EN_STOPWORDS:
        return True
    if _PUNCT_RE.match(t):
        return True
    return False


def filter_cn_tokens(tokens: list[str]) -> list[str]:
    """过滤中文无意义词片，保持顺序（不去重；对齐场景用）。"""
    return [t for t in tokens if not is_cn_stopword(t)]


def normalize_cn_lexemes(tokens: list[str] | None) -> list[str]:
    """词片规范化 SSOT：去空白 → 保序去重 → 滤停用词。

    写入 ``segment_trace.jieba`` / 回写术语字典前只经此函数。
    顺序锁定为先去重再过滤，避免同一词片重复做停用词判定。
    """
    if not tokens:
        return []
    seen: set[str] = set()
    unique: list[str] = []
    for raw in tokens:
        t = (raw or "").strip()
        if not t or t in seen:
            continue
        seen.add(t)
        unique.append(t)
    return [t for t in unique if not is_cn_stopword(t)]


def split_en_raw_tokens(text: str) -> list[str]:
    """英文按空白/分隔符切开（不去停用词）。"""
    raw = (text or "").strip()
    if not raw:
        return []
    return [p for p in _EN_SPLIT_RE.split(raw) if p]


def tokenize_en_for_align(text: str) -> list[str]:
    """将英文译文切成对齐用 token，并去掉连接词。"""
    return [p for p in split_en_raw_tokens(text) if not is_en_stopword(p)]


def en_stopword_drop_count(text: str) -> tuple[list[str], int]:
    """返回过滤后 EN token 与丢掉的停用词个数。"""
    raw = split_en_raw_tokens(text)
    kept = [p for p in raw if not is_en_stopword(p)]
    return kept, len(raw) - len(kept)
