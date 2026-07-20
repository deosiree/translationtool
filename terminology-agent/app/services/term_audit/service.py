"""术语学习审核服务 — 待审核列表、详情与确认入库（MergeToStore）。"""

from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiError
from app.repository.term_repo import TermRepository
from app.repository.word_repo import WordRepository
from app.schemas.agent import TermBatchReviewResult, TermBatchReviewFailure, TermAuditListFilters
from app.services.term_audit.pending_dedupe import dedupe_pending_by_entry_key
from app.services.workbench_sync import WorkbenchEntrySyncService
from app.shared.term_word.stopwords import normalize_cn_lexemes


class TermAuditService:
    """审核列表/详情/确认 — 与 PreTranslateService 同构的领域服务。"""

    def __init__(self, session: AsyncSession) -> None:
        """注入异步数据库会话，内部创建 Repository。

        Args:
            session: FastAPI Depends(get_session) 提供的 AsyncSession。
        """
        self._repo = TermRepository(session)
        self._word_repo = WordRepository(session)
        self._workbench_sync = WorkbenchEntrySyncService(session)

    async def list_pending(
        self,
        *,
        page: int,
        page_size: int,
        filters: TermAuditListFilters | None = None,
    ) -> tuple[list, int]:
        """分页查询 review_status=pending 的 audit 记录。

        同页内按「源词条+comment+语种+部门」软去重（保留最新），减轻 LLM 多次跑出的重复行。
        ``total`` 仍为库内 pending 总数；彻底清理请用 ``devtools.cleanup_adm_test_data``。

        Args:
            page: 页码，从 1 开始。
            page_size: 每页条数。
            filters: 可选列表筛选条件。

        Returns:
            (records, total) — ORM 列表与总数。
        """
        records, total = await self._repo.list_pending_audits(
            page=page,
            page_size=page_size,
            filters=filters,
        )
        return dedupe_pending_by_entry_key(records), total

    async def get_audit_or_raise(self, audit_id: str):
        """按 id 查询 audit，不存在时抛 ApiError。

        Args:
            audit_id: term_agent_audit 主键。

        Returns:
            TermAgentAudit ORM 对象。

        Raises:
            ApiError: 记录不存在。
        """
        record = await self._repo.get_audit(audit_id)
        if record is None:
            raise ApiError(f"审核记录 {audit_id} 不存在")
        return record

    async def review(self, audit_id: str, *, action: str, comment: str | None):
        """人工确认或拒绝 audit 记录。

        approved 且存在 suggested_translation 时，调用 _merge_to_store 写入术语库。

        Args:
            audit_id: term_agent_audit 主键。
            action: ``approved`` 或 ``rejected``。
            comment: 可选审核备注。

        Returns:
            更新后的 TermAgentAudit ORM 对象。

        Raises:
            ApiError: 记录不存在或已是终态。
        """
        record = await self.get_audit_or_raise(audit_id)
        if record.review_status != "pending":
            raise ApiError(
                f"审核记录 {audit_id} 已是终态「{record.review_status}」，无法重复审核"
            )

        if action == "approved" and record.suggested_translation:
            await self._sync_workbench_entry(record)

        await self._repo.update_audit(
            audit_id,
            review_status=action,
            review_comment=comment,
        )

        if action == "approved" and record.suggested_translation:
            await self._merge_to_store(record)

        # 审核通过且存在 segment_trace 时，自动将词片导入 term_word
        if action == "approved" and record.segment_trace:
            await self._import_segment_trace_to_term_word(record)

        return await self._repo.get_audit(audit_id)

    async def _sync_workbench_entry(self, record) -> None:
        """术语学习同意后回写工作台任务译文并推进到待审核。"""
        if not record.entry_info_id or not record.target_lang:
            return
        await self._workbench_sync.sync_translation_to_pending_audit(
            entry_info_id=record.entry_info_id,
            target_lang=record.target_lang,
            translate=record.suggested_translation,
            audit_suggest=record.llm_reasoning,
            department=record.department,
            entry_text=record.source_text,
        )

    async def batch_review(
        self,
        ids: list[str],
        *,
        action: str,
        comment: str | None,
    ) -> TermBatchReviewResult:
        """批量人工确认或拒绝 audit 记录；单条失败不中断。

        Args:
            ids: term_agent_audit 主键列表。
            action: ``approved`` 或 ``rejected``。
            comment: 可选审核备注。

        Returns:
            成功/失败计数及失败明细。
        """
        success_count = 0
        failures: list[TermBatchReviewFailure] = []

        for audit_id in ids:
            try:
                await self.review(audit_id, action=action, comment=comment)
                success_count += 1
            except ApiError as exc:
                failures.append(TermBatchReviewFailure(id=audit_id, reason=str(exc)))

        return TermBatchReviewResult(
            success_count=success_count,
            failed_count=len(failures),
            failures=failures,
        )

    async def _merge_to_store(self, record) -> None:
        """审核通过后写入 t_translate，术语库已有精确匹配时跳过。

        Args:
            record: TermAgentAudit ORM 对象。
        """
        existing = await self._repo.find_exact(
            record.source_text,
            record.target_lang,
            record.department,
        )
        if not existing:
            await self._repo.insert_translate(
                entry=record.source_text,
                translate=record.suggested_translation,
                target_lang=record.target_lang,
                department=record.department,
            )

    async def _import_segment_trace_to_term_word(self, record) -> None:
        """审核通过后，从 segment_trace.jieba 提取词片导入 term_word。

        已存在的词片跳过，不重复写入。
        """
        trace = record.segment_trace
        if not isinstance(trace, dict):
            return
        jieba_tokens: list[str] = normalize_cn_lexemes(trace.get("jieba") or [])
        if not jieba_tokens:
            return

        created = 0
        for token in jieba_tokens:
            existing = await self._word_repo.find_by_key(
                word=token,
                comment="",
                target_lang=record.target_lang or "",
            )
            if existing:
                continue
            await self._word_repo.create_word({
                "word": token,
                "target_lang": record.target_lang or "",
                "department": record.department,
                "status": "1",
                "comment": "",
            })
            created += 1

        if created:
            from app.repository.trie_cache import clear_trie_cache
            clear_trie_cache()
