"""term_word lexeme lookup — 为拆解 span 批量查译法。"""

from __future__ import annotations

from typing import Any

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
    """为每个 span 查 term_word 并写入 translate / ambiguous。

    Args:
        word_repo: WordRepository 实例。
        spans: decompose_to_spans 输出。
        target_lang: 目标语种。
        comment: Grep 消歧 comment。
        department: 部门过滤。

    Returns:
        原地 enriched 后的 Span 列表（新对象）。
    """
    enriched: list[Span] = []
    cache: dict[str, tuple[str | None, bool]] = {}

    for span in spans:
        word = span.text
        if word not in cache:
            rows = await word_repo.find_by_word(
                word,
                target_lang=target_lang,
                comment=comment,
                department=department,
            )
            cache[word] = _pick_translate(rows)

        translate, ambiguous = cache[word]
        enriched.append(
            Span(
                text=span.text,
                start=span.start,
                end=span.end,
                translate=translate,
                ambiguous=ambiguous,
            )
        )
    return enriched
