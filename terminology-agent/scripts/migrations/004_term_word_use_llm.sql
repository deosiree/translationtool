-- term_word.use_llm：走 LLM 判断（非直译；含指代/词性/分场景等）
-- 默认 0；Agent 命中时若为 1 不得纯词表直换。

ALTER TABLE term_word
  ADD COLUMN use_llm TINYINT(1) NOT NULL DEFAULT 0
  COMMENT '走LLM：1=需LLM判断非直译'
  AFTER remark3;
