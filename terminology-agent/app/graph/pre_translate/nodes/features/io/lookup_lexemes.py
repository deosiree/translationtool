"""term_word lexeme lookup — 为拆解 span 批量查译法（含 Phase 3d n-gram 对齐）。"""

from __future__ import annotations

from typing import Any

from app.graph.pre_translate.utils.align_spans import align_spans_with_lexicon
from app.graph.pre_translate.utils.decompose import Span


def _pick_translate(rows: list[Any]) -> tuple[str | None, bool]:
    """从 find_by_word 结果选取唯一译法。"""
    if not rows:
        return None, False
    if len(rows) > 1:
        return None, True
    translate = getattr(rows[0], "translate", None) or rows[0].get("translate")
    return (str(translate).strip() if translate else None), False


async def lookup_lexeme_spans(
    word_repo,
    *,
    spans: list[Span],
    target_lang: str,
    comment: str,
    department: str | None,
) -> list[Span]:
    """jieba spans →（可选 n-gram 合并）查 term_word → 写入 translate / ambiguous。

    合并仅当合并串有唯一译法；原始 jieba 切界记入 ``jieba_parts``。

    Args:
        word_repo: WordRepository 实例。
        spans: decompose_to_spans 输出。
        target_lang: 目标语种。
        comment: Grep 消歧 comment。
        department: 部门过滤。

    Returns:
        对齐并 enriched 后的 Span 列表。
    """
    cache: dict[str, tuple[str | None, bool]] = {}

    async def ensure(word: str) -> tuple[str | None, bool]:
        if word not in cache:
            rows = await word_repo.find_by_word(
                word,
                target_lang=target_lang,
                comment=comment,
                department=department,
            )
            cache[word] = _pick_translate(rows)
        return cache[word]

    # 预取：单 token + 相邻 n-gram 候选，减少对齐阶段漏查
    from app.graph.pre_translate.constants import ALIGN_MAX_NGRAM

    candidates: list[str] = []
    for i, span in enumerate(spans):
        candidates.append(span.text)
        for n in range(2, min(ALIGN_MAX_NGRAM, len(spans) - i) + 1):
            window = spans[i : i + n]
            if all(
                window[j].start == window[j - 1].end for j in range(1, len(window))
            ):
                candidates.append("".join(s.text for s in window))
    for word in candidates:
        await ensure(word)

    return align_spans_with_lexicon(spans, lambda w: cache.get(w, (None, False)))
