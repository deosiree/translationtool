"""工作台批量 Agent 预翻译 — 领域编排入口。"""

from __future__ import annotations

import uuid

from sqlalchemy.ext.asyncio import AsyncSession

from app.services.pre_translate.single import run_single_pre_translate


class PreTranslateService:
    """批量预翻译服务：过滤子词条、循环单条、汇总 auto_count / pending_count。"""

    def __init__(self, session: AsyncSession) -> None:
        """注入异步数据库会话。

        Args:
            session: FastAPI Depends(get_session) 提供的 AsyncSession。
        """
        self._session = session

    async def run_batch(
        self,
        *,
        entries: list[dict],
        task_id: str | None,
        task_name: str | None,
        product_name: str | None,
        target_lang: str | None,
        department: str | None,
        confidence_threshold: float,
    ) -> dict:
        """对一批工作台词条执行 Agent 预翻译。

        Args:
            entries: 词条 dict 列表。
            task_id: 翻译任务 id。
            task_name: 任务名称。
            product_name: 产品名称。
            target_lang: 目标语种。
            department: 部门/可视范围。
            confidence_threshold: 自动批准置信度阈值。

        Returns:
            ``list`` / ``auto_count`` / ``pending_count`` 三字段 dict。
        """
        results: list[dict] = []
        auto_count = 0
        pending_count = 0

        for entry in entries:
            if entry.get("parentID"):
                continue
            if not (entry.get("entry") or ""):
                continue

            result_item = await run_single_pre_translate(
                self._session,
                entry=entry,
                task_id=task_id,
                task_name=task_name,
                product_name=product_name,
                target_lang=target_lang,
                department=department,
                confidence_threshold=confidence_threshold,
            )

            meta = result_item.get("agent_meta") or {}
            if meta.get("review_status") == "auto_approved":
                auto_count += 1
            else:
                pending_count += 1

            results.append(result_item)

        return {
            "list": results,
            "auto_count": auto_count,
            "pending_count": pending_count,
        }

    @staticmethod
    def new_translate_id() -> str:
        """生成 t_translate.id（32 位 hex）。

        Returns:
            32 字符十六进制字符串。
        """
        return uuid.uuid4().hex[:32]
