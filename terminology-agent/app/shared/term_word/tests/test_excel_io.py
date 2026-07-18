"""Excel 模板 / 导入解析单测。"""

from app.shared.term_word.excel_io import (
    build_template_bytes,
    parse_import_rows,
    parse_use_llm,
)


def test_parse_use_llm_variants():
    assert parse_use_llm(True) is True
    assert parse_use_llm("是") is True
    assert parse_use_llm("FALSE") is False
    assert parse_use_llm(None) is False


def test_template_roundtrip_sample():
    data = build_template_bytes(with_sample=True)
    rows, errors = parse_import_rows(data)
    assert errors == []
    assert len(rows) == 5
    by_word = {r["word"]: r for r in rows}
    assert by_word["获取/检索"]["use_llm"] is True
    assert by_word["画面"]["translate"] == "graph"
    assert by_word["画面"]["status"] == "3"
    assert by_word["图元"]["category"] == "图形"
