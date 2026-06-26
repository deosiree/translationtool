"""PreTranslateGraph 可调常量。"""

from __future__ import annotations

# fuzzy 候选置信度 ≥ 此值且整句取自术语 → translation_source=term
FUZZY_AUTO_FLOOR: float = 0.95

# LLM 机翻默认置信度（通常低于工作台默认阈值 0.8 → needs_human）
LLM_DEFAULT_CONFIDENCE: float = 0.65
