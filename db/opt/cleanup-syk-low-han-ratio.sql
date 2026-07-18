-- 本地术语库（SYK）精简：中文占比 < 80%
-- 表：t_translate（public_state=0 为术语库；软删 delete_state=1）
-- 前置：整库 backup（db-回滚数据库 / backup-database.ps1），非生产专用
-- 中文占比：entry 中汉字（一-龥）字符数 / entry 总字符数；空 entry 一并软删
-- 副作用：被引用的 *_trans_id 置 NULL（本地可接受；勿用于生产）

SET NAMES utf8mb4 COLLATE utf8mb4_bin;

DROP TABLE IF EXISTS tmp_syk_low_han_ids;
CREATE TABLE tmp_syk_low_han_ids (
  id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

INSERT INTO tmp_syk_low_han_ids (id)
SELECT tr.id
FROM t_translate tr
WHERE tr.delete_state = 0
  AND tr.public_state = 0
  AND (
    CHAR_LENGTH(IFNULL(tr.entry, '')) = 0
    OR (
      CHAR_LENGTH(IFNULL(tr.entry, '')) > 0
      AND (
        CHAR_LENGTH(REGEXP_REPLACE(IFNULL(tr.entry, ''), '[^一-龥]', ''))
        / CHAR_LENGTH(IFNULL(tr.entry, ''))
      ) < 0.8
    )
  );

-- dry-run 时可只跑到这里并：SELECT COUNT(*) FROM tmp_syk_low_han_ids;

UPDATE t_translate tr
INNER JOIN tmp_syk_low_han_ids x ON x.id = tr.id
SET tr.delete_state = 1
WHERE tr.delete_state = 0 AND tr.public_state = 0;

UPDATE t_entry_info e
INNER JOIN tmp_syk_low_han_ids x ON x.id = e.zh_trans_id
SET e.zh_trans_id = NULL
WHERE e.is_delete = 0;

UPDATE t_entry_info e
INNER JOIN tmp_syk_low_han_ids x ON x.id = e.en_trans_id
SET e.en_trans_id = NULL
WHERE e.is_delete = 0;

UPDATE t_entry_info e
INNER JOIN tmp_syk_low_han_ids x ON x.id = e.fra_trans_id
SET e.fra_trans_id = NULL
WHERE e.is_delete = 0;

UPDATE t_entry_info e
INNER JOIN tmp_syk_low_han_ids x ON x.id = e.spa_trans_id
SET e.spa_trans_id = NULL
WHERE e.is_delete = 0;

UPDATE t_entry_info e
INNER JOIN tmp_syk_low_han_ids x ON x.id = e.ru_trans_id
SET e.ru_trans_id = NULL
WHERE e.is_delete = 0;

-- 验收
-- SELECT COUNT(*) AS syk_alive FROM t_translate WHERE delete_state=0 AND public_state=0;
-- SELECT COUNT(*) FROM tmp_syk_low_han_ids;

DROP TABLE IF EXISTS tmp_syk_low_han_ids;
