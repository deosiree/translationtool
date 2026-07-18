"""词典拆分预览 — jieba + 停用词过滤 + 译法对齐。"""

from __future__ import annotations

from typing import Any, Iterable

import jieba

from app.shared.term_word.extract import extract_words
from app.shared.term_word.segment import _ensure_jieba
from app.shared.term_word.stopwords import (
    en_stopword_drop_count,
    filter_cn_tokens,
)

# 仅用于切界提权，不提供译法
SPLIT_SEGMENT_SEEDS: tuple[str, ...] = (
    "字段",
    "长度",
    "获取",
    "失败",
    "文件",
    "系统",
    "用户",
    "管理",
    "配置",
    "保存",
    "成功",
    "全部",
    "对象",
    "属性",
    "删除",
    "同步",
    "错误",
    "环形",
    "图例",
    "名称",
    "限制",
    "字符",
    "检查",
)

# 默认词典里会抢走正确切界的错误整词
SPLIT_BAD_COMPOUNDS: tuple[str, ...] = (
    "段长度",
)


def boost_split_jieba_words(extra_words: Iterable[str] | None = None) -> None:
    """为拆分路径提升 jieba 词频（lexicon 词 + 切界种子），并去掉错误整词。"""
    _ensure_jieba()
    seen: set[str] = set()
    for w in (*SPLIT_SEGMENT_SEEDS, *(extra_words or ())):
        token = (w or "").strip()
        if len(token) < 2 or token in seen:
            continue
        seen.add(token)
        jieba.add_word(token)
        jieba.suggest_freq(token, True)
    for bad in SPLIT_BAD_COMPOUNDS:
        jieba.del_word(bad)


def _can_positional_zip(cn_raw: list[str], cn_tokens: list[str], translate: str) -> bool:
    """过滤后长度相等且两侧丢掉的停用词个数相等时才允许位置 zip。"""
    if not cn_tokens:
        return False
    en_tokens, en_dropped = en_stopword_drop_count(translate)
    cn_dropped = len(cn_raw) - len(cn_tokens)
    return len(cn_tokens) == len(en_tokens) and cn_dropped == en_dropped


def split_entry_to_candidates(
    *,
    entry: str,
    translate: str,
    target_lang: str,
    department: str | None = None,
    comment: str = "",
    lexicon: dict[str, str] | None = None,
    keep_untranslated: bool = False,
) -> list[dict[str, Any]]:
    """将一条术语词典词条切分为可导出的词片候选。

    规则：
    - 过滤中文无意义词（如「与」）；
    - 译法：lexicon 命中 > 对称停用词丢弃下的 zip 对齐 > 整词条相等用整句译；
    - 默认丢弃无译法词片；keep_untranslated=True 时保留空译供 LLM 补；
    - 无任何词片译且非 keep_untranslated 时，回退整词条一行；
    - use_llm 恒为 False（与是否 LLM 补译无关）。

    Args:
        entry: 词条原文。
        translate: 词条译文。
        target_lang: 翻译类型。
        department: 可见范围。
        comment: 消歧 comment。
        lexicon: word → translate 已有字典（term_word 查询结果）。
        keep_untranslated: 是否保留尚无译法的词片。

    Returns:
        标准行 dict 列表。
    """
    lexicon = lexicon or {}
    boost_split_jieba_words(lexicon.keys())

    raw_tokens = extract_words(entry)
    # 仅统计非空原始 token；停用词过滤后再比丢弃数
    cn_raw = [t for t in raw_tokens if (t or "").strip()]
    cn_tokens = filter_cn_tokens(cn_raw)
    if not cn_tokens:
        return []

    en_tokens, _en_dropped = en_stopword_drop_count(translate)
    zip_map: dict[str, str] = {}
    if _can_positional_zip(cn_raw, cn_tokens, translate):
        zip_map = dict(zip(cn_tokens, en_tokens, strict=True))

    entry_stripped = (entry or "").strip()
    translate_stripped = (translate or "").strip()
    out: list[dict[str, Any]] = []
    seen: set[str] = set()

    for token in cn_tokens:
        if token in seen:
            continue
        seen.add(token)
        hit = (lexicon.get(token) or "").strip()
        aligned = (zip_map.get(token) or "").strip()
        whole = translate_stripped if token == entry_stripped else ""
        final_tr = hit or aligned or whole
        if not final_tr and not keep_untranslated:
            continue
        out.append(
            {
                "word": token,
                "translate": final_tr,
                "target_lang": target_lang,
                "department": department,
                "comment": comment or "",
                "category": None,
                "abbr": None,
                "use_llm": False,
                "usage_notes": None,
                "status": "1",
                "source_entry": entry_stripped,
            }
        )

    if (
        not out
        and not keep_untranslated
        and entry_stripped
        and translate_stripped
    ):
        out.append(
            {
                "word": entry_stripped,
                "translate": translate_stripped,
                "target_lang": target_lang,
                "department": department,
                "comment": comment or "",
                "category": None,
                "abbr": None,
                "use_llm": False,
                "usage_notes": None,
                "status": "1",
                "source_entry": entry_stripped,
            }
        )
    return out


def split_items_preview(
    items: list[dict[str, Any]],
    *,
    lexicon: dict[str, str] | None = None,
    keep_untranslated: bool = False,
) -> list[dict[str, Any]]:
    """批量拆分预览；按词片去重（保留首次）。"""
    lexicon = lexicon or {}
    boost_split_jieba_words(lexicon.keys())
    merged: list[dict[str, Any]] = []
    seen_keys: set[tuple[str, str, str]] = set()
    for item in items:
        rows = split_entry_to_candidates(
            entry=str(item.get("entry") or item.get("word") or ""),
            translate=str(item.get("translate") or ""),
            target_lang=str(item.get("target_lang") or item.get("type") or ""),
            department=item.get("department") or item.get("visual_range"),
            comment=str(item.get("comment") or ""),
            lexicon=lexicon,
            keep_untranslated=keep_untranslated,
        )
        for row in rows:
            key = (row["word"], row.get("comment") or "", row["target_lang"])
            if key in seen_keys:
                continue
            seen_keys.add(key)
            merged.append(row)
    return merged
