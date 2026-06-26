"""ORM / 内部对象 → API DTO 转换。"""

from app.schemas.agent import AuditRecordData


def audit_to_data(record) -> AuditRecordData:
    """TermAgentAudit ORM → AuditRecordData（含 similar_terms / is_new_term 类型转换）。"""
    return AuditRecordData.model_validate(record)
