-- 本地验数：固定 admin（通用平台部）+ 全角色
-- 完整验数请用 db/opt/seed-verify-term-syk.sql
SET NAMES utf8mb4;

DELETE FROM t_user_role WHERE user_id = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';
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
