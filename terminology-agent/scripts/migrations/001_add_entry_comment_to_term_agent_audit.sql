-- term_agent_audit 增加 entry_comment（工作台词条 comment / Grep 消歧键）
-- 执行：mysql -u root -p translationtool < scripts/migrations/001_add_entry_comment_to_term_agent_audit.sql

ALTER TABLE term_agent_audit
  ADD COLUMN entry_comment VARCHAR(512) NULL COMMENT '工作台词条 comment，Grep 消歧键'
  AFTER source_text;
