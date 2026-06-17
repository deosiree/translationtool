-- 清理重复的 admin 用户，只保留最新登录的那个
DELETE FROM t_user WHERE user_name = 'admin' AND id != 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';

-- 设置 admin 的部门为"通用平台部"
UPDATE t_user SET department = '通用平台部' WHERE user_name = 'admin';

-- 给 admin 分配角色：管理员 (role_id = 1)
-- 先删除已有角色关联
DELETE FROM t_user_role WHERE user_id = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';
-- 插入角色关联
INSERT INTO t_user_role (user_id, role_id) VALUES ('d37d01e4-2df1-4681-b7bf-8a5f97f06495', '1');
