-- comment_rule.related_id：1:1 对应关系（双向互指）

SET NAMES utf8mb4;

ALTER TABLE comment_rule
  ADD COLUMN related_id VARCHAR(64) NULL COMMENT '对应 comment_rule.id' AFTER case_type,
  ADD KEY idx_related_id (related_id);

-- 成对互指：Time <-> Abbr
UPDATE comment_rule a
  INNER JOIN comment_rule b ON a.comment_key = 'Time' AND b.comment_key = 'Abbr'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

UPDATE comment_rule a
  INNER JOIN comment_rule b ON a.comment_key = 'Abbr' AND b.comment_key = 'Time'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

-- Date <-> DayOfWeek（rule_text：时间天数的「日」 vs 星期几的「日」）
UPDATE comment_rule a
  INNER JOIN comment_rule b
    ON a.comment_key = 'Date'
   AND b.comment_key = 'DayOfWeek'
   AND IFNULL(b.rule_text, '') LIKE '%星期几的%日%'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

UPDATE comment_rule a
  INNER JOIN comment_rule b
    ON a.comment_key = 'DayOfWeek'
   AND b.comment_key = 'Date'
   AND IFNULL(a.rule_text, '') LIKE '%星期几的%日%'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

-- Number <-> DayOfWeek（rule_text：数值 vs 星期几的一二三四五六）
UPDATE comment_rule a
  INNER JOIN comment_rule b
    ON a.comment_key = 'Number'
   AND b.comment_key = 'DayOfWeek'
   AND IFNULL(b.rule_text, '') LIKE '%星期几的%一二三四五六%'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

UPDATE comment_rule a
  INNER JOIN comment_rule b
    ON a.comment_key = 'DayOfWeek'
   AND b.comment_key = 'Number'
   AND IFNULL(a.rule_text, '') LIKE '%星期几的%一二三四五六%'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

-- Boolean <-> UI
UPDATE comment_rule a
  INNER JOIN comment_rule b ON a.comment_key = 'Boolean' AND b.comment_key = 'UI'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

UPDATE comment_rule a
  INNER JOIN comment_rule b ON a.comment_key = 'UI' AND b.comment_key = 'Boolean'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

-- Upper <-> Lower
UPDATE comment_rule a
  INNER JOIN comment_rule b ON a.comment_key = 'Upper' AND b.comment_key = 'Lower'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

UPDATE comment_rule a
  INNER JOIN comment_rule b ON a.comment_key = 'Lower' AND b.comment_key = 'Upper'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

-- internal <-> external
UPDATE comment_rule a
  INNER JOIN comment_rule b ON a.comment_key = 'internal' AND b.comment_key = 'external'
SET a.related_id = b.id
WHERE a.related_id IS NULL;

UPDATE comment_rule a
  INNER JOIN comment_rule b ON a.comment_key = 'external' AND b.comment_key = 'internal'
SET a.related_id = b.id
WHERE a.related_id IS NULL;
