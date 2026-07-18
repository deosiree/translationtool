-- 切分轨迹 segment_trace：Agent audit 真源 + 工作台 entry 持久列
-- 执行：mysql -u root -p translationtool < scripts/migrations/002_add_segment_trace.sql

ALTER TABLE term_agent_audit
  ADD COLUMN segment_trace JSON NULL COMMENT 'jieba/对齐切分轨迹 {jieba,aligned,display}'
  AFTER llm_reasoning;

ALTER TABLE t_entry_info
  ADD COLUMN segment_trace JSON NULL COMMENT 'jieba/对齐切分轨迹，供工作台列表出参'
  AFTER comment;
