"""Settings 配置契约 — .env 为唯一运行时真相源。"""

import inspect

import pytest
from pydantic_core import PydanticUndefined

from config.settings import Settings


@pytest.mark.unit
def test_settings_fields_have_no_python_defaults():
    """业务配置不得写死在 Python 中，必填项由 .env 提供。"""
    for name, field in Settings.model_fields.items():
        assert field.default is PydanticUndefined, (
            f"{name} 不应有 Python 默认值，请在 .env 中配置"
        )


@pytest.mark.unit
def test_settings_module_docstring_documents_env_only():
    """模块 docstring 应说明 .env 为唯一运行时来源。"""
    doc = inspect.getmodule(Settings).__doc__ or ""
    assert ".env" in doc
    assert ".env.example" in doc


@pytest.mark.unit
def test_settings_loads_llm_model_from_env(monkeypatch):
    """Settings 实例应反映 .env / 环境变量中的 LLM_MODEL。"""
    from config import settings as settings_module

    monkeypatch.setenv("LLM_MODEL", "deepseek-v4-flash-test")
    reloaded = settings_module.Settings()
    assert reloaded.llm_model == "deepseek-v4-flash-test"
