"""健康检查端点。"""

from fastapi import APIRouter

from app.core.response import ok
from app.schemas.agent import HealthData

router = APIRouter()


@router.get("/health", summary="健康检查")
async def health():
    """存活探针，Docker / 本地开发健康检查。"""
    return ok(HealthData())
