"""拆分词片批量 LLM 补译 — 一批词条一次调用。"""

from __future__ import annotations

import json
import logging
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import BaseModel, Field

from app.graph.pre_translate.utils.llm_json import parse_llm_output
from config.settings import settings

logger = logging.getLogger(__name__)

MAX_ENTRIES_PER_BATCH = 20
MAX_FRAGMENTS_PER_BATCH = 80

_SYSTEM_PROMPT = """你是术语词典拆分助手。根据每条「整句词条 + 整句译文」，为已拆好的中文词片补上对应译法。

硬性要求：
1. 词片译法必须能从该条整句 translate 中合理解释（考虑中英语序差异）。
2. 禁止发明整句译文中不存在的词义。
3. 不确定时将该词片 translate 置为空字符串。
4. 只输出 JSON，不要 Markdown 代码围栏以外的说明。
5. 输出格式：{"results":[{"id":"...","items":[{"word":"...","translate":"..."}]}]}
"""


class _FillItem(BaseModel):
    word: str
    translate: str = ""


class _FillEntry(BaseModel):
    id: str
    items: list[_FillItem] = Field(default_factory=list)


class SplitFillLlmOutput(BaseModel):
    results: list[_FillEntry] = Field(default_factory=list)


def _chunk_jobs(jobs: list[dict[str, Any]]) -> list[list[dict[str, Any]]]:
    """按词条数 / 词片数切批。"""
    batches: list[list[dict[str, Any]]] = []
    current: list[dict[str, Any]] = []
    frag_count = 0
    for job in jobs:
        frags = job.get("fragments") or []
        need = len(frags)
        if current and (
            len(current) >= MAX_ENTRIES_PER_BATCH
            or frag_count + need > MAX_FRAGMENTS_PER_BATCH
        ):
            batches.append(current)
            current = []
            frag_count = 0
        current.append(job)
        frag_count += need
    if current:
        batches.append(current)
    return batches


def build_fill_jobs_from_items(
    items: list[dict[str, Any]],
    candidates: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """从源词条与本地候选组装 LLM 补译任务（仅缺译词片）。"""
    by_entry: dict[str, list[dict[str, Any]]] = {}
    for row in candidates:
        entry = (row.get("source_entry") or "").strip()
        if not entry:
            continue
        by_entry.setdefault(entry, []).append(row)

    jobs: list[dict[str, Any]] = []
    for idx, item in enumerate(items):
        entry = str(item.get("entry") or item.get("word") or "").strip()
        translate = str(item.get("translate") or "").strip()
        if not entry or not translate:
            continue
        rows = by_entry.get(entry) or []
        need = [
            r["word"]
            for r in rows
            if r.get("word") and not str(r.get("translate") or "").strip()
        ]
        # 去重保序
        seen: set[str] = set()
        fragments: list[str] = []
        for w in need:
            if w in seen:
                continue
            seen.add(w)
            fragments.append(w)
        if not fragments:
            continue
        jobs.append(
            {
                "id": f"e{idx}",
                "entry": entry,
                "translate": translate,
                "fragments": fragments,
            }
        )
    return jobs


def merge_fill_into_candidates(
    candidates: list[dict[str, Any]],
    fills: dict[str, dict[str, str]],
) -> list[dict[str, Any]]:
    """将 LLM 回填合并进候选；丢掉仍无译的行；use_llm 保持 False。"""
    out: list[dict[str, Any]] = []
    for row in candidates:
        entry = (row.get("source_entry") or "").strip()
        word = (row.get("word") or "").strip()
        tr = str(row.get("translate") or "").strip()
        if not tr and entry in fills:
            tr = (fills[entry].get(word) or "").strip()
        if not tr:
            continue
        merged = {**row, "translate": tr, "use_llm": False}
        out.append(merged)
    return out


async def fill_fragments_with_llm(
    jobs: list[dict[str, Any]],
) -> dict[str, dict[str, str]]:
    """批量补译。返回 entry → {word → translate}。失败返回空 dict。"""
    if not jobs:
        return {}
    api_key = settings.llm_api_key
    if not api_key:
        logger.warning("split llm fill skipped: llm_api_key empty")
        return {}

    from langchain_openai import ChatOpenAI

    llm = ChatOpenAI(
        api_key=api_key,
        base_url=settings.llm_base_url,
        model=settings.llm_model,
        temperature=settings.llm_temperature,
    )

    id_to_entry = {str(j["id"]): j["entry"] for j in jobs}
    merged: dict[str, dict[str, str]] = {}

    for batch in _chunk_jobs(jobs):
        payload = [
            {
                "id": j["id"],
                "entry": j["entry"],
                "translate": j["translate"],
                "fragments": j["fragments"],
            }
            for j in batch
        ]
        user_text = (
            "请为下列词条的词片补译，严格按 JSON schema 返回。\n"
            + json.dumps(payload, ensure_ascii=False)
        )
        try:
            response = await llm.ainvoke(
                [
                    SystemMessage(content=_SYSTEM_PROMPT),
                    HumanMessage(content=user_text),
                ]
            )
            content = (response.content or "").strip()
            parsed = parse_llm_output(content, SplitFillLlmOutput)
            if parsed is None:
                logger.warning("split llm fill parse failed: %s", content[:200])
                continue
            for entry_out in parsed.results:
                entry_key = id_to_entry.get(entry_out.id)
                if not entry_key:
                    continue
                bucket = merged.setdefault(entry_key, {})
                for it in entry_out.items:
                    w = (it.word or "").strip()
                    t = (it.translate or "").strip()
                    if w and t:
                        bucket[w] = t
        except Exception:
            logger.exception("split llm fill batch failed")
            continue

    return merged
