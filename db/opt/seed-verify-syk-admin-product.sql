-- 向用户创建的产品 admin 灌 SYK 术语库翻译验数词条
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

SET @product_id = 'a2128cfc-14f2-46ab-930e-76350aaf0255';
SET @task_id = 'verify-syk-admin-task';
SET @admin_uid = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';

-- 绑定用户
DELETE FROM t_user_product WHERE product_id = @product_id AND user_id = @admin_uid;
INSERT INTO t_user_product (id, user_id, product_id, `read`, `write`)
VALUES ('verify-admin-up-admin-prod', @admin_uid, @product_id, 1, 1);

-- 任务（五人齐填）
DELETE FROM t_product_relation WHERE task_id = @task_id;
DELETE FROM t_task_info WHERE id = @task_id;
INSERT INTO t_task_info (
  id, creator, name, create_time, department, state, translate_type, is_delete, product_id,
  developer, entry_auditor, translator, translation_auditor
) VALUES (
  @task_id, 'admin', 'verify-syk-admin', NOW(), '通用平台部', 1, '英文', 0, @product_id,
  'admin', 'admin', 'admin', 'admin'
);

-- 清理旧种子
DELETE r FROM t_product_relation r
  INNER JOIN t_entry_info e ON e.id = r.entry_id
  WHERE e.id LIKE 'verify-admin-%';
DELETE FROM t_entry_info WHERE id LIKE 'verify-admin-%';
DELETE FROM t_translate WHERE id LIKE 'verify-admin-syk-%';

-- entry_state=3（词条审核通过）才能进翻译阶段；=0 会令 getTaskPending 抛「新建词条」异常
INSERT INTO t_entry_info (id, entry, classify_id, product_id, task_id, en_trans_id, is_delete, comment, entry_state)
VALUES
  ('verify-admin-syk-exact', 'VERIFY/SYK-exact-用户登录', @product_id, @product_id, @task_id, NULL, 0, '', 3),
  ('verify-admin-syk-exact2', 'VERIFY/SYK-exact-权限管理', @product_id, @product_id, @task_id, NULL, 0, '', 3),
  ('verify-admin-syk-miss', 'VERIFY/SYK-miss-全新句子XYZ', @product_id, @product_id, @task_id, NULL, 0, '', 3),
  ('verify-admin-syk-exact3', 'VERIFY/SYK-exact-数据备份', @product_id, @product_id, @task_id, NULL, 0, '', 3);

INSERT INTO t_translate (id, entry, translate, type, visual_range, translate_state, delete_state, public_state, last_use_time)
VALUES
  ('verify-admin-syk-hit-1', 'VERIFY/SYK-exact-用户登录', 'SYK-HIT-User Login', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('verify-admin-syk-hit-2', 'VERIFY/SYK-exact-权限管理', 'SYK-HIT-Permission Mgmt', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('verify-admin-syk-hit-3', 'VERIFY/SYK-exact-数据备份', 'SYK-HIT-Data Backup', '英文', '通用平台部', '3', 0, 0, NOW());

INSERT INTO t_product_relation (id, entry_id, product_id, task_id) VALUES
  ('verify-admin-pr-1', 'verify-admin-syk-exact', @product_id, @task_id),
  ('verify-admin-pr-2', 'verify-admin-syk-exact2', @product_id, @task_id),
  ('verify-admin-pr-3', 'verify-admin-syk-miss', @product_id, @task_id),
  ('verify-admin-pr-4', 'verify-admin-syk-exact3', @product_id, @task_id);

SET FOREIGN_KEY_CHECKS=1;

SELECT 'ok' AS status, @product_id AS product_id, @task_id AS task_id;
SELECT COUNT(*) AS entry_cnt FROM t_product_relation WHERE task_id = @task_id;
