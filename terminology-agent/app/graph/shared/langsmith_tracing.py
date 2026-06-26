"""LangSmith 域项目名 — 全程读 Settings，不写 os.environ。"""

from __future__ import annotations

from contextlib import contextmanager
from typing import Iterator

from langsmith import Client
from langsmith.run_helpers import tracing_context

from config.settings import settings

PRE_TRANSLATE_DOMAIN = "pre_translate"

_TRACING_TEST_MODE: bool = False


def project_for_domain(domain: str, *, test: bool | None = None) -> str:
    """返回 LangSmith 项目名：{base}[{domain}]，pytest 时追加 (test)。"""
    is_test = _TRACING_TEST_MODE if test is None else test
    name = f"{settings.langsmith_project}[{domain}]"
    return f"{name}(test)" if is_test else name


@contextmanager
def domain_run_context(domain: str) -> Iterator[None]:
    """在域项目名下开启 tracing；未配置时 no-op。"""
    if not settings.langsmith_tracing or not settings.langsmith_api_key:
        yield
        return

    client = Client(api_key=settings.langsmith_api_key)
    with tracing_context(
        enabled=True,
        client=client,
        project_name=project_for_domain(domain),
    ):
        yield
