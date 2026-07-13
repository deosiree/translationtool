"""条件边：decompose_compose 之后 — 词级拼装够不够用、要不要改走 LLM。

Phase 3c 分流：coverage 达标且 trace 拼装非空 → compose_suggest LLM 受约束拼装；
未达标 → 整句 translate_suggest。
"""

from __future__ import annotations

from typing import Literal

from app.graph.pre_translate.constants import COVERAGE_FLOOR
from app.graph.pre_translate.state import PreTranslateState


def route_after_decompose_compose(
    state: PreTranslateState,
) -> Literal["compose_ok", "llm_fallback"]:
    """词级 coverage 是否足够进入 LLM 受约束拼装。

    判定字段：

    - ``coverage``：已 lookup 词片字符 / 原文长度。
    - ``decomposed_translation``：trace 级 ``"".join()`` 拼装，仅 compose_ok 时供 LLM 参考。

    下一跳：

    - ``compose_ok`` → ``compose_suggest``（词片术语表 + 目标语语法）
    - ``llm_fallback`` → ``translate_suggest``（整句机翻）

    Args:
        state: 上游 ``decompose_compose_node`` 写入 ``coverage``、``decomposed_translation``。

    Returns:
        ``compose_ok`` 或 ``llm_fallback``。
    """
    coverage = state.get("coverage") or 0.0
    decomposed = (state.get("decomposed_translation") or "").strip()
    if decomposed and coverage >= COVERAGE_FLOOR:
        return "compose_ok"
    return "llm_fallback"
