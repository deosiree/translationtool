-- =============================================================================
-- 修复 term_agent_audit 中文 COMMENT 乱码
-- 执行：
--   Get-Content db\opt\fix-agent-table-comments-utf8.sql -Encoding utf8 | docker exec -i translation-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 translationtool
-- =============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

ALTER TABLE term_agent_audit COMMENT='Agent 术语学习/预翻译待审核记录';
ALTER TABLE term_agent_audit
    MODIFY COLUMN source_text VARCHAR(1024) NOT NULL COMMENT '词条原文（对应工作台 entry 列）',
    MODIFY COLUMN context TEXT NULL COMMENT '可选上下文（旧版单条术语发现流程保留）',
    MODIFY COLUMN matched_term VARCHAR(1024) NULL COMMENT '术语库已有翻译',
    MODIFY COLUMN match_confidence FLOAT NULL COMMENT '匹配置信度',
    MODIFY COLUMN is_new_term TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为新术语',
    MODIFY COLUMN suggested_translation VARCHAR(1024) NULL COMMENT '建议译文（审核页必展示，即使工作台未回填）',
    MODIFY COLUMN llm_reasoning TEXT NULL COMMENT 'Agent 推理说明',
    MODIFY COLUMN review_status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending | approved | rejected',
    MODIFY COLUMN review_comment VARCHAR(512) NULL COMMENT '审核人备注',
    MODIFY COLUMN error TEXT NULL COMMENT '工作流异常信息',
    MODIFY COLUMN entry_info_id VARCHAR(64) NULL COMMENT '工作台词条 t_entry_info.id',
    MODIFY COLUMN task_id VARCHAR(64) NULL COMMENT '翻译任务 id',
    MODIFY COLUMN task_name VARCHAR(255) NULL COMMENT '任务名称',
    MODIFY COLUMN product_name VARCHAR(255) NULL COMMENT '产品名称',
    MODIFY COLUMN target_lang VARCHAR(64) NULL COMMENT '目标语种，如「俄文」',
    MODIFY COLUMN department VARCHAR(128) NULL COMMENT '部门所属，对应 t_translate.visual_range',
    MODIFY COLUMN confidence FLOAT NULL COMMENT '预翻译置信度 0~1',
    MODIFY COLUMN similar_terms JSON NULL COMMENT 'RAG 参考术语 [{entry, translate, score}]',
    MODIFY COLUMN retrieval_method VARCHAR(32) NULL COMMENT '检索策略：exact | fuzzy | hybrid',
    MODIFY COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'workbench_agent' COMMENT '记录来源';
