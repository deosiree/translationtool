"""build_word_index 集成测 — mock DB，验证 join 与冲突检测。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.word.conflict import detect_translate_mismatches
from app.word.join_entry_info import build_term_word_payload, index_entry_infos_by_trans_id


@pytest.mark.unit
def test_build_payloads_skip_translate_without_entry_info():
    tr = SimpleNamespace(
        id="orphan",
        entry="孤立词条",
        translate="orphan tr",
        type="俄文",
        translate_state="3",
        visual_range=None,
    )
    index = index_entry_infos_by_trans_id([])
    linked = index.get((str(tr.id), "俄文"), [])
    assert linked == []
    assert build_term_word_payload(translate=tr, entry_info=SimpleNamespace(id="x")) is not None


@pytest.mark.unit
def test_conflict_detection_with_task_product_provenance():
    tr1 = SimpleNamespace(
        id="t1", entry="词", translate="A", type="俄文", translate_state="3", visual_range="D1"
    )
    tr2 = SimpleNamespace(
        id="t2", entry="词", translate="B", type="俄文", translate_state="3", visual_range="D2"
    )
    ei1 = SimpleNamespace(id="e1", comment="", task_id="task-a", product_id="prod-x", is_delete=0, ru_trans_id="t1")
    ei2 = SimpleNamespace(id="e2", comment="", task_id="task-b", product_id="prod-y", is_delete=0, ru_trans_id="t2")

    p1 = build_term_word_payload(translate=tr1, entry_info=ei1)
    p2 = build_term_word_payload(translate=tr2, entry_info=ei2)
    assert p1["comment"] == ""
    conflicts = detect_translate_mismatches([p1, p2])
    assert len(conflicts) == 1
    payload = conflicts[0].to_conflict_payload()
    assert payload["task_ids"] == ["task-a", "task-b"]
    assert payload["product_ids"] == ["prod-x", "prod-y"]


@pytest.mark.unit
async def test_build_word_index_dry_run_stats():
    from devtools.build_word_index import build_word_index

    tr = SimpleNamespace(
        id="t1",
        entry="测试",
        translate="test",
        type="俄文",
        translate_state="3",
        visual_range=None,
        delete_state=0,
    )
    ei = SimpleNamespace(
        id="e1",
        comment="c1",
        task_id="task-1",
        product_id="prod-1",
        is_delete=0,
        ru_trans_id="t1",
    )

    mock_session = AsyncMock()
    mock_trans_result = MagicMock()
    mock_trans_result.scalars.return_value.all.return_value = [tr]
    mock_ei_result = MagicMock()
    mock_ei_result.scalars.return_value.all.return_value = [ei]
    mock_session.execute = AsyncMock(side_effect=[mock_trans_result, mock_ei_result])

    mock_ctx = AsyncMock()
    mock_ctx.__aenter__ = AsyncMock(return_value=mock_session)
    mock_ctx.__aexit__ = AsyncMock(return_value=None)

    with patch("devtools.build_word_index.AsyncSessionLocal", return_value=mock_ctx):
        stats = await build_word_index(dry_run=True)

    assert stats["words_to_write"] == 1
    assert stats["dry_run"] is True
    assert stats["conflicts"] == 0
