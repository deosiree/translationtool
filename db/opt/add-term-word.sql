-- =============================================================================
-- Agent 元词词典 term_word + term_word_conflict（Grep 线语料）
-- 执行（PowerShell 必须指定 UTF-8 编码）：
--   Get-Content db\opt\add-term-word.sql -Encoding utf8 | docker exec -i translation-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 translationtool
-- =============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS term_word (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    word VARCHAR(255) NOT NULL COMMENT '单词文本',
    comment VARCHAR(512) NOT NULL DEFAULT '' COMMENT '消歧 comment，仅来自 t_entry_info.comment',
    translate VARCHAR(1024) NOT NULL COMMENT '译法',
    target_lang VARCHAR(64) NOT NULL COMMENT '目标语种，如俄文',
    department VARCHAR(128) NULL COMMENT '部门可见范围，运行时过滤，非消歧键',
    source_translate_id VARCHAR(64) NOT NULL COMMENT '溯源 t_translate.id',
    source_entry_info_id VARCHAR(64) NULL COMMENT '溯源 t_entry_info.id',
    task_id VARCHAR(255) NULL COMMENT '溯源 t_entry_info.task_id',
    product_id VARCHAR(255) NULL COMMENT '溯源 t_entry_info.product_id',
    description TEXT NULL COMMENT '描述：使用场景、选型原因等，非消歧键',
    remark1 VARCHAR(512) NULL COMMENT '备注拓展槽 1',
    remark2 VARCHAR(512) NULL COMMENT '备注拓展槽 2',
    remark3 VARCHAR(512) NULL COMMENT '备注拓展槽 3',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'approved | pending | deprecated',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_term_word_source (
        word, comment, target_lang, source_translate_id, source_entry_info_id
    ),
    INDEX idx_term_word_lookup (word, target_lang, status),
    INDEX idx_term_word_task (task_id),
    INDEX idx_term_word_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent Grep 线元词词典';

CREATE TABLE IF NOT EXISTS term_word_conflict (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    word VARCHAR(255) NOT NULL COMMENT '矛盾单词',
    comment VARCHAR(512) NOT NULL DEFAULT '' COMMENT '消歧 scope',
    target_lang VARCHAR(64) NOT NULL COMMENT '目标语种',
    word_ids JSON NOT NULL COMMENT '冲突 term_word.id 列表',
    conflict_type VARCHAR(32) NOT NULL DEFAULT 'translate_mismatch' COMMENT '矛盾类型',
    resolution VARCHAR(32) NOT NULL DEFAULT 'open' COMMENT 'open | human_resolved | llm_suggested',
    task_ids JSON NULL COMMENT '涉及任务 id 列表',
    product_ids JSON NULL COMMENT '涉及产品 id 列表',
    source_entry_info_ids JSON NULL COMMENT '涉及 entry_info id 列表',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_term_word_conflict_open (resolution, target_lang),
    INDEX idx_term_word_conflict_word (word, target_lang)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元词译法矛盾工单';
