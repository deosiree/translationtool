"""comment_rule 端点 — 列表 / CRUD / Excel 导入。"""

from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, File, Form, Query, UploadFile
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ApiError
from app.core.response import ResponseCode, ok
from app.models.database import get_session
from app.repository.comment_rule_repo import CommentRuleRepository
from app.schemas.agent import CommentRuleCreateRequest, CommentRuleUpdateRequest
from app.shared.comment_rule.excel_io import parse_comment_rule_workbook
from app.shared.comment_rule.format import rule_row_to_dict

router = APIRouter()


@router.get("/list", summary="comment_rule 分页列表")
async def list_comment_rules(
    page: int = 1,
    pageSize: int = Query(20, alias="pageSize", ge=1, le=200),
    commentKey: Optional[str] = Query(None, alias="commentKey"),
    preferAbbr: Optional[bool] = Query(None, alias="preferAbbr"),
    scene: Optional[str] = None,
    ruleText: Optional[str] = Query(None, alias="ruleText"),
    session: AsyncSession = Depends(get_session),
):
    rows, total = await CommentRuleRepository(session).list_rules(
        page=page,
        page_size=pageSize,
        comment_key=commentKey,
        prefer_abbr=preferAbbr,
        scene=scene,
        rule_text=ruleText,
    )
    return ok(
        {
            "list": [rule_row_to_dict(r) for r in rows],
            "total": total,
            "page": page,
            "pageSize": pageSize,
        }
    )


@router.get("/{rule_id}", summary="comment_rule 单条详情")
async def get_comment_rule(
    rule_id: str,
    session: AsyncSession = Depends(get_session),
):
    row = await CommentRuleRepository(session).get_by_id(rule_id)
    if row is None:
        raise ApiError("规则不存在", code=ResponseCode.PARAM_ERROR)
    return ok(rule_row_to_dict(row))


@router.post("", summary="新建 comment 规则")
@router.post("/", summary="新建 comment 规则", include_in_schema=False)
async def create_comment_rule(
    body: CommentRuleCreateRequest,
    session: AsyncSession = Depends(get_session),
):
    repo = CommentRuleRepository(session)
    row = await repo.create_rule(
        {
            "comment_key": body.comment_key.strip(),
            "entry_source": (body.entry_source or "").strip() or None,
            "scene": body.scene,
            "rule_text": body.rule_text,
            "prefer_abbr": bool(body.prefer_abbr),
            "case_type": body.case_type,
            "related_id": (body.related_id or "").strip() or None,
        }
    )
    await repo.commit()
    return ok(rule_row_to_dict(row))


@router.put("/{rule_id}", summary="更新 comment 规则")
async def update_comment_rule(
    rule_id: str,
    body: CommentRuleUpdateRequest,
    session: AsyncSession = Depends(get_session),
):
    repo = CommentRuleRepository(session)
    row = await repo.get_by_id(rule_id)
    if row is None:
        raise ApiError("规则不存在", code=ResponseCode.PARAM_ERROR)
    payload = body.model_dump(exclude_unset=True, by_alias=False)
    if "comment_key" in payload and payload["comment_key"] is not None:
        payload["comment_key"] = str(payload["comment_key"]).strip()
    if "entry_source" in payload and payload["entry_source"] is not None:
        payload["entry_source"] = str(payload["entry_source"]).strip() or None
    if "related_id" in payload and payload["related_id"] is not None:
        payload["related_id"] = str(payload["related_id"]).strip() or None
    await repo.update_rule(row, payload)
    await repo.commit()
    return ok(rule_row_to_dict(row))


@router.delete("/{rule_id}", summary="删除 comment 规则")
async def delete_comment_rule(
    rule_id: str,
    session: AsyncSession = Depends(get_session),
):
    repo = CommentRuleRepository(session)
    ok_del = await repo.delete_by_id(rule_id)
    if not ok_del:
        raise ApiError("规则不存在", code=ResponseCode.PARAM_ERROR)
    await repo.commit()
    return ok({"deleted": True})


@router.post("/import", summary="导入 comment 规则 Excel")
async def import_comment_rules(
    file: UploadFile = File(...),
    overwritePreferAbbr: bool = Form(False, alias="overwritePreferAbbr"),
    session: AsyncSession = Depends(get_session),
):
    data = await file.read()
    if not data:
        raise ApiError("空文件", code=ResponseCode.PARAM_ERROR)
    try:
        rows = parse_comment_rule_workbook(data)
    except Exception as exc:
        raise ApiError(f"解析失败: {exc}", code=ResponseCode.PARAM_ERROR) from exc

    repo = CommentRuleRepository(session)
    created = 0
    updated = 0
    for row in rows:
        existing = await repo.find_by_key_and_scene(
            row["comment_key"], row.get("scene")
        )
        if existing is None:
            await repo.create_rule(row)
            created += 1
            continue
        payload = {
            "entry_source": row.get("entry_source"),
            "scene": row.get("scene"),
            "rule_text": row.get("rule_text"),
            "case_type": row.get("case_type"),
        }
        if overwritePreferAbbr:
            payload["prefer_abbr"] = bool(row.get("prefer_abbr"))
        await repo.update_rule(existing, payload)
        updated += 1
    await repo.commit()
    return ok({"created": created, "updated": updated, "total": len(rows)})
