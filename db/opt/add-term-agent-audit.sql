-- =============================================================================
-- 术语学习 Agent 审核表 term_agent_audit
-- 用途：工作台 Agent 预翻译 confidence < 阈值时，词条进入此表待人工确认
-- 执行：Get-Content db\opt\add-term-agent-audit.sql | docker exec -i translation-mysql mysql -uroot -p123456 translationtool
-- =============================================================================

CREATE TABLE IF NOT EXISTS term_agent_audit (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    source_text VARCHAR(1024) NOT NULL COMMENT '词条原文（对应工作台 entry 列）',
    context TEXT NULL COMMENT '可选上下文（旧版单条术语发现流程保留）',

    -- 旧版术语发现字段
    matched_term VARCHAR(1024) NULL COMMENT '术语库已有翻译',
    match_confidence FLOAT NULL COMMENT '匹配置信度',
    is_new_term TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为新术语',

    -- Agent 预翻译产出
    suggested_translation VARCHAR(1024) NULL COMMENT '建议译文（审核页必展示，即使工作台未回填）',
    llm_reasoning TEXT NULL COMMENT 'Agent 推理说明',

    -- 人工审核
    review_status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending | approved | rejected',
    review_comment VARCHAR(512) NULL COMMENT '审核人备注',
    error TEXT NULL COMMENT '工作流异常信息',

    -- 工作台上下文（与 PreTranslateModal / terminologyAgent 列表对齐）
    entry_info_id VARCHAR(64) NULL COMMENT '工作台词条 t_entry_info.id',
    task_id VARCHAR(64) NULL COMMENT '翻译任务 id',
    task_name VARCHAR(255) NULL COMMENT '任务名称',
    product_name VARCHAR(255) NULL COMMENT '产品名称',
    target_lang VARCHAR(64) NULL COMMENT '目标语种，如「俄文」',
    department VARCHAR(128) NULL COMMENT '部门所属，对应 t_translate.visual_range',
    confidence FLOAT NULL COMMENT '预翻译置信度 0~1',
    similar_terms JSON NULL COMMENT 'RAG 参考术语 [{entry, translate, score}]',
    retrieval_method VARCHAR(32) NULL COMMENT '检索策略：exact | fuzzy | hybrid',
    source_type VARCHAR(16) NOT NULL DEFAULT 'workbench_agent' COMMENT '记录来源',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_term_agent_audit_status (review_status),
    INDEX idx_term_agent_audit_task (task_id),
    INDEX idx_term_agent_audit_entry (entry_info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 术语学习/预翻译待审核记录';
