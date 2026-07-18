-- 本地术语库（SYK）精简：空挂 + 完全重复（2026-07-18 实跑沉淀）
-- 表：t_translate public_state=0；软删 delete_state=1（勿硬 DELETE）
-- 前置：整库 backup；仅本地 / 非上线库
--
-- 空挂：未被 t_entry_info 任一语种 *_trans_id 引用
-- 完全重复：同 entry + type + visual_range + translate；保留 MIN(id)，其余软删；
--           被删 id 若仍被词条引用则改挂到 keep_id
--
-- 用法（容器内）：
--   mysql ... translationtool < cleanup-syk-unused-and-dup.sql
-- 建议先手工跑「统计」段，确认条数后再跑「执行」段。

SET NAMES utf8mb4 COLLATE utf8mb4_bin;

-- ========== 统计（dry-run）==========
-- 活跃术语库
-- SELECT COUNT(*) AS syk_alive FROM t_translate WHERE delete_state=0 AND public_state=0;

-- ========== 执行：空挂 ==========
DROP TABLE IF EXISTS tmp_used_trans_ids;
CREATE TABLE tmp_used_trans_ids (
  id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

INSERT IGNORE INTO tmp_used_trans_ids (id)
SELECT zh_trans_id FROM t_entry_info WHERE is_delete=0 AND zh_trans_id IS NOT NULL AND zh_trans_id<>'';
INSERT IGNORE INTO tmp_used_trans_ids (id)
SELECT en_trans_id FROM t_entry_info WHERE is_delete=0 AND en_trans_id IS NOT NULL AND en_trans_id<>'';
INSERT IGNORE INTO tmp_used_trans_ids (id)
SELECT fra_trans_id FROM t_entry_info WHERE is_delete=0 AND fra_trans_id IS NOT NULL AND fra_trans_id<>'';
INSERT IGNORE INTO tmp_used_trans_ids (id)
SELECT spa_trans_id FROM t_entry_info WHERE is_delete=0 AND spa_trans_id IS NOT NULL AND spa_trans_id<>'';
INSERT IGNORE INTO tmp_used_trans_ids (id)
SELECT ru_trans_id FROM t_entry_info WHERE is_delete=0 AND ru_trans_id IS NOT NULL AND ru_trans_id<>'';

UPDATE t_translate tr
LEFT JOIN tmp_used_trans_ids u ON u.id = tr.id
SET tr.delete_state = 1
WHERE tr.delete_state = 0
  AND tr.public_state = 0
  AND u.id IS NULL;

-- ========== 执行：完全重复 ==========
DROP TABLE IF EXISTS tmp_syk_dup_map;
CREATE TABLE tmp_syk_dup_map (
  drop_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
  keep_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

INSERT INTO tmp_syk_dup_map (drop_id, keep_id)
SELECT tr.id AS drop_id, k.keep_id
FROM t_translate tr
INNER JOIN (
  SELECT entry, type, visual_range, translate, MIN(id) AS keep_id
  FROM t_translate
  WHERE delete_state = 0 AND public_state = 0
  GROUP BY entry, type, visual_range, translate
  HAVING COUNT(*) > 1
) k ON tr.entry <=> k.entry
   AND tr.type <=> k.type
   AND tr.visual_range <=> k.visual_range
   AND tr.translate <=> k.translate
WHERE tr.delete_state = 0
  AND tr.public_state = 0
  AND tr.id <> k.keep_id;

UPDATE t_entry_info e
INNER JOIN tmp_syk_dup_map m ON m.drop_id = e.zh_trans_id
SET e.zh_trans_id = m.keep_id;

UPDATE t_entry_info e
INNER JOIN tmp_syk_dup_map m ON m.drop_id = e.en_trans_id
SET e.en_trans_id = m.keep_id;

UPDATE t_entry_info e
INNER JOIN tmp_syk_dup_map m ON m.drop_id = e.fra_trans_id
SET e.fra_trans_id = m.keep_id;

UPDATE t_entry_info e
INNER JOIN tmp_syk_dup_map m ON m.drop_id = e.spa_trans_id
SET e.spa_trans_id = m.keep_id;

UPDATE t_entry_info e
INNER JOIN tmp_syk_dup_map m ON m.drop_id = e.ru_trans_id
SET e.ru_trans_id = m.keep_id;

UPDATE t_translate tr
INNER JOIN tmp_syk_dup_map m ON m.drop_id = tr.id
SET tr.delete_state = 1;

DROP TABLE IF EXISTS tmp_syk_dup_map;
DROP TABLE IF EXISTS tmp_used_trans_ids;
