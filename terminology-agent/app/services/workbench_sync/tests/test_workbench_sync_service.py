"""WorkbenchEntrySyncService 行为契约测试 — Mock EntryRepository。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest

from app.core.exceptions import ApiError
from app.services.workbench_sync.service import WorkbenchEntrySyncService


@pytest.fixture
def mock_entry_repo():
    repo = AsyncMock()
    repo.get_entry_info = AsyncMock()
    repo.get_translate = AsyncMock()
    repo.insert_workbench_translate = AsyncMock()
    repo.update_workbench_translate = AsyncMock()
    repo.set_entry_trans_id = AsyncMock()
    repo.commit = AsyncMock()
    return repo


@pytest.fixture
def sync_service(mock_entry_repo):
    session = AsyncMock()
    service = WorkbenchEntrySyncService(session)
    service._repo = mock_entry_repo
    return service


@pytest.mark.service
async def test_sync_updates_existing_translate(sync_service, mock_entry_repo):
    """已有 ru_trans_id 时应 update translate 且 state=1。"""
    entry_info = SimpleNamespace(
        id="entry-001",
        entry="admin",
        ru_trans_id="trans-001",
    )
    existing = SimpleNamespace(
        id="trans-001",
        translate_state="0",
    )
    mock_entry_repo.get_entry_info.return_value = entry_info
    mock_entry_repo.get_translate.return_value = existing

    await sync_service.sync_translation_to_pending_audit(
        entry_info_id="entry-001",
        target_lang="俄文",
        translate="админ",
        audit_suggest="Agent 建议",
        department="通用平台部",
    )

    mock_entry_repo.update_workbench_translate.assert_awaited_once()
    kwargs = mock_entry_repo.update_workbench_translate.await_args.kwargs
    assert kwargs["translate"] == "админ"
    assert kwargs["translate_state"] == "1"
    mock_entry_repo.commit.assert_awaited_once()


@pytest.mark.service
async def test_sync_inserts_when_no_trans_id(sync_service, mock_entry_repo):
    """无 trans_id 时应 insert 并更新 entry_info 外键。"""
    entry_info = SimpleNamespace(
        id="entry-001",
        entry="admin",
        ru_trans_id=None,
    )
    created = SimpleNamespace(id="trans-new")
    mock_entry_repo.get_entry_info.return_value = entry_info
    mock_entry_repo.insert_workbench_translate.return_value = created

    await sync_service.sync_translation_to_pending_audit(
        entry_info_id="entry-001",
        target_lang="俄文",
        translate="админ",
    )

    mock_entry_repo.insert_workbench_translate.assert_awaited_once()
    mock_entry_repo.set_entry_trans_id.assert_awaited_once_with(
        entry_info, "ru_trans_id", "trans-new"
    )
    mock_entry_repo.commit.assert_awaited_once()


@pytest.mark.service
async def test_sync_does_not_downgrade_finalized_state(sync_service, mock_entry_repo):
    """translate_state=3 时不降级为 1。"""
    entry_info = SimpleNamespace(
        id="entry-001",
        entry="admin",
        ru_trans_id="trans-001",
    )
    existing = SimpleNamespace(id="trans-001", translate_state="3")
    mock_entry_repo.get_entry_info.return_value = entry_info
    mock_entry_repo.get_translate.return_value = existing

    await sync_service.sync_translation_to_pending_audit(
        entry_info_id="entry-001",
        target_lang="俄文",
        translate="новый перевод",
    )

    kwargs = mock_entry_repo.update_workbench_translate.await_args.kwargs
    assert kwargs["translate_state"] == "3"


@pytest.mark.service
async def test_sync_missing_entry_info_raises(sync_service, mock_entry_repo):
    """entry_info 不存在时应抛 ApiError。"""
    mock_entry_repo.get_entry_info.return_value = None

    with pytest.raises(ApiError, match="不存在"):
        await sync_service.sync_translation_to_pending_audit(
            entry_info_id="missing",
            target_lang="俄文",
            translate="test",
        )


@pytest.mark.service
async def test_sync_missing_entry_info_id_raises(sync_service):
    """缺少 entry_info_id 时应抛 ApiError。"""
    with pytest.raises(ApiError, match="entry_info_id"):
        await sync_service.sync_translation_to_pending_audit(
            entry_info_id=None,
            target_lang="俄文",
            translate="test",
        )
