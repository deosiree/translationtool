"""通用中文分词切界 — jieba 默认词典（非术语库驱动）。"""

from __future__ import annotations

import os
import re

import jieba

_initialized = False

# %N 占位符正则：%\d+
_PLACEHOLDER_PATTERN = re.compile(r"%\d+")
# 分隔符：%N 之间的允许连接字符
_SEPARATORS = frozenset("/_-.")

# 停用词（从百度停用词表加载，含中英文无意义词/符号）
_STOPWORDS_FILE = os.path.join(os.path.dirname(__file__), "stopwords.txt")
_STOPWORDS: frozenset[str] = frozenset()
if os.path.isfile(_STOPWORDS_FILE):
    with open(_STOPWORDS_FILE, encoding="utf-8") as _f:
        _base = {line.strip() for line in _f if line.strip()}
    # 补充术语切分场景特有的单字停用词（百度表未覆盖）
    _EXTRA_STOPWORDS = {
        "第",     # 序数前缀
        "页", "项", "条", "个", "种", "份", "类", "款", "型",  # 量词
        "各",     # 限定词
    }
    _base.update(_EXTRA_STOPWORDS)
    _STOPWORDS = frozenset(_base)

# 有意义 token 判定：仅含中文字符的 token 保留（过滤英文/数字/符号/占位符）
_HAS_CONTENT = re.compile(r"[\u4e00-\u9fff]")

# ---------- 后处理：占位符合并 ----------


def _is_placeholder_token(token: str) -> bool:
    """判断 token 是否为 %\\d+ 占位符（如 %1、%12）。"""
    return bool(_PLACEHOLDER_PATTERN.fullmatch(token))


def _merge_placeholders(
    tokens: list[tuple[str, int, int]],
) -> list[tuple[str, int, int]]:
    """后处理：合并 %N 占位符及其相邻片段。

    两趟：
      第一趟：`%` + 连续数字 → 合并为 %1、%12
      第二趟：相邻 %\d+ 间仅为 /、_、-、. → 合并为 %1/%2 等

    不改变其它中文词界。

    Args:
        tokens: jieba 分词后的 (token, start, end) 列表。

    Returns:
        合并后的 (token, start, end) 列表。
    """
    if not tokens:
        return []

    # ---- 第一趟：合并 % + 连续数字 ----
    pass1: list[tuple[str, int, int]] = []
    i = 0
    while i < len(tokens):
        token, start, end = tokens[i]

        if token == "%" and i + 1 < len(tokens):
            # 尝试合并后续数字
            j = i + 1
            while j < len(tokens) and tokens[j][0].isdigit() and tokens[j][1] == tokens[j - 1][2]:
                j += 1
            if j > i + 1:
                # 至少有一个数字 → 合并
                merged = "".join(tokens[k][0] for k in range(i, j))
                pass1.append((merged, start, tokens[j - 1][2]))
                i = j
                continue

        pass1.append((token, start, end))
        i += 1

    # ---- 第二趟：合并 %N 分隔符 %N ----
    pass2: list[tuple[str, int, int]] = []
    i = 0
    while i < len(pass1):
        token, start, end = pass1[i]

        # 模式 A: 两个 %N 中间是单个分隔符（如 %1/%2）
        if (
            _is_placeholder_token(token)
            and i + 2 < len(pass1)
            and _is_placeholder_token(pass1[i + 2][0])
        ):
            mid_token, mid_start, mid_end = pass1[i + 1]
            next_token, next_start, next_end = pass1[i + 2]
            if (
                len(mid_token) == 1
                and mid_token in _SEPARATORS
                and mid_start == end
                and next_start == mid_end
            ):
                merged = f"{token}{mid_token}{next_token}"
                pass2.append((merged, start, next_end))
                i += 3
                continue

        # 模式 B: %N 后跟分隔符+%+数字（jieba 把分隔符和 % 粘在一起）
        # 如 %1_%2 → jieba: % 1 _% 2 → pass1: %1 _% 2
        # 注意：此模式依赖 jieba 将 "_%" / "-%" 合并为单 token 的行为；
        # jieba 版本/配置变更时可能需要调整此处的匹配逻辑。
        if (
            _is_placeholder_token(token)
            and i + 2 < len(pass1)
            and pass1[i + 2][0].isdigit()
            and pass1[i + 1][1] == end
            and pass1[i + 2][1] == pass1[i + 1][2]
        ):
            mid_token, mid_start, mid_end = pass1[i + 1]
            digit_token, digit_start, digit_end = pass1[i + 2]
            # mid_token 形如 "_%" / "-%" / ".%" — 前半是分隔符，后半是 %
            if (
                len(mid_token) >= 2
                and mid_token[-1] == "%"
                and all(c in _SEPARATORS for c in mid_token[:-1])
            ):
                merged = f"{token}{mid_token}{digit_token}"
                pass2.append((merged, start, digit_end))
                i += 3
                continue

        pass2.append((token, start, end))
        i += 1

    return pass2


# ---------- 后处理：停用词过滤 ----------


def _filter_stopwords(
    tokens: list[tuple[str, int, int]],
) -> list[tuple[str, int, int]]:
    """过滤无意义 token：停用词 + 纯特殊字符 + 占位符。

    仅保留含中文、英文字母或数字的 token（文件、系统、admin、123），
    过滤无意义词（的、与、于）和纯符号/占位符（/、%1、%1/%2）。

    Args:
        tokens: 已合并占位符后的 (token, start, end) 列表。

    Returns:
        过滤后的列表。
    """
    return [
        (token, start, end)
        for token, start, end in tokens
        if (
            # 不是单字停用词
            (len(token) != 1 or token not in _STOPWORDS)
            # 且包含有意义的字符（中文/字母/数字）
            and _HAS_CONTENT.search(token)
        )
    ]


# ---------- 公共 API ----------


def _ensure_jieba() -> None:
    global _initialized
    if not _initialized:
        jieba.initialize()
        _initialized = True


def segment_source_text(text: str) -> list[tuple[str, int, int]]:
    """对 text 做 jieba 通用分词，返回 (token, start, end) 列表。

    切界仅依赖 jieba 默认词频，不 load_userdict(term_word)。

    Args:
        text: 待切分原文。

    Returns:
        非空白 token 的 (词, start, end) 列表；end 为 exclusive。
    """
    raw = (text or "").strip()
    if not raw:
        return []
    _ensure_jieba()
    tokens = [
        (token, start, end)
        for token, start, end in jieba.tokenize(raw, mode="default")
        if token.strip()
    ]
    return _filter_stopwords(_merge_placeholders(tokens))
