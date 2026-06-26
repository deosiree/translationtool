"""SQLAlchemy 异步引擎与 ORM 基类。"""

from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase

from config.settings import settings

# 异步驱动：pymysql 同步 URL 替换为 aiomysql
async_db_url = settings.database_url.replace("pymysql", "aiomysql")
engine = create_async_engine(async_db_url, pool_pre_ping=True, echo=False)

AsyncSessionLocal = async_sessionmaker(engine, expire_on_commit=False)


class Base(DeclarativeBase):
    """所有 ORM 模型的声明式基类。"""
    pass


async def get_session():
    """FastAPI 依赖注入：每个请求提供一个异步数据库会话。"""
    async with AsyncSessionLocal() as session:
        yield session
