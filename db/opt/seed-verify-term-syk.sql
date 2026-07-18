-- VERIFY/SYK + ADM 验数种子（通用平台部 / mon-cn 产品）
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ========== admin ==========
DELETE FROM t_user_role WHERE user_id = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';
DELETE FROM t_user_product WHERE user_id = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';
DELETE FROM t_user WHERE id = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495' OR user_name = 'admin';

INSERT INTO t_user (id, user_name, job_number, department)
VALUES ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', 'admin', NULL, '通用平台部');

INSERT INTO t_user_role (user_id, role_id) VALUES
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '1'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '2'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '3'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '4'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '5'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '6'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '7');

SET @product_id = '6d04f4c6-ed77-4dee-b2c3-dd6c2a37a9cd';
SET @classify_id = '6d04f4c6-ed77-4dee-b2c3-dd6c2a37a9cd';
SET @task_id = 'verify-syk-mon-cn-task';

INSERT INTO t_user_product (id, user_id, product_id, `read`, `write`)
VALUES ('verify-admin-up-001', 'd37d01e4-2df1-4681-b7bf-8a5f97f06495', @product_id, 1, 1);

DELETE FROM t_product_relation WHERE task_id = @task_id;
DELETE FROM t_task_info WHERE id = @task_id;
-- 人员四角色必须与 creator 一并填写（UI 任务流依赖）；本地验数统一填 admin
INSERT INTO t_task_info (
  id, creator, name, create_time, department, state, translate_type, is_delete, product_id,
  developer, entry_auditor, translator, translation_auditor
) VALUES (
  @task_id, 'admin', 'verify-syk-mon-cn', NOW(), '通用平台部', 1, '英文', 0, @product_id,
  'admin', 'admin', 'admin', 'admin'
);

-- 清理旧验数
DELETE r FROM t_product_relation r
  INNER JOIN t_entry_info e ON e.id = r.entry_id
  WHERE e.id LIKE 'verify-%' OR e.id LIKE 'adm-entry-%';
DELETE FROM t_entry_info WHERE id LIKE 'verify-%' OR id LIKE 'adm-entry-%';
DELETE FROM t_translate WHERE id LIKE 'verify-%' OR id LIKE 'adm-seed-%';

-- ========== SYK ==========
-- entry_state=3（词条审核通过）；=0 会触发 getTaskPending RuntimeException
INSERT INTO t_entry_info (id, entry, classify_id, product_id, task_id, en_trans_id, is_delete, comment, entry_state)
VALUES
  ('verify-entry-syk-exact', 'VERIFY/SYK-exact-用户登录', @classify_id, @product_id, @task_id, NULL, 0, '', 3),
  ('verify-entry-syk-miss', 'VERIFY/SYK-miss-全新句子XYZ', @classify_id, @product_id, @task_id, NULL, 0, '', 3),
  ('verify-entry-syk-exact2', 'VERIFY/SYK-exact-权限管理', @classify_id, @product_id, @task_id, NULL, 0, '', 3);

INSERT INTO t_translate (id, entry, translate, type, visual_range, translate_state, delete_state, public_state, last_use_time)
VALUES
  ('verify-syk-hit-exact', 'VERIFY/SYK-exact-用户登录', 'SYK-HIT-User Login', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('verify-syk-hit-exact2', 'VERIFY/SYK-exact-权限管理', 'SYK-HIT-Permission Mgmt', '英文', '通用平台部', '3', 0, 0, NOW());

INSERT INTO t_product_relation (id, entry_id, product_id, task_id) VALUES
  ('verify-pr-syk-1', 'verify-entry-syk-exact', @product_id, @task_id),
  ('verify-pr-syk-2', 'verify-entry-syk-miss', @product_id, @task_id),
  ('verify-pr-syk-3', 'verify-entry-syk-exact2', @product_id, @task_id);

-- ========== ADM ==========
INSERT INTO t_entry_info (id, entry, classify_id, product_id, task_id, en_trans_id, is_delete, comment, entry_state)
VALUES
  ('adm-entry-r01', 'ADM/R01-RAG精确', @classify_id, @product_id, @task_id, 'adm-seed-tr-r01', 0, '', 3),
  ('adm-entry-r04', 'ADM/R04-RAGGREP一致', @classify_id, @product_id, @task_id, 'adm-seed-tr-r04', 0, '', 3),
  ('adm-entry-s02', 'ADM/S02-RAG模糊-用户管理系统', @classify_id, @product_id, @task_id, NULL, 0, 'ADM-S02', 3),
  ('adm-entry-s03-trig', '文件、系统、资源', @classify_id, @product_id, @task_id, NULL, 0, '', 3),
  ('adm-entry-3b-trig', '文件与系统', @classify_id, @product_id, @task_id, NULL, 0, '', 3),
  ('adm-entry-t99', 'T99-全新未收录', @classify_id, @product_id, @task_id, NULL, 0, 'ADM-T99', 3),
  ('adm-entry-s03-file', 'ADM/S03-文件', @classify_id, @product_id, @task_id, 'adm-seed-tr-s03-file', 0, '', 3),
  ('adm-entry-s03-sys', 'ADM/S03-系统', @classify_id, @product_id, @task_id, 'adm-seed-tr-s03-sys', 0, '', 3),
  ('adm-entry-3b-file', 'ADM/3B-文件', @classify_id, @product_id, @task_id, 'adm-seed-tr-3b-file', 0, '', 3),
  ('adm-entry-3b-sys', 'ADM/3B-系统', @classify_id, @product_id, @task_id, 'adm-seed-tr-3b-sys', 0, '', 3);

INSERT INTO t_translate (id, entry, translate, type, visual_range, translate_state, delete_state, public_state, last_use_time)
VALUES
  ('adm-seed-tr-r01', 'ADM/R01-RAG精确', 'ADM R01 Exact Hit', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('adm-seed-tr-r04', 'ADM/R04-RAGGREP一致', 'ADM RAG Grep Same', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('adm-seed-tr-s03-file', 'ADM/S03-文件', 'File', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('adm-seed-tr-s03-sys', 'ADM/S03-系统', 'System', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('adm-seed-tr-3b-file', 'ADM/3B-文件', 'File', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('adm-seed-tr-3b-sys', 'ADM/3B-系统', 'System', '英文', '通用平台部', '3', 0, 0, NOW()),
  ('adm-seed-tr-s03-res', 'ADM/S03-资源', 'Resource', '英文', '通用平台部', '3', 0, 0, NOW());

INSERT INTO t_entry_info (id, entry, classify_id, product_id, task_id, en_trans_id, is_delete, comment, entry_state)
VALUES ('adm-entry-s03-res', 'ADM/S03-资源', @classify_id, @product_id, @task_id, 'adm-seed-tr-s03-res', 0, '', 3);

INSERT INTO t_product_relation (id, entry_id, product_id, task_id) VALUES
  ('verify-pr-adm-01', 'adm-entry-r01', @product_id, @task_id),
  ('verify-pr-adm-02', 'adm-entry-r04', @product_id, @task_id),
  ('verify-pr-adm-03', 'adm-entry-s02', @product_id, @task_id),
  ('verify-pr-adm-04', 'adm-entry-s03-trig', @product_id, @task_id),
  ('verify-pr-adm-05', 'adm-entry-3b-trig', @product_id, @task_id),
  ('verify-pr-adm-06', 'adm-entry-t99', @product_id, @task_id),
  ('verify-pr-adm-07', 'adm-entry-s03-file', @product_id, @task_id),
  ('verify-pr-adm-08', 'adm-entry-s03-sys', @product_id, @task_id),
  ('verify-pr-adm-09', 'adm-entry-3b-file', @product_id, @task_id),
  ('verify-pr-adm-10', 'adm-entry-3b-sys', @product_id, @task_id),
  ('verify-pr-adm-11', 'adm-entry-s03-res', @product_id, @task_id);

SET FOREIGN_KEY_CHECKS=1;
