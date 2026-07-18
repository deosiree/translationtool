-- term_word 命名字段：领域 / 缩写 / 使用场景与注意事项（替代 remark1/2/description 语义）

ALTER TABLE term_word
  ADD COLUMN category VARCHAR(128) NULL COMMENT '领域' AFTER use_llm,
  ADD COLUMN abbr VARCHAR(128) NULL COMMENT '缩写' AFTER category,
  ADD COLUMN usage_notes TEXT NULL COMMENT '使用场景与注意事项' AFTER abbr;

UPDATE term_word
SET
  category = NULLIF(TRIM(remark1), ''),
  abbr = NULLIF(TRIM(remark2), ''),
  usage_notes = NULLIF(TRIM(description), '')
WHERE
  (remark1 IS NOT NULL AND TRIM(remark1) <> '')
  OR (remark2 IS NOT NULL AND TRIM(remark2) <> '')
  OR (description IS NOT NULL AND TRIM(description) <> '');
