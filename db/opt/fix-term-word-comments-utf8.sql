-- =============================================================================
-- 修复 term_word / term_word_conflict 中文 COMMENT 乱码（需先建表）
-- 执行：
--   Get-Content db\opt\fix-term-word-comments-utf8.sql -Encoding utf8 | docker exec -i translation-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 translationtool
-- =============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

ALTER TABLE term_word COMMENT='Agent Grep 线元词词典';
ALTER TABLE term_word
    MODIFY COLUMN word VARCHAR(255) NOT NULL COMMENT '单词文本',
    MODIFY COLUMN comment VARCHAR(512) NOT NULL DEFAULT '' COMMENT '消歧 comment，仅来自 t_entry_info.comment',
    MODIFY COLUMN translate VARCHAR(1024) NOT NULL COMMENT '译法',
    MODIFY COLUMN target_lang VARCHAR(64) NOT NULL COMMENT '目标语种，如俄文',
    MODIFY COLUMN department VARCHAR(128) NULL COMMENT '部门可见范围，运行时过滤，非消歧键',
    MODIFY COLUMN source_translate_id VARCHAR(64) NOT NULL COMMENT '溯源 t_translate.id',
    MODIFY COLUMN source_entry_info_id VARCHAR(64) NULL COMMENT '溯源 t_entry_info.id',
    MODIFY COLUMN task_id VARCHAR(255) NULL COMMENT '溯源 t_entry_info.task_id',
    MODIFY COLUMN product_id VARCHAR(255) NULL COMMENT '溯源 t_entry_info.product_id',
    MODIFY COLUMN description TEXT NULL COMMENT '描述：使用场景、选型原因等，非消歧键',
    MODIFY COLUMN remark1 VARCHAR(512) NULL COMMENT '备注拓展槽 1',
    MODIFY COLUMN remark2 VARCHAR(512) NULL COMMENT '备注拓展槽 2',
    MODIFY COLUMN remark3 VARCHAR(512) NULL COMMENT '备注拓展槽 3',
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'approved | pending | deprecated';

ALTER TABLE term_word_conflict COMMENT='元词译法矛盾工单';
ALTER TABLE term_word_conflict
    MODIFY COLUMN word VARCHAR(255) NOT NULL COMMENT '矛盾单词',
    MODIFY COLUMN comment VARCHAR(512) NOT NULL DEFAULT '' COMMENT '消歧 scope',
    MODIFY COLUMN target_lang VARCHAR(64) NOT NULL COMMENT '目标语种',
    MODIFY COLUMN word_ids JSON NOT NULL COMMENT '冲突 term_word.id 列表',
    MODIFY COLUMN conflict_type VARCHAR(32) NOT NULL DEFAULT 'translate_mismatch' COMMENT '矛盾类型',
    MODIFY COLUMN resolution VARCHAR(32) NOT NULL DEFAULT 'open' COMMENT 'open | human_resolved | llm_suggested',
    MODIFY COLUMN task_ids JSON NULL COMMENT '涉及任务 id 列表',
    MODIFY COLUMN product_ids JSON NULL COMMENT '涉及产品 id 列表',
    MODIFY COLUMN source_entry_info_ids JSON NULL COMMENT '涉及 entry_info id 列表';
