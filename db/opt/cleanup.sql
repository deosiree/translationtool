-- 1. 先删 t_product_relation 中对应的关联
DELETE pr FROM t_product_relation pr
INNER JOIN t_entry_info ei ON pr.entry_id = ei.id
WHERE ei.is_delete = 1 OR ei.entry_state = -1;

-- 2. 收集要删除的 translate ID（被删条目引用的翻译）
-- 注意：有些 translate 可能被多个条目共享，先标记再删
DROP TEMPORARY TABLE IF EXISTS tmp_del_trans;
CREATE TEMPORARY TABLE tmp_del_trans AS
SELECT DISTINCT t.id FROM t_translate t
LEFT JOIN t_entry_info ei ON t.id IN (ei.en_trans_id, ei.ru_trans_id, ei.zh_trans_id, ei.fra_trans_id, ei.spa_trans_id)
  AND ei.is_delete = 0 AND ei.entry_state != -1
WHERE ei.id IS NULL;

-- 3. 删 t_translate
DELETE FROM t_translate WHERE id IN (SELECT id FROM tmp_del_trans);

-- 4. 删 t_entry_info（已删除+禁用）
DELETE FROM t_entry_info WHERE is_delete = 1 OR entry_state = -1;

-- 5. 加索引（之前超时，现在数据少了应该快了）
CREATE INDEX idx_ei_delete_state ON t_entry_info(is_delete, entry_state, id) COMMENT '过滤 is_delete=0 + entry_state IN';
CREATE INDEX idx_ei_classfy1 ON t_entry_info(classfy1) COMMENT '一级分类过滤';
CREATE INDEX idx_ei_classfy2 ON t_entry_info(classfy2) COMMENT '二级分类过滤';
CREATE INDEX idx_ei_entry ON t_entry_info(entry(100)) COMMENT '词条精确/前缀匹配';
CREATE INDEX idx_trans_id_state ON t_translate(id, translate_state, type, delete_state) COMMENT '翻译查询 JOIN';
CREATE INDEX idx_task_department ON t_task_info(department) COMMENT '部门过滤';
