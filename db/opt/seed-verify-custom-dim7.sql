-- 工作台验数播种 · custom 种子模板（复制后改变量与矩阵行）
-- 用法：
--   1) 复制为本文件副本到 ProjectRoot/db/opt/seed-verify-custom-<slug>.sql
--      或：scripts/new-custom-seed.ps1 -Slug <slug> -ProductId ... -TaskId ...
--   2) 改 SET @* 与 INSERT 矩阵（至少 1 命中 + 1 miss）
--   3) apply-workbench-verify-seed.ps1 -SeedProfile custom -SeedSqlPath <该文件>
-- 硬约束：五人员齐填；entry_state=3；目标语种 *_trans_id=NULL；须写 t_product_relation
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ========== 必改变量 ==========
SET @product_id = 'a2128cfc-14f2-46ab-930e-76350aaf0255';
SET @task_id = 'verify-custom-dim7-task';
SET @task_name = 'verify-custom-dim7';
SET @department = '通用平台部';
SET @translate_type = '英文';
SET @admin_uid = 'd37d01e4-2df1-4681-b7bf-8a5f97f06495';          -- t_user.id（本地 admin）
SET @personnel = 'admin';                 -- 五角色同名时可全用此值
SET @id_prefix = 'verify-custom-dim7';         -- 词条/relation/translate id 前缀，勿与他档案冲突

-- 绑定用户可见产品
DELETE FROM t_user_product WHERE product_id = @product_id AND user_id = @admin_uid;
INSERT INTO t_user_product (id, user_id, product_id, `read`, `write`)
VALUES (CONCAT(@id_prefix, '-up'), @admin_uid, @product_id, 1, 1);

-- 任务（五人齐填）
DELETE FROM t_product_relation WHERE task_id = @task_id;
DELETE FROM t_task_info WHERE id = @task_id;
INSERT INTO t_task_info (
  id, creator, name, create_time, department, state, translate_type, is_delete, product_id,
  developer, entry_auditor, translator, translation_auditor
) VALUES (
  @task_id, @personnel, @task_name, NOW(), @department, 1, @translate_type, 0, @product_id,
  @personnel, @personnel, @personnel, @personnel
);

-- 清理同前缀旧种子
DELETE r FROM t_product_relation r
  INNER JOIN t_entry_info e ON e.id = r.entry_id
  WHERE e.id LIKE CONCAT(@id_prefix, '-%');
DELETE FROM t_entry_info WHERE id LIKE CONCAT(@id_prefix, '-%');
DELETE FROM t_translate WHERE id LIKE CONCAT(@id_prefix, '-hit-%');

-- ========== 矩阵：改原文/译文；保持 entry_state=3、en_trans_id NULL ==========
-- 默认 2 命中 + 1 miss（可增删，同步改 relation）
INSERT INTO t_entry_info (id, entry, classify_id, product_id, task_id, en_trans_id, is_delete, comment, entry_state)
VALUES
  (CONCAT(@id_prefix, '-exact1'), 'VERIFY/CUSTOM-exact-示例甲', @product_id, @product_id, @task_id, NULL, 0, '', 3),
  (CONCAT(@id_prefix, '-exact2'), 'VERIFY/CUSTOM-exact-示例乙', @product_id, @product_id, @task_id, NULL, 0, '', 3),
  (CONCAT(@id_prefix, '-miss1'),  'VERIFY/CUSTOM-miss-未收录句', @product_id, @product_id, @task_id, NULL, 0, '', 3);

INSERT INTO t_translate (id, entry, translate, type, visual_range, translate_state, delete_state, public_state, last_use_time)
VALUES
  (CONCAT(@id_prefix, '-hit-1'), 'VERIFY/CUSTOM-exact-示例甲', 'CUSTOM-HIT-Example A', @translate_type, @department, '3', 0, 0, NOW()),
  (CONCAT(@id_prefix, '-hit-2'), 'VERIFY/CUSTOM-exact-示例乙', 'CUSTOM-HIT-Example B', @translate_type, @department, '3', 0, 0, NOW());

INSERT INTO t_product_relation (id, entry_id, product_id, task_id) VALUES
  (CONCAT(@id_prefix, '-pr-1'), CONCAT(@id_prefix, '-exact1'), @product_id, @task_id),
  (CONCAT(@id_prefix, '-pr-2'), CONCAT(@id_prefix, '-exact2'), @product_id, @task_id),
  (CONCAT(@id_prefix, '-pr-3'), CONCAT(@id_prefix, '-miss1'),  @product_id, @task_id);

SET FOREIGN_KEY_CHECKS=1;

SELECT 'ok' AS status, @product_id AS product_id, @task_id AS task_id;
SELECT COUNT(*) AS entry_cnt FROM t_product_relation WHERE task_id = @task_id;
