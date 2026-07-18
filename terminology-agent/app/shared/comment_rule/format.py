"""comment_rule 解析 / markdown 格式化 / prefer_abbr 作用于 spans。

对齐 translate skill：parseCommentKeys / formatCommentRuleToMarkdown。
"""

from __future__ import annotations

import re
from typing import Any, Mapping, Sequence


_COMMENT_KEY_SPLIT = re.compile(r"[;,，\n]+")


def parse_comment_keys(comment_value: str | None) -> list[str]:
    """从词条 comment 字段提取 key 列表（去重保序）。"""
    raw = str(comment_value or "").strip()
    if not raw:
        return []
    keys = [s.strip() for s in _COMMENT_KEY_SPLIT.split(raw) if s.strip()]
    seen: set[str] = set()
    out: list[str] = []
    for k in keys:
        if k in seen:
            continue
        seen.add(k)
        out.append(k)
    return out


def infer_case_type(scene: str | None, rule_text: str | None) -> str | None:
    """从场景/规则文案启发 SentenceCase / TitleCase。"""
    merged = f"{scene or ''}\n{rule_text or ''}".lower()
    has_sentence = (
        "sentence case" in merged
        or "第一个单词首字母大写" in merged
        or "首个单词首字母大写" in merged
    )
    has_title = (
        "title case" in merged
        or "每个单词首字母都要大写" in merged
        or "每个单词首字母大写" in merged
    )
    if has_sentence and not has_title:
        return "SentenceCase"
    if has_title and not has_sentence:
        return "TitleCase"
    return None


def infer_prefer_abbr(scene: str | None, rule_text: str | None) -> bool:
    """导入启发：精简翻译 / 明确允许缩写 → prefer_abbr。"""
    text = f"{scene or ''}\n{rule_text or ''}"
    return "精简翻译" in text or "可以使用通用的缩写" in text


def format_comment_rule_to_markdown(rule: Mapping[str, Any]) -> str:
    """单条规则 → markdown（喂 LLM）。"""
    parts: list[str] = [f"- **comment**: `{rule.get('comment_key') or rule.get('comment') or ''}`"]
    source = (rule.get("entry_source") or rule.get("source") or "").strip()
    if source:
        parts.append(f"  - **词条来源**: {source}")
    scene = (rule.get("scene") or "").strip()
    if scene:
        parts.append("  - **场景**:")
        for line in scene.splitlines():
            line = line.strip()
            if line:
                parts.append(f"    - {line}")
    tips = (rule.get("rule_text") or rule.get("tips") or "").strip()
    if tips:
        parts.append("  - **规则**:")
        for line in tips.splitlines():
            line = line.strip()
            if line:
                parts.append(f"    - {line}")
    if rule.get("prefer_abbr"):
        parts.append("  - **优先缩写**: 是（有缩写的词片须用缩写，禁止完整译法）")
    return "\n".join(parts)


def build_comment_rules_section_markdown(
    rules: Sequence[Mapping[str, Any]],
    *,
    unmatched_keys: Sequence[str] | None = None,
) -> str:
    """多条规则 → 整段 markdown；无内容则空串。"""
    blocks: list[str] = []
    for rule in rules:
        block = format_comment_rule_to_markdown(rule)
        if block:
            blocks.append(block)
    for key in unmatched_keys or []:
        blocks.append(
            f"- **comment**: `{key}`（未在 comment_rule 表中找到对应条目）"
        )
    if not blocks:
        return ""
    return "## comment 场景规则\n\n" + "\n\n".join(blocks) + "\n"


def any_prefer_abbr(rules: Sequence[Mapping[str, Any]]) -> bool:
    """任一规则 prefer_abbr=True。"""
    return any(bool(r.get("prefer_abbr")) for r in rules)


def apply_prefer_abbr_to_spans(
    spans: list[dict],
    *,
    prefer_abbr: bool,
) -> list[dict]:
    """prefer_abbr 时：有 abbr 的 hit span 将 translate 替换为 abbr（副本）。"""
    if not prefer_abbr:
        return [dict(s) for s in spans]
    out: list[dict] = []
    for raw in spans:
        d = dict(raw)
        abbr = (d.get("abbr") or "").strip()
        if (
            abbr
            and d.get("translate")
            and not bool(d.get("ambiguous"))
        ):
            d["translate"] = abbr
            d["_forced_abbr"] = True
        out.append(d)
    return out


def rule_row_to_dict(row: Any) -> dict[str, Any]:
    """ORM / SimpleNamespace → dict。"""
    return {
        "id": getattr(row, "id", None),
        "comment_key": getattr(row, "comment_key", None),
        "entry_source": getattr(row, "entry_source", None),
        "scene": getattr(row, "scene", None),
        "rule_text": getattr(row, "rule_text", None),
        "prefer_abbr": bool(getattr(row, "prefer_abbr", False)),
        "case_type": getattr(row, "case_type", None),
        "related_id": getattr(row, "related_id", None),
        "created_at": (
            row.created_at.isoformat(sep=" ", timespec="seconds")
            if getattr(row, "created_at", None)
            else None
        ),
        "updated_at": (
            row.updated_at.isoformat(sep=" ", timespec="seconds")
            if getattr(row, "updated_at", None)
            else None
        ),
    }
