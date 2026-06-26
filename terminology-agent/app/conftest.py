"""共享 pytest fixtures — app 下所有 tests/ 子目录自动继承。"""

from __future__ import annotations

# 测试与运行时共用同一 `.env` 真相源；缺失时 fail fast，不读取 .env.example
from pathlib import Path

from dotenv import load_dotenv

_ENV_PATH = Path(__file__).resolve().parents[1] / ".env"
if not _ENV_PATH.is_file():
    raise FileNotFoundError(
        "测试需要 terminology-agent/.env。"
        "请先执行：copy .env.example .env（或仓库根目录 setup 脚本）"
    )
load_dotenv(_ENV_PATH)

import json
from datetime import datetime
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock

import pytest
from httpx import ASGITransport, AsyncClient

FIXTURES_DIR = Path(__file__).parent / "testing" / "fixtures"


@pytest.fixture(autouse=True)
def _langsmith_test_project(monkeypatch):
    """pytest 触发的 trace 上报至 translationtool-agent[domain](test)。"""
    monkeypatch.setattr(
        "app.graph.shared.langsmith_tracing._TRACING_TEST_MODE",
        True,
    )


@pytest.fixture
def sample_entry_oid() -> str:
    """含 %1/%2 占位符的 OID 样例词条。"""
    return "正在查询第 %1/%2 个路径的OID..."


@pytest.fixture
def sample_entries() -> list[dict]:
    """从 fixtures/entries.json 加载的多条样例词条。"""
    with open(FIXTURES_DIR / "entries.json", encoding="utf-8") as f:
        return json.load(f)


@pytest.fixture
def mock_translate_entry():
    """模拟 t_translate 一行（精确匹配场景）。"""
    return SimpleNamespace(
        id="trans-001",
        entry="正在查询第 %1/%2 个路径的OID...",
        translate="Запрос OID пути %1/%2...",
        type="俄文",
        visual_range="通用平台部",
    )


@pytest.fixture
def mock_fuzzy_entries():
    """模拟模糊匹配候选（相似度中等）。"""
    return [
        SimpleNamespace(
            entry="正在查询路径 OID",
            translate="Запрос OID пути",
        ),
    ]


@pytest.fixture
def mock_repo(mock_translate_entry, mock_fuzzy_entries):
    """AsyncMock TermRepository — 默认无精确命中、无模糊命中。"""
    repo = AsyncMock()
    repo.find_exact = AsyncMock(return_value=None)
    repo.find_fuzzy = AsyncMock(return_value=[])
    repo.create_pretranslate_audit = AsyncMock(
        return_value=SimpleNamespace(id="audit-001")
    )
    repo.insert_translate = AsyncMock()
    repo.list_pending_audits = AsyncMock(return_value=([], 0))
    repo.get_audit = AsyncMock(return_value=None)
    repo.update_audit = AsyncMock()
    return repo


@pytest.fixture
def pre_translate_service(mock_repo, monkeypatch):
    """注入 mock TermRepository 的 PreTranslateService。"""

    def repo_factory(_session):
        return mock_repo

    monkeypatch.setattr(
        "app.graph.pre_translate.nodes.features.io.retrieve_similar.TermRepository",
        repo_factory,
    )
    monkeypatch.setattr(
        "app.graph.pre_translate.nodes.features.io.write_result.TermRepository",
        repo_factory,
    )

    from app.services.pre_translate import PreTranslateService

    session = AsyncMock()
    return PreTranslateService(session)


@pytest.fixture
def term_audit_service(mock_repo):
    """注入 mock repo 的 TermAuditService。"""
    from app.services.term_audit import TermAuditService

    session = AsyncMock()
    service = TermAuditService(session)
    service._repo = mock_repo
    return service


@pytest.fixture
async def api_client(monkeypatch):
    """httpx AsyncClient — mock engine lifespan，不连 MySQL。"""
    from contextlib import asynccontextmanager

    from app.main import app
    from app.models.database import get_session

    @asynccontextmanager
    async def fake_begin():
        conn = AsyncMock()
        conn.run_sync = AsyncMock(return_value=None)
        yield conn

    mock_engine = MagicMock()
    mock_engine.begin = fake_begin
    mock_engine.dispose = AsyncMock()
    monkeypatch.setattr("app.main.engine", mock_engine)

    async def override_session():
        yield AsyncMock()

    app.dependency_overrides[get_session] = override_session

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

    app.dependency_overrides.clear()


@pytest.fixture
def sample_audit_record():
    """模拟 pending audit ORM 对象。"""
    now = datetime(2026, 6, 23, 10, 0, 0)
    return SimpleNamespace(
        id="audit-001",
        source_text="admin",
        context=None,
        matched_term=None,
        match_confidence=None,
        is_new_term=1,
        suggested_translation="Mock LLM перевод",
        llm_reasoning="基于LLM机翻：术语库未命中",
        review_status="pending",
        review_comment=None,
        error=None,
        entry_info_id="entry-002",
        task_id="task-001",
        task_name="Mock任务",
        product_name="Mock产品",
        target_lang="俄文",
        department="通用平台部",
        confidence=0.45,
        similar_terms=[],
        retrieval_method="none",
        source_type="workbench_agent",
        created_at=now,
        updated_at=now,
    )
