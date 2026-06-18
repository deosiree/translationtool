-- 先清理旧数据（如果有之前乱码的残留）
DELETE FROM t_role_menu WHERE menu_id = '16';
DELETE FROM t_menu WHERE id = '16';

-- 重新插入菜单（注意 userLogin 接口返回 menu 使用的是 component 和 name 字段）
INSERT INTO t_menu (id, parent_id, menu_name, url, `rank`, icon, active_icon, `name`, component, is_delete)
VALUES ('16', '0', '术语学习', '/translate/terminologyAgent', 5, NULL, NULL, 'terminologyAgent', '/terminologyAgent/index', 0);

-- 授权给角色
INSERT INTO t_role_menu (id, role_id, menu_id) VALUES (104, '1', '16');
INSERT INTO t_role_menu (id, role_id, menu_id) VALUES (105, '2', '16');
