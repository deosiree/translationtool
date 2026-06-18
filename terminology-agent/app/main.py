"""FastAPI application entry point."""

from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.router import router
from app.models.database import engine, Base


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: ensure tables exist (create only if not present)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    # Shutdown
    await engine.dispose()


app = FastAPI(
    title="Terminology Learning Agent",
    description="LangGraph-powered agent for discovering and learning new i18n terminology",
    version="0.1.0",
    lifespan=lifespan,
)

app.include_router(router, prefix="/agent")


@app.get("/")
async def root():
    return {
        "service": "Terminology Learning Agent",
        "version": "0.1.0",
        "docs": "/docs",
    }
