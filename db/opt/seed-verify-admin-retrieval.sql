-- 产品 admin：多检索路径验数词条（exact / exact-grep / fuzzy|none / decomposed / none）
-- 对齐 terminology-agent/devtools/verify_adm_pretranslate.py CASES
-- 硬约束：五人员齐填；entry_state=3；en_trans_id NULL；写 t_product_relation
-- 应用：工作台验数播种 apply -SeedProfile custom -SeedSqlPath 本文件
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

SET @product_id = 'a2128cfc-14f2-46ab-930e-76350aaf0255';
SET @task_id = 'verify-admin-retrieval-task';
SET @task_name = 'verify-admin-retrieval';
SET @admin_uid = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';
SET @department = '通用平台部';
SET @pfx = 'verify-admin-idx';

DELETE FROM t_user_product WHERE product_id = @product_id AND user_id = @admin_uid;
INSERT INTO t_user_product (id, user_id, product_id, `read`, `write`)
VALUES (CONCAT(@pfx, '-up'), @admin_uid, @product_id, 1, 1);

DELETE FROM t_product_relation WHERE task_id = @task_id;
DELETE FROM t_task_info WHERE id = @task_id;
INSERT INTO t_task_info (
  id, creator, name, create_time, department, state, translate_type, is_delete, product_id,
  developer, entry_auditor, translator, translation_auditor
) VALUES (
  @task_id, 'admin', @task_name, NOW(), @department, 1, '英文', 0, @product_id,
  'admin', 'admin', 'admin', 'admin'
);

DELETE r FROM t_product_relation r
  INNER JOIN t_entry_info e ON e.id = r.entry_id
  WHERE e.id LIKE CONCAT(@pfx, '-%');
DELETE FROM t_entry_info WHERE id LIKE CONCAT(@pfx, '-%');

-- 6 条：与 verify_adm_pretranslate CASES 原文一致（待译，测预翻译多路径）
INSERT INTO t_entry_info (id, entry, classify_id, product_id, task_id, en_trans_id, is_delete, comment, entry_state)
VALUES
  (CONCAT(@pfx, '-r01'),  'ADM/R01-RAG精确',              @product_id, @product_id, @task_id, NULL, 0, '', 3),
  (CONCAT(@pfx, '-r04'),  'ADM/R04-RAGGREP一致',           @product_id, @product_id, @task_id, NULL, 0, '', 3),
  (CONCAT(@pfx, '-s02'),  'ADM/S02-RAG模糊-用户管理系统',   @product_id, @product_id, @task_id, NULL, 0, 'ADM-S02', 3),
  (CONCAT(@pfx, '-dec1'), '文件、系统、资源',               @product_id, @product_id, @task_id, NULL, 0, '', 3),
  (CONCAT(@pfx, '-dec2'), '文件与系统',                     @product_id, @product_id, @task_id, NULL, 0, '', 3),
  (CONCAT(@pfx, '-t99'),  'T99-全新未收录',                 @product_id, @product_id, @task_id, NULL, 0, 'ADM-T99', 3);

INSERT INTO t_product_relation (id, entry_id, product_id, task_id) VALUES
  (CONCAT(@pfx, '-pr-1'), CONCAT(@pfx, '-r01'),  @product_id, @task_id),
  (CONCAT(@pfx, '-pr-2'), CONCAT(@pfx, '-r04'),  @product_id, @task_id),
  (CONCAT(@pfx, '-pr-3'), CONCAT(@pfx, '-s02'),  @product_id, @task_id),
  (CONCAT(@pfx, '-pr-4'), CONCAT(@pfx, '-dec1'), @product_id, @task_id),
  (CONCAT(@pfx, '-pr-5'), CONCAT(@pfx, '-dec2'), @product_id, @task_id),
  (CONCAT(@pfx, '-pr-6'), CONCAT(@pfx, '-t99'),  @product_id, @task_id);

SET FOREIGN_KEY_CHECKS=1;

SELECT 'ok' AS status, @product_id AS product_id, @task_id AS task_id;
SELECT e.id, e.entry, e.entry_state, e.comment
FROM t_entry_info e
JOIN t_product_relation r ON r.entry_id = e.id
WHERE r.task_id = @task_id
ORDER BY e.id;
