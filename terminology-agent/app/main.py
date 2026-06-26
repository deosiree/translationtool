"""FastAPI 应用入口。"""

from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.router import router
from app.core.exceptions import register_exception_handlers
from app.models.database import engine, Base


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：启动时建表，关闭时释放连接池。"""
    # 启动：若表不存在则创建（不覆盖已有表）
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    # 关闭：释放数据库连接池
    await engine.dispose()


app = FastAPI(
    title="术语学习 Agent",
    description="基于 LangGraph 的 i18n 术语发现与学习服务",
    version="0.1.0",
    lifespan=lifespan,
)

app.include_router(router, prefix="/agent")
register_exception_handlers(app)


@app.get("/", summary="服务信息", tags=["服务信息"])
async def root():
    """根路径，返回服务名与文档链接。"""
    return {
        "service": "术语学习 Agent",
        "version": "0.1.0",
        "docs": "/docs",
    }
