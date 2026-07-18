-- usage_notes：确保为 TEXT（无 VARCHAR 长度上限），供「使用场景与注意事项」长文入库
-- 幂等：已是 TEXT 时仍可安全执行

SET NAMES utf8mb4;

ALTER TABLE term_word
  MODIFY COLUMN usage_notes TEXT NULL COMMENT '使用场景与注意事项（TEXT，不限业务字符长度）';
