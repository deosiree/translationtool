"""FastAPI 路由聚合 — 术语学习 Agent HTTP 接口。

路由前缀：/agent（见 app/main.py）
前端 dev proxy：vue.config.js → localhost:18002

主要端点：
  GET  /health                      健康检查
  POST /pre-translate/batch         工作台 Agent 批量预翻译
  GET  /term-learning/list          术语学习待审核列表
  GET  /term-learning/{id}          审核详情
  POST /term-learning/{id}/review   人工确认 / 拒绝（approved 时 MergeToStore）
  POST /term-learning/batch/review  批量确认 / 拒绝
"""

from fastapi import APIRouter

from app.api import health, pre_translate, term_learning, word

router = APIRouter()
router.include_router(health.router, tags=["健康检查"])
router.include_router(pre_translate.router, prefix="/pre-translate", tags=["预翻译"])
router.include_router(term_learning.router, prefix="/term-learning", tags=["术语审核"])
router.include_router(word.router, prefix="/word", tags=["术语词片"])
