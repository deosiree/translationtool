"""工作台词条上下文 — entry_comment 消歧（Grep 线 term_word 查表用）。"""

from __future__ import annotations


def resolve_entry_comment(
    *,
    entry_comment: str | None = None,
    entry_info_comment: str | None = None,
) -> str:
    """合并工作台 comment 来源为 Grep 消歧键第三维。

    ``comment`` 仅来自 ``t_entry_info.comment`` 或 batch 传入的 ``entry.comment``，
    不使用 ``t_translate.remark``。

    Args:
        entry_comment: 工作台词条 dict 的 ``comment`` 字段（优先）。
        entry_info_comment: ``t_entry_info.comment`` 回退值。

    Returns:
        strip 后的 comment；皆空则 ``''``。
    """
    raw = entry_comment if entry_comment is not None else entry_info_comment
    return (raw or "").strip()
