"""term_word 端点 — 列表 / CRUD / Excel 导入导出 / 拆分预览 / 注意事项适配。"""

from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, File, Query, UploadFile
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiError
from app.core.response import ResponseCode, ok
from app.models.database import get_session
from app.models.word_constants import (
    WORD_STATUS_APPROVED,
    WORD_STATUS_PENDING,
    WORD_STATUS_REJECTED,
)
from app.repository.trie_cache import clear_trie_cache
from app.repository.word_repo import WordRepository
from app.schemas.agent import (
    TermWordBatchDeleteRequest,
    TermWordBatchReviewRequest,
    TermWordCreateRequest,
    TermWordExportRequest,
    TermWordExportRowsRequest,
    TermWordImportRowsRequest,
    TermWordSplitPreviewRequest,
    TermWordUpdateRequest,
)
from app.shared.term_word.excel_io import (
    build_template_bytes,
    build_workbook,
    parse_import_rows,
    workbook_to_bytes,
)
from app.shared.term_word.notes_adapt import adapt_notes_workbook
from app.shared.term_word.split_preview import split_items_preview

router = APIRouter()


def attachment_headers(ascii_name: str, utf8_name: str | None = None) -> dict[str, str]:
    """构建可 latin-1 编码的 Content-Disposition（避免中文 filename 触发 203）。"""
    from urllib.parse import quote

    disposition = f'attachment; filename="{ascii_name}"'
    if utf8_name:
        disposition += f"; filename*=UTF-8''{quote(utf8_name)}"
    return {"Content-Disposition": disposition}


async def _insert_standard_rows(
    repo: WordRepository,
    rows: list[dict],
    *,
    force_pending: bool,
) -> dict:
    """标准行入库：重复键跳过；返回 created/skipped 统计。"""
    created = 0
    skipped = 0
    skip_details: list[str] = []
    for row in rows:
        word = str(row.get("word") or "").strip()
        target_lang = str(row.get("target_lang") or "").strip()
        if not word or not target_lang:
            skipped += 1
            skip_details.append(f"跳过：词片/翻译类型为空 ({word!r}/{target_lang!r})")
            continue
        if force_pending and (row.get("translate") or "").strip():
            row = {**row, "status": WORD_STATUS_PENDING}
        existing = await repo.find_by_key(
            word,
            comment=row.get("comment") or "",
            target_lang=target_lang,
        )
        if existing is not None:
            skipped += 1
            skip_details.append(
                f"第{row.get('_row')}行跳过：已存在 {word}/{target_lang}"
            )
            continue
        payload = {
            "word": word,
            "translate": (row.get("translate") or "").strip(),
            "target_lang": target_lang,
            "department": row.get("department"),
            "comment": row.get("comment") or "",
            "category": row.get("category"),
            "abbr": row.get("abbr"),
            "usage_notes": row.get("usage_notes"),
            "use_llm": bool(row.get("use_llm")),
            "status": row.get("status") or WORD_STATUS_PENDING,
            "source_translate_id": "",
        }
        await repo.create_word(payload)
        created += 1
    await repo.commit()
    if created:
        clear_trie_cache()
    return {
        "created": created,
        "skipped": skipped,
        "skipDetails": skip_details[:50],
    }


def _row_to_dict(r) -> dict:
    return {
        "id": r.id,
        "word": r.word,
        "comment": r.comment,
        "translate": r.translate,
        "target_lang": r.target_lang,
        "department": r.department,
        "source_translate_id": getattr(r, "source_translate_id", None),
        "source_entry_info_id": getattr(r, "source_entry_info_id", None),
        "task_id": r.task_id,
        "product_id": r.product_id,
        "category": getattr(r, "category", None),
        "abbr": getattr(r, "abbr", None),
        "usage_notes": getattr(r, "usage_notes", None),
        "use_llm": bool(getattr(r, "use_llm", False)),
        "status": r.status,
        "created_at": r.created_at.isoformat(sep=" ", timespec="seconds")
        if getattr(r, "created_at", None)
        else None,
        "updated_at": r.updated_at.isoformat(sep=" ", timespec="seconds")
        if getattr(r, "updated_at", None)
        else None,
    }


@router.get("/list", summary="term_word 分页列表（术语字典 Tab）")
async def list_words(
    page: int = 1,
    pageSize: int = Query(20, alias="pageSize", ge=1, le=200),
    word: Optional[str] = Query(None, description="词片原文，模糊匹配"),
    translate: Optional[str] = Query(None, description="翻译，模糊匹配"),
    targetLang: Optional[str] = Query(None, alias="targetLang"),
    department: Optional[str] = Query(None),
    status: Optional[str] = Query(None, description="0|1|2|3；空=全部"),
    session: AsyncSession = Depends(get_session),
):
    """供前端「术语库 → 术语字典」浏览切分命中依据。"""
    rows, total = await WordRepository(session).list_words(
        page=page,
        page_size=pageSize,
        word=word,
        translate=translate,
        target_lang=targetLang,
        department=department,
        status=status,
    )
    return ok(
        {
            "list": [_row_to_dict(r) for r in rows],
            "total": total,
            "page": page,
            "pageSize": pageSize,
        }
    )


@router.get("/import-template", summary="下载术语字典导入模板")
async def download_import_template(
    withSample: bool = Query(True, alias="withSample"),
):
    """下载标准 xlsx；默认带 5 条样例。"""
    data = build_template_bytes(with_sample=withSample)
    filename = "term_word_import_sample.xlsx" if withSample else "term_word_import_template.xlsx"
    return Response(
        content=data,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers=attachment_headers(filename),
    )


@router.post("/import", summary="导入术语字典 Excel")
async def import_words(
    file: UploadFile = File(...),
    forcePendingWhenTranslated: bool = Query(
        False,
        alias="forcePendingWhenTranslated",
        description="True 时有翻译一律待审核（词典拆分导入用）",
    ),
    session: AsyncSession = Depends(get_session),
):
    """解析标准模板；重复键跳过。"""
    raw = await file.read()
    if not raw:
        raise ApiError("空文件", code=ResponseCode.PARAM_ERROR)
    rows, parse_errors = parse_import_rows(raw)
    repo = WordRepository(session)
    result = await _insert_standard_rows(
        repo, rows, force_pending=forcePendingWhenTranslated
    )
    return ok({**result, "parseErrors": parse_errors})


@router.post("/import-rows", summary="按标准行直接导入术语字典")
async def import_rows(
    body: TermWordImportRowsRequest,
    session: AsyncSession = Depends(get_session),
):
    """拆分一键导入：客户端组表，跳过 Excel 往返。"""
    repo = WordRepository(session)
    result = await _insert_standard_rows(
        repo, [dict(r) for r in body.rows], force_pending=body.forcePending
    )
    return ok(result)


@router.post("/export-rows", summary="导出任意标准行 Excel")
async def export_rows(body: TermWordExportRowsRequest):
    """拆分结果等场景：按标准列导出；默认翻译状态待审核。"""
    rows = []
    for raw in body.rows:
        row = dict(raw)
        if body.forcePending:
            row["status"] = WORD_STATUS_PENDING
        rows.append(row)
    data = workbook_to_bytes(build_workbook(rows, with_guide=False))
    return Response(
        content=data,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers=attachment_headers("split_export.xlsx", "split_export_词典切分.xlsx"),
    )


@router.post("/export", summary="导出术语字典 Excel")
async def export_words(
    body: TermWordExportRequest,
    session: AsyncSession = Depends(get_session),
):
    """按 ids 或当前筛选导出标准 xlsx。"""
    repo = WordRepository(session)
    if body.ids:
        rows = await repo.list_by_ids(body.ids)
    else:
        rows, _ = await repo.list_words(
            page=1,
            page_size=5000,
            word=body.word,
            translate=body.translate,
            target_lang=body.targetLang,
            department=body.department,
            status=body.status,
        )
    payload = [_row_to_dict(r) for r in rows]
    data = workbook_to_bytes(build_workbook(payload, with_guide=False))
    return Response(
        content=data,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers=attachment_headers("term_word_export.xlsx"),
    )


@router.post("/notes-adapt", summary="注意事项清单适配为标准行")
async def notes_adapt(
    file: UploadFile = File(...),
    targetLang: str = Query("英文", alias="targetLang"),
):
    """只解析「通用术语」→ 返回标准行 JSON（默认待审核）。"""
    raw = await file.read()
    try:
        rows = adapt_notes_workbook(raw, target_lang=targetLang, default_status=WORD_STATUS_PENDING)
    except ValueError as exc:
        raise ApiError(str(exc), code=ResponseCode.PARAM_ERROR) from exc
    return ok({"list": rows, "total": len(rows)})


@router.post("/split-preview", summary="术语词典拆分预览")
async def split_preview(
    body: TermWordSplitPreviewRequest,
    session: AsyncSession = Depends(get_session),
):
    """jieba 切分 + 过滤无意义词 + 译法对齐；可选批量 LLM 补译。"""
    repo = WordRepository(session)
    items = [it.model_dump(by_alias=False) for it in body.items]
    # 预取 lexicon：各语种分别查
    by_lang: dict[str, list[str]] = {}
    from app.shared.term_word.extract import extract_words
    from app.shared.term_word.split_preview import boost_split_jieba_words
    from app.shared.term_word.stopwords import filter_cn_tokens

    boost_split_jieba_words()
    for it in items:
        lang = it.get("target_lang") or ""
        tokens = filter_cn_tokens(extract_words(it.get("entry") or ""))
        by_lang.setdefault(lang, []).extend(tokens)

    lexicon: dict[str, str] = {}
    for lang, words in by_lang.items():
        if not lang:
            continue
        part = await repo.build_lexicon(target_lang=lang, words=words)
        lexicon.update(part)

    fill_with_llm = bool(body.fill_with_llm)
    candidates = split_items_preview(
        items,
        lexicon=lexicon,
        keep_untranslated=fill_with_llm,
    )
    if fill_with_llm:
        from app.shared.term_word.split_llm_fill import (
            build_fill_jobs_from_items,
            fill_fragments_with_llm,
            merge_fill_into_candidates,
        )

        jobs = build_fill_jobs_from_items(items, candidates)
        fills = await fill_fragments_with_llm(jobs)
        candidates = merge_fill_into_candidates(candidates, fills)
        if not candidates:
            candidates = split_items_preview(
                items, lexicon=lexicon, keep_untranslated=False
            )

    return ok({"list": candidates, "total": len(candidates)})


@router.post("", summary="新建 term_word")
@router.post("/", summary="新建 term_word", include_in_schema=False)
async def create_word(
    body: TermWordCreateRequest,
    session: AsyncSession = Depends(get_session),
):
    repo = WordRepository(session)
    row = await repo.create_word(
        {
            "word": body.word.strip(),
            "translate": body.translate.strip(),
            "target_lang": body.target_lang.strip(),
            "department": body.department,
            "comment": (body.comment or "").strip(),
            "category": body.category,
            "abbr": body.abbr,
            "usage_notes": body.usage_notes,
            "use_llm": bool(body.use_llm),
            "status": body.status,
            "source_translate_id": "",
        }
    )
    await repo.commit()
    clear_trie_cache()
    return ok(_row_to_dict(row))


@router.put("/{word_id}", summary="更新 term_word")
async def update_word(
    word_id: str,
    body: TermWordUpdateRequest,
    session: AsyncSession = Depends(get_session),
):
    repo = WordRepository(session)
    row = await repo.get_by_id(word_id)
    if row is None:
        raise ApiError("词片不存在", code=ResponseCode.PARAM_ERROR)

    payload = body.model_dump(exclude_unset=True)
    for key in ("word", "translate", "target_lang", "comment"):
        if key in payload and payload[key] is not None:
            payload[key] = payload[key].strip()

    row = await repo.update_word(row, payload)
    await repo.commit()
    clear_trie_cache()
    return ok(_row_to_dict(row))


@router.delete("/{word_id}", summary="删除 term_word")
async def delete_word(
    word_id: str,
    session: AsyncSession = Depends(get_session),
):
    repo = WordRepository(session)
    deleted = await repo.delete_by_ids([word_id])
    if deleted == 0:
        raise ApiError("词片不存在", code=ResponseCode.PARAM_ERROR)
    await repo.commit()
    clear_trie_cache()
    return ok({"deleted": deleted})


@router.post("/batch-delete", summary="批量删除 term_word")
async def batch_delete_words(
    body: TermWordBatchDeleteRequest,
    session: AsyncSession = Depends(get_session),
):
    repo = WordRepository(session)
    deleted = await repo.delete_by_ids(body.ids)
    await repo.commit()
    clear_trie_cache()
    return ok({"deleted": deleted})


@router.post("/batch-review", summary="批量审阅 term_word（通过/驳回）")
async def batch_review_words(
    body: TermWordBatchReviewRequest,
    session: AsyncSession = Depends(get_session),
):
    """将勾选词片批量设为已审核(3)或审核不通过(2)；非待审核行计入 skipped。"""
    target_status = (
        WORD_STATUS_APPROVED if body.action == "approved" else WORD_STATUS_REJECTED
    )
    repo = WordRepository(session)
    rows = await repo.list_by_ids(body.ids)
    by_id = {r.id: r for r in rows}
    updated = 0
    skipped = 0
    missing = 0
    for wid in body.ids:
        row = by_id.get(wid)
        if row is None:
            missing += 1
            continue
        if row.status != WORD_STATUS_PENDING:
            skipped += 1
            continue
        await repo.update_word(row, {"status": target_status})
        updated += 1
    await repo.commit()
    if updated:
        clear_trie_cache()
    return ok(
        {
            "updated": updated,
            "skipped": skipped,
            "missing": missing,
            "status": target_status,
        }
    )


@router.get("/{word}", summary="Grep 线调试 — 按 word 查 term_word")
async def lookup_word(
    word: str,
    target_lang: str = Query(..., description="目标语种，如俄文"),
    comment: str | None = Query(None, description="消歧 comment；省略则不过滤"),
    department: str | None = Query(None, description="部门过滤，非消歧键"),
    session: AsyncSession = Depends(get_session),
):
    """调试端点 — live keyword lookup on term_word（仅 status=3 已审核）。"""
    rows = await WordRepository(session).find_by_word(
        word,
        target_lang=target_lang,
        comment=comment,
        department=department,
    )
    data = [_row_to_dict(r) for r in rows]
    return ok({"items": data, "count": len(data)})
