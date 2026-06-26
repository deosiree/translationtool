"""工作台批量 Agent 预翻译服务。

流程（对应 LangGraph Demo 设计，当前以规则引擎实现 MVP）：
  RetrieveSimilar → AssessConfidence → 分流
    ├─ confidence >= threshold → auto_approved（回填翻译）
    └─ confidence <  threshold → needs_human（写 term_agent_audit）
"""

from __future__ import annotations

import re
import uuid
from difflib import SequenceMatcher

from sqlalchemy.ext.asyncio import AsyncSession

from app.repository.term_repo import TermRepository


def _strip_placeholders(text: str) -> str:
    """去掉 %1/%2 等占位符后再做相似度比较，避免工业 i18n 词条误判。"""
    return re.sub(r"%\d+", "", text or "").strip()


def _similarity(a: str, b: str) -> float:
    """基于 SequenceMatcher 的字符级相似度，范围 0~1。"""
    if not a or not b:
        return 0.0
    return SequenceMatcher(None, a, b).ratio()


def _parse_target_lang(translate_type: str | None) -> str:
    """从任务 translateType（如「中文-俄文」）解析目标语种。"""
    if not translate_type:
        return ""
    parts = str(translate_type).split("-")
    return parts[1] if len(parts) > 1 else translate_type


class PreTranslateService:
    """批量预翻译：术语库 RAG 检索 + 置信度路由。

    与 Java EntryTempServiceImpl.getSYKTranslate 对齐：
      - 精确匹配优先（entry + type + visual_range + translate_state=3）
      - 多条时 Java 取 lastUseTime 最新；此处 find_exact 已 order_by desc
    """

    def __init__(self, session: AsyncSession):
        """注入异步数据库会话，内部创建 TermRepository。"""
        self._repo = TermRepository(session)

    async def batch_pre_translate(
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
            entries: 前端词条列表（含 id、entry、各语种字段）。
            task_id: 翻译任务 id（query 参数 taskID）。
            task_name: 任务名称，写入 audit 供列表展示。
            product_name: 产品名称。
            target_lang: 目标语种。
            department: 部门所属（对应 visual_range）。
            confidence_threshold: 置信度阈值，默认 0.8。

        Returns:
            含 list、auto_count、pending_count 的结果字典。
        """
        results: list[dict] = []
        auto_count = 0
        pending_count = 0

        for entry in entries:
            # 子词条不参与预翻译（与 Java preTranslate 一致）
            if entry.get("parentID"):
                continue

            source_entry = entry.get("entry") or ""
            if not source_entry:
                continue

            retrieval = await self._retrieve_similar(
                source_entry=source_entry,
                target_lang=target_lang,
                department=department,
            )
            confidence = retrieval["confidence"]
            suggested = retrieval["suggested_translation"]
            review_status = "auto_approved" if confidence >= confidence_threshold else "needs_human"

            # agent_meta 结构对齐前端 PreTranslateModal / terminologyAgent 列表
            agent_meta = {
                "confidence": confidence,
                "review_status": review_status,
                "suggested_translation": suggested,
                "similar_terms": retrieval["similar_terms"],
                "retrieval_method": retrieval["retrieval_method"],
                "reasoning": retrieval["reasoning"],
            }

            result_item = {**entry, "agent_meta": agent_meta}

            if review_status == "auto_approved":
                # 高于阈值：写入目标语字段，工作台直接回填
                lang_key = self._guess_lang_field(entry, target_lang)
                if lang_key:
                    result_item[lang_key] = suggested
                result_item["translate"] = suggested
                auto_count += 1
            else:
                # 低于阈值：不写回翻译列，持久化到 term_agent_audit 待人工确认
                pending_count += 1
                await self._repo.create_pretranslate_audit(
                    entry_info_id=entry.get("id"),
                    task_id=task_id,
                    task_name=task_name,
                    product_name=product_name,
                    target_lang=target_lang,
                    department=department,
                    source_text=source_entry,
                    suggested_translation=suggested,
                    confidence=confidence,
                    similar_terms=retrieval["similar_terms"],
                    retrieval_method=retrieval["retrieval_method"],
                    llm_reasoning=retrieval["reasoning"],
                )

            results.append(result_item)

        return {
            "list": results,
            "auto_count": auto_count,
            "pending_count": pending_count,
        }

    async def _retrieve_similar(
        self,
        *,
        source_entry: str,
        target_lang: str | None,
        department: str | None,
    ) -> dict:
        """RetrieveSimilar 节点：精确匹配 → 模糊匹配 → 无命中兜底。

        Returns:
            含 confidence、suggested_translation、similar_terms、
            retrieval_method、reasoning 的字典。
        """
        # 1) 精确匹配 — 对齐 Java getVersionSuggestTrans
        exact = await self._repo.find_exact(source_entry, target_lang, department)
        if exact and exact.translate:
            return {
                "confidence": 1.0,
                "suggested_translation": exact.translate,
                "similar_terms": [
                    {"entry": exact.entry or source_entry, "translate": exact.translate, "score": 1.0}
                ],
                "retrieval_method": "exact",
                "reasoning": "术语库精确匹配",
            }

        # 2) 模糊匹配 — LIKE + 去占位符相似度
        fuzzy_matches = await self._repo.find_fuzzy(source_entry, target_lang, department, limit=5)
        similar_terms = []
        best_score = 0.0
        best_translate = None

        core = _strip_placeholders(source_entry)
        for match in fuzzy_matches:
            if not match.translate:
                continue
            score = _similarity(core, _strip_placeholders(match.entry or ""))
            similar_terms.append(
                {"entry": match.entry or "", "translate": match.translate, "score": round(score, 3)}
            )
            if score > best_score:
                best_score = score
                best_translate = match.translate

        if best_translate:
            # 经验公式：基础分 0.55 + 相似度贡献，上限 0.95
            confidence = min(0.55 + best_score * 0.4, 0.95)
            return {
                "confidence": round(confidence, 3),
                "suggested_translation": best_translate,
                "similar_terms": similar_terms[:3],
                "retrieval_method": "fuzzy",
                "reasoning": "基于术语库模糊匹配与相似度评估",
            }

        # 3) 无命中 — 低置信度，强制走人工审核
        fallback = f"[Agent] {source_entry}"
        return {
            "confidence": 0.45,
            "suggested_translation": fallback,
            "similar_terms": [],
            "retrieval_method": "hybrid",
            "reasoning": "未找到相似术语，建议人工审核",
        }

    @staticmethod
    def _guess_lang_field(entry: dict, target_lang: str | None) -> str | None:
        """从词条 dict 中推断目标语字段名（如 russian、english）。

        工作台 language.value 即为该 key；此处遍历排除元数据字段后取第一个 str 型翻译列。
        """
        for key in entry:
            if key in {"id", "entry", "parentID", "translate", "agent_meta"}:
                continue
            if isinstance(entry.get(key), str) and key not in {
                "chineseInterpretation",
                "englishInterpretation",
                "comment",
                "abbr",
                "tag",
                "translateState",
            }:
                return key
        return None

    @staticmethod
    def new_translate_id() -> str:
        """生成 t_translate.id（32 位 hex）。"""
        return uuid.uuid4().hex[:32]
