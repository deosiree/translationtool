"""WordRepository mock 单测。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.repository.word_repo import WordRepository


@pytest.mark.unit
async def test_find_by_word_applies_department_filter():
    session = AsyncMock()
    mock_result = MagicMock()
    mock_result.scalars.return_value.all.return_value = []
    session.execute = AsyncMock(return_value=mock_result)

    repo = WordRepository(session)
    await repo.find_by_word("按钮", target_lang="俄文", department="通用平台部")

    session.execute.assert_awaited_once()
    stmt = session.execute.await_args[0][0]
    compiled = str(stmt)
    assert "term_word" in compiled.lower() or "word" in compiled.lower()


@pytest.mark.unit
async def test_insert_words_flushes_rows():
    session = MagicMock()
    session.flush = AsyncMock()
    repo = WordRepository(session)
    rows = await repo.insert_words([{"word": "X", "comment": "", "translate": "Y", "target_lang": "俄文", "source_translate_id": "t1"}])
    session.add_all.assert_called_once()
    session.flush.assert_awaited_once()
    assert len(rows) == 1
