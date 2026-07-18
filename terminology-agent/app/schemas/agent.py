"""Pydantic 请求/响应模型 — 与前端 axios 拦截器及 Java 后端包装格式对齐。

前端期望响应结构：{ code: 200, message: "success", data: {...} }
"""

from __future__ import annotations

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field, field_validator, ConfigDict, model_validator

# ── 请求体 ──


class TermReviewRequest(BaseModel):
    """POST /agent/term-learning/{id}/review — 术语学习页人工确认/拒绝。"""
    action: str = Field(..., pattern="^(approved|rejected)$", description="approved=确认入库 | rejected=拒绝")
    comment: Optional[str] = Field(None, max_length=512, description="审核备注")


class TermBatchReviewRequest(BaseModel):
    """POST /agent/term-learning/batch/review — 术语学习页批量确认/拒绝。"""
    ids: list[str] = Field(..., min_length=1, description="待审核记录 id 列表")
    action: str = Field(..., pattern="^(approved|rejected)$", description="approved=确认入库 | rejected=拒绝")
    comment: Optional[str] = Field(None, max_length=512, description="审核备注")


class TermBatchReviewFailure(BaseModel):
    """批量审核单条失败明细。"""
    id: str
    reason: str


class TermBatchReviewResult(BaseModel):
    """POST /agent/term-learning/batch/review 的 data 字段。"""
    success_count: int = Field(0, description="成功处理条数")
    failed_count: int = Field(0, description="失败条数")
    failures: list[TermBatchReviewFailure] = Field(default_factory=list, description="失败明细")


class PreTranslateBatchRequest(BaseModel):
    """POST /agent/pre-translate/batch — 工作台批量 Agent 预翻译。

    Body 必须为对象：{ entries, task_name, product_name, target_lang, department }
    """
    entries: list[dict] = Field(default_factory=list, description="工作台词条列表，结构与 Java preTranslate 一致")
    task_name: Optional[str] = Field(None, description="任务名称，供术语学习列表展示")
    product_name: Optional[str] = Field(None, description="产品名称")
    target_lang: Optional[str] = Field(None, description="目标语种，如「俄文」")
    department: Optional[str] = Field(None, description="部门所属，对应术语库 visual_range")


class TermAuditListFilters(BaseModel):
    """GET /agent/term-learning/list 可选筛选条件。"""

    source_text: Optional[str] = Field(None, description="词条模糊匹配")
    target_lang: Optional[str] = Field(None, description="目标语种精确匹配")
    task_name: Optional[str] = Field(None, description="任务名称模糊匹配")
    product_name: Optional[str] = Field(None, description="产品名称模糊匹配")
    department: Optional[str] = Field(None, description="部门所属精确匹配")
    confidence_min: Optional[float] = Field(None, ge=0, le=1, description="置信度下限 0~1")
    confidence_max: Optional[float] = Field(None, ge=0, le=1, description="置信度上限 0~1")
    retrieval_method: Optional[str] = Field(
        None,
        description="检索方式 exact|fuzzy|grep|hybrid|decomposed|none",
    )
    entry_comment: Optional[str] = Field(None, description="工作台词条 comment 模糊匹配")

    @field_validator(
        "source_text",
        "target_lang",
        "task_name",
        "product_name",
        "department",
        "retrieval_method",
        "entry_comment",
        mode="before",
    )
    @classmethod
    def _strip_optional_text(cls, value):
        if value is None:
            return None
        text = str(value).strip()
        return text or None

    @model_validator(mode="after")
    def _validate_confidence_range(self):
        if (
            self.confidence_min is not None
            and self.confidence_max is not None
            and self.confidence_min > self.confidence_max
        ):
            raise ValueError("confidence_min 不能大于 confidence_max")
        return self


# ── 响应 data 层 ──


class SimilarTermData(BaseModel):
    """RAG/Grep 检索命中的参考术语对 — 对应前端 similar_terms Popover。"""
    entry: str = Field(..., description="参考词条原文")
    translate: str = Field(..., description="参考词条已有译文")
    score: Optional[float] = Field(None, description="相似度分数 0~1")
    retrieval_source: Optional[str] = Field(None, description="rag | grep | rag+grep")


class AuditRecordData(BaseModel):
    """术语学习待审核列表单行 — 字段与前端 terminologyAgent/index.vue 列一一对应。"""
    id: str
    source_text: str = Field(..., description="词条")
    entry_comment: Optional[str] = Field(None, description="工作台词条 comment / Grep 消歧")
    context: Optional[str] = None
    matched_term: Optional[str] = None
    match_confidence: Optional[float] = None
    is_new_term: bool = False
    suggested_translation: Optional[str] = Field(None, description="Agent 建议译文（低于阈值时也需展示）")
    llm_reasoning: Optional[str] = Field(None, description="Agent 说明")
    segment_trace: Optional[dict] = Field(
        None, description="jieba/对齐切分轨迹 {jieba,aligned,display}"
    )
    review_status: str
    review_comment: Optional[str] = None
    error: Optional[str] = None

    # 工作台 + Agent 扩展字段
    entry_info_id: Optional[str] = Field(None, description="工作台词条 id")
    task_id: Optional[str] = None
    task_name: Optional[str] = None
    product_name: Optional[str] = None
    target_lang: Optional[str] = None
    department: Optional[str] = None
    confidence: Optional[float] = Field(None, description="预翻译置信度，仅审核页展示")
    similar_terms: Optional[list[SimilarTermData]] = Field(None, description="参考术语列表")
    retrieval_method: Optional[str] = Field(None, description="exact | fuzzy | grep | hybrid | decomposed | none")
    source_type: Optional[str] = Field("workbench_agent", description="记录来源")

    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}

    @field_validator("is_new_term", mode="before")
    @classmethod
    def _coerce_is_new_term(cls, value):
        """MySQL TINYINT(1) 经 ORM 读出为 0/1，统一转为 bool。"""
        return bool(value)

    @field_validator("similar_terms", mode="before")
    @classmethod
    def _coerce_similar_terms(cls, value):
        """兼容 JSON 列偶尔被驱动读成 str 的情况。"""
        if value is None:
            return None
        if isinstance(value, str):
            import json
            try:
                value = json.loads(value)
            except json.JSONDecodeError:
                return None
        if isinstance(value, list):
            return value
        return None

    @field_validator("segment_trace", mode="before")
    @classmethod
    def _coerce_segment_trace(cls, value):
        """兼容 JSON 列被读成 str 的情况。"""
        if value is None:
            return None
        if isinstance(value, str):
            import json
            try:
                value = json.loads(value)
            except json.JSONDecodeError:
                return None
        if isinstance(value, dict):
            return value
        return None


class AuditListData(BaseModel):
    """GET /agent/term-learning/list 的 data 字段。"""
    model_config = ConfigDict(populate_by_name=True)

    entry_list: list[AuditRecordData] = Field(..., alias="list")
    total: int


class AgentMetaData(BaseModel):
    """单条词条预翻译结果中的 agent_meta — 与工作台 PreTranslateModal 产出结构一致。"""
    confidence: float
    review_status: str = Field(..., description="auto_approved | needs_human")
    suggested_translation: Optional[str] = None
    similar_terms: list[SimilarTermData] = Field(default_factory=list)
    retrieval_method: str = ""
    reasoning: str = ""
    segment_trace: Optional[dict] = Field(
        None, description="jieba/对齐切分轨迹；未走过切分时为 null"
    )


class PreTranslateBatchData(BaseModel):
    """POST /agent/pre-translate/batch 的 data 字段。

    字段名用 entry_list 避免遮蔽内置 list；JSON 序列化仍为 "list"。
    """
    model_config = ConfigDict(populate_by_name=True)

    entry_list: list[dict] = Field(..., alias="list", description="预翻译后的词条列表")
    auto_count: int = Field(0, description="高于阈值、已自动回填的数量")
    pending_count: int = Field(0, description="低于阈值、写入待审核队列的数量")


class HealthData(BaseModel):
    """GET /agent/health 的 data 字段。"""
    status: str = "ok"
    service: str = "术语学习 Agent"
