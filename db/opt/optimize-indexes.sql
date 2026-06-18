-- 词条表：常用过滤条件索引
CREATE INDEX idx_ei_delete_state ON t_entry_info(is_delete, entry_state, id) COMMENT '覆盖 is_delete=0 + entry_state IN 过滤';
CREATE INDEX idx_ei_classfy1 ON t_entry_info(classfy1) COMMENT '一级分类过滤';
CREATE INDEX idx_ei_classfy2 ON t_entry_info(classfy2) COMMENT '二级分类过滤';
CREATE INDEX idx_ei_entry ON t_entry_info(entry(100)) COMMENT '词条精确/前缀匹配';

-- 翻译表：JOIN 时加速
CREATE INDEX idx_trans_id_state ON t_translate(id, translate_state, type, delete_state) COMMENT '覆盖翻译查询 JOIN + 状态过滤';

-- 任务表
CREATE INDEX idx_task_department ON t_task_info(department) COMMENT '部门过滤任务';
