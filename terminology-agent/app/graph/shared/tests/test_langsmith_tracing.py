"""LangSmith 域项目名 helper 单元测试。"""

from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from app.graph.shared import langsmith_tracing


@pytest.mark.unit
def test_project_for_domain_default(monkeypatch):
    """实跑项目名：translationtool-agent[pre_translate]。"""
    monkeypatch.setattr(
        langsmith_tracing.settings,
        "langsmith_project",
        "translationtool-agent",
    )
    assert (
        langsmith_tracing.project_for_domain(
            langsmith_tracing.PRE_TRANSLATE_DOMAIN,
            test=False,
        )
        == "translationtool-agent[pre_translate]"
    )


@pytest.mark.unit
def test_project_for_domain_test_suffix(monkeypatch):
    """pytest 项目名：translationtool-agent[pre_translate](test)。"""
    monkeypatch.setattr(
        langsmith_tracing.settings,
        "langsmith_project",
        "translationtool-agent",
    )
    assert (
        langsmith_tracing.project_for_domain(
            langsmith_tracing.PRE_TRANSLATE_DOMAIN,
            test=True,
        )
        == "translationtool-agent[pre_translate](test)"
    )


@pytest.mark.unit
def test_domain_run_context_disabled(monkeypatch):
    """tracing 关闭时不调用 Client、不抛错。"""
    monkeypatch.setattr(langsmith_tracing.settings, "langsmith_tracing", False)
    monkeypatch.setattr(langsmith_tracing.settings, "langsmith_api_key", None)

    with patch("app.graph.shared.langsmith_tracing.Client") as client_cls:
        with langsmith_tracing.domain_run_context(langsmith_tracing.PRE_TRANSLATE_DOMAIN):
            pass
        client_cls.assert_not_called()


@pytest.mark.unit
def test_domain_run_context_missing_api_key(monkeypatch):
    """tracing 开但无 API key 时仍为 no-op。"""
    monkeypatch.setattr(langsmith_tracing.settings, "langsmith_tracing", True)
    monkeypatch.setattr(langsmith_tracing.settings, "langsmith_api_key", None)

    with patch("app.graph.shared.langsmith_tracing.Client") as client_cls:
        with langsmith_tracing.domain_run_context(langsmith_tracing.PRE_TRANSLATE_DOMAIN):
            pass
        client_cls.assert_not_called()


@pytest.mark.unit
def test_domain_run_context_enabled(monkeypatch):
    """tracing 开启时 conftest autouse 使 project 带 (test) 后缀。"""
    monkeypatch.setattr(langsmith_tracing.settings, "langsmith_tracing", True)
    monkeypatch.setattr(langsmith_tracing.settings, "langsmith_api_key", "test-key")
    monkeypatch.setattr(
        langsmith_tracing.settings,
        "langsmith_project",
        "translationtool-agent",
    )

    mock_client = MagicMock()
    with patch(
        "app.graph.shared.langsmith_tracing.Client",
        return_value=mock_client,
    ) as client_cls, patch(
        "app.graph.shared.langsmith_tracing.tracing_context",
    ) as tracing_ctx:
        tracing_ctx.return_value.__enter__ = MagicMock(return_value=None)
        tracing_ctx.return_value.__exit__ = MagicMock(return_value=False)

        with langsmith_tracing.domain_run_context(langsmith_tracing.PRE_TRANSLATE_DOMAIN):
            pass

        client_cls.assert_called_once_with(api_key="test-key")
        tracing_ctx.assert_called_once_with(
            enabled=True,
            client=mock_client,
            project_name="translationtool-agent[pre_translate](test)",
        )
