"""comment_rule format / prefer_abbr 单测。"""

from app.shared.comment_rule.format import (
    any_prefer_abbr,
    apply_prefer_abbr_to_spans,
    build_comment_rules_section_markdown,
    infer_prefer_abbr,
    parse_comment_keys,
)
from app.graph.pre_translate.prompts.compose_suggest import (
    build_compose_suggest_user_message,
)


def test_parse_comment_keys_split_and_dedupe():
    assert parse_comment_keys("tabBarTitle, menuName，tabBarTitle") == [
        "tabBarTitle",
        "menuName",
    ]


def test_infer_prefer_abbr_from_tips():
    assert infer_prefer_abbr("", "- 精简翻译") is True
    assert infer_prefer_abbr("", "可以使用通用的缩写") is True
    assert infer_prefer_abbr("告警", "用 status") is False


def test_apply_prefer_abbr_replaces_translate():
    spans = [
        {"text": "系统", "translate": "System", "abbr": "Sys", "ambiguous": False},
        {"text": "文件", "translate": "File", "abbr": "", "ambiguous": False},
    ]
    out = apply_prefer_abbr_to_spans(spans, prefer_abbr=True)
    assert out[0]["translate"] == "Sys"
    assert out[0]["_forced_abbr"] is True
    assert out[1]["translate"] == "File"


def test_compose_user_message_forces_abbr_and_injects_rules():
    msg = build_compose_suggest_user_message(
        source_text="系统配置",
        target_lang="英文",
        coverage=1.0,
        spans=[
            {
                "text": "系统",
                "translate": "System",
                "abbr": "Sys",
                "ambiguous": False,
            },
        ],
        decomposed_translation="System",
        comment_rules=[
            {
                "comment_key": "tabBarTitle",
                "scene": "tab标题",
                "rule_text": "精简翻译",
                "prefer_abbr": True,
            }
        ],
        prefer_abbr=True,
    )
    assert "| 系统 | Sys |" in msg
    assert "优先缩写" in msg
    assert "tabBarTitle" in msg
    assert "comment 场景规则" in msg


def test_build_comment_rules_section():
    md = build_comment_rules_section_markdown(
        [{"comment_key": "UI", "scene": "界面", "rule_text": "Yes/No", "prefer_abbr": False}],
        unmatched_keys=["UnknownKey"],
    )
    assert "UI" in md
    assert "UnknownKey" in md
    assert any_prefer_abbr([{"prefer_abbr": True}]) is True
