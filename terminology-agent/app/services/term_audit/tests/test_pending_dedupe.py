"""pending 列表软去重单测。"""

from types import SimpleNamespace

from app.services.term_audit.pending_dedupe import dedupe_pending_by_entry_key


def test_dedupe_keeps_newest_first():
    rows = [
        SimpleNamespace(
            id="a",
            source_text="T99",
            entry_comment="ADM-T99",
            target_lang="英文",
            department="通用平台部",
            suggested_translation="New A",
        ),
        SimpleNamespace(
            id="b",
            source_text="T99",
            entry_comment="ADM-T99",
            target_lang="英文",
            department="通用平台部",
            suggested_translation="New B",
        ),
        SimpleNamespace(
            id="c",
            source_text="其他",
            entry_comment="",
            target_lang="英文",
            department="通用平台部",
            suggested_translation="X",
        ),
    ]
    out = dedupe_pending_by_entry_key(rows)
    assert [r.id for r in out] == ["a", "c"]


def test_dedupe_different_comment_kept():
    rows = [
        SimpleNamespace(
            id="1",
            source_text="文件",
            entry_comment="A",
            target_lang="英文",
            department="通用平台部",
        ),
        SimpleNamespace(
            id="2",
            source_text="文件",
            entry_comment="B",
            target_lang="英文",
            department="通用平台部",
        ),
    ]
    assert [r.id for r in dedupe_pending_by_entry_key(rows)] == ["1", "2"]
