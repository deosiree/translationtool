"""拆解覆盖率与置信度映射 — Phase 3b 确定性评分。"""

from __future__ import annotations

import re

from app.graph.pre_translate.constants import COVERAGE_FLOOR
from app.graph.pre_translate.utils.decompose import Span

_PUNCT_ONLY = re.compile(r"^[\W_]+$", re.UNICODE)


def _is_coverage_eligible(span: Span) -> bool:
    """参与覆盖率分母的 span：中文词片等可 lookup 片段，排除标点与 ASCII 前缀。"""
    token = (span.text or "").strip()
    if not token:
        return False
    if _PUNCT_ONLY.match(token):
        return False
    if token.isascii():
        return False
    if len(token) == 1:
        return False
    return True


def compute_coverage(spans: list[Span], source_text: str) -> float:
    """已译 eligible span 字符数 / eligible span 总字符数。

    Args:
        spans: 拆解并 lookup 后的 spans。
        source_text: 原始词条（与拆解输入一致）。

    Returns:
        0~1 覆盖率，保留三位小数。
    """
    if not (source_text or "").strip():
        return 0.0
    eligible_len = sum(len(span.text) for span in spans if _is_coverage_eligible(span))
    if eligible_len == 0:
        return 0.0
    covered = sum(
        len(span.text)
        for span in spans
        if span.translate and not span.ambiguous and _is_coverage_eligible(span)
    )
    return round(covered / eligible_len, 3)


def coverage_to_confidence(coverage: float) -> float:
    """coverage 映射到 auto_approve 用置信度（0.7~0.95）。

    Args:
        coverage: compute_coverage 结果。

    Returns:
        置信度；未达 COVERAGE_FLOOR 时按比例压低。
    """
    if coverage >= COVERAGE_FLOOR:
        return round(min(0.7 + coverage * 0.25, 0.95), 3)
    return round(max(coverage * 0.7, 0.0), 3)


def meets_coverage_floor(coverage: float) -> bool:
    """coverage 是否达到自动拼装阈值。"""
    return coverage >= COVERAGE_FLOOR
