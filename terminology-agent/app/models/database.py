"""SQLAlchemy engine and declarative base."""

from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase

from config.settings import settings

# Build async URL (replace pymysql with aiomysql for async)
async_db_url = settings.database_url.replace("pymysql", "aiomysql")
engine = create_async_engine(async_db_url, pool_pre_ping=True, echo=False)

AsyncSessionLocal = async_sessionmaker(engine, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


async def get_session():
    async with AsyncSessionLocal() as session:
        yield session
