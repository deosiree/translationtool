"""TermRepository 列表筛选条件单元测试。"""

import pytest

from app.repository.term_repo import TermRepository
from app.schemas.agent import TermAuditListFilters


@pytest.mark.unit
def test_build_pending_filter_conditions_empty():
    conditions = TermRepository._build_pending_filter_conditions(None)
    assert len(conditions) == 1


@pytest.mark.unit
def test_build_pending_filter_conditions_with_task_and_confidence():
    filters = TermAuditListFilters(
        task_name="admin-proj",
        confidence_min=0.8,
        confidence_max=1.0,
        retrieval_method="exact",
    )
    conditions = TermRepository._build_pending_filter_conditions(filters)
    assert len(conditions) == 5


@pytest.mark.unit
def test_term_audit_list_filters_rejects_invalid_confidence_range():
    with pytest.raises(ValueError, match="confidence_min"):
        TermAuditListFilters(confidence_min=0.9, confidence_max=0.5)
