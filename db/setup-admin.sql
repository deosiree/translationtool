-- 清理重复的 admin 用户，只保留固定 id
DELETE FROM t_user WHERE user_name = 'admin' AND id != 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';

-- 设置 admin 的部门为"通用平台部"
UPDATE t_user SET department = '通用平台部' WHERE user_name = 'admin';

-- 若 admin 不存在则插入
INSERT INTO t_user (id, user_name, job_number, department)
SELECT 'd37d01e4-2df1-4681-b7bf-8a5f97f06495', 'admin', NULL, '通用平台部'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE user_name = 'admin');

-- 重置并写入全部 7 个角色
DELETE FROM t_user_role WHERE user_id = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';
INSERT INTO t_user_role (user_id, role_id) VALUES
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '1'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '2'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '3'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '4'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '5'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '6'),
  ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '7');
