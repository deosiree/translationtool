/**
 * Terminology Learning Agent API — 前端与 Python Agent 服务通信模块
 *
 * 开发代理：见 references/本地开发.md「附录：dev proxy 四档矩阵」
 *   - pnpm serve / dev / dev:dockerJava / dev:dockerPy → translation/dev/devProxyConfig.js
 * 生产：nginx 反向代理 /agent/* → terminology-agent:18002
 *
 * 响应格式（与 Java 后端一致）：
 *   { code: 200, message: "success", data: {...} }
 *
 * @module terminologyAgent
 */

import request, { requestBinary, requestMultipart } from "../request";

/**
 * RAG 检索命中的参考术语
 * @typedef {Object} SimilarTerm
 * @property {string} entry - 参考词条原文
 * @property {string} translate - 参考词条已有译文
 * @property {number} [score] - 相似度 0~1
 * @property {string} [retrieval_source] - rag | grep | rag+grep
 */

/**
 * 单条词条 Agent 预翻译元数据（嵌在 list 项的 agent_meta 字段）
 * @typedef {Object} AgentMeta
 * @property {number} confidence - 置信度 0~1
 * @property {"auto_approved"|"needs_human"} review_status - 分流结果
 * @property {string} [suggested_translation] - 建议译文
 * @property {SimilarTerm[]} similar_terms - 参考术语
 * @property {string} retrieval_method - exact | fuzzy | grep | hybrid | decomposed | none | mock_hybrid
 * @property {string} reasoning - Agent 说明
 * @property {Object} [segment_trace] - jieba/对齐切分轨迹 {jieba,aligned,display}
 */

/**
 * 术语学习待审核列表单行（GET /agent/term-learning/list）
 * @typedef {Object} AuditRecord
 * @property {string} id - audit 主键
 * @property {string} source_text - 词条原文
 * @property {string} [entry_comment] - 工作台词条 comment / Grep 消歧
 * @property {string} [suggested_translation] - 建议译文（低于阈值时也展示）
 * @property {string} [target_lang] - 目标语种
 * @property {string} [task_id] - 翻译任务 id
 * @property {string} [task_name] - 任务名称
 * @property {string} [product_name] - 产品名称
 * @property {string} [department] - 部门所属
 * @property {string} [entry_info_id] - 工作台词条 id
 * @property {number} [confidence] - 置信度（仅审核页展示）
 * @property {SimilarTerm[]} [similar_terms] - 参考术语
 * @property {string} [retrieval_method] - 检索方式
 * @property {string} [llm_reasoning] - Agent 说明
 * @property {Object} [segment_trace] - jieba/对齐切分轨迹
 * @property {"pending"|"approved"|"rejected"|"auto_approved"} review_status
 * @property {string} created_at
 * @property {string} updated_at
 */

/**
 * 批量预翻译响应 data
 * @typedef {Object} PreTranslateBatchResult
 * @property {Array<Object & { agent_meta?: AgentMeta }>} list - 预翻译结果列表
 * @property {number} auto_count - 高于阈值、自动回填数量
 * @property {number} pending_count - 低于阈值、进入待审核数量
 */

/**
 * 获取单条 audit 详情
 * @param {string} auditId
 * @returns {Promise<{ data: AuditRecord }>}
 */
export function getAuditRecord(auditId) {
  return request({
    url: `/agent/term-learning/${auditId}`,
    method: "GET",
  });
}

/**
 * 术语学习页 — 待审核列表
 * @param {Object} params - page, pageSize 及可选筛选字段
 * @param {string} [params.sourceText]
 * @param {string} [params.targetLang]
 * @param {string} [params.taskName]
 * @param {string} [params.productName]
 * @param {string} [params.department]
 * @param {number} [params.confidenceMin] - 0~1
 * @param {number} [params.confidenceMax] - 0~1
 * @param {string} [params.retrievalMethod]
 * @returns {Promise<{ data: { list: AuditRecord[], total: number } }>}
 */
export function listPendingAudits(params) {
  return request({
    url: "/agent/term-learning/list",
    method: "GET",
    params,
  });
}

/**
 * 术语库「术语字典」Tab — term_word 分页列表（切分命中依据）
 * @param {Object} params
 * @param {number} [params.page]
 * @param {number} [params.pageSize]
 * @param {string} [params.word]
 * @param {string} [params.translate]
 * @param {string} [params.targetLang]
 * @param {string} [params.department]
 * @param {string} [params.status] - 0|1|2|3；空=全部
 * @param {boolean} [params.hasAbbr] - 带缩写
 * @param {boolean} [params.useLlm] - 走LLM
 * @param {string} [params.wordRegex] - 词片 REGEXP
 * @param {string} [params.translateRegex] - 翻译 REGEXP
 * @returns {Promise<{ data: { list: Array, total: number } }>}
 */
export function listTermWords(params) {
  return request({
    url: "/agent/word/list",
    method: "GET",
    params,
  });
}

/**
 * 新建 term_word
 * @param {Object} data
 * @param {string} data.word
 * @param {string} data.translate
 * @param {string} data.target_lang
 * @param {string} [data.department]
 * @param {string} [data.comment]
 * @param {string} [data.status] - 0|1|2|3，默认 1
 */
export function createTermWord(data) {
  return request({
    url: "/agent/word",
    method: "POST",
    data,
  });
}

/**
 * 更新 term_word
 * @param {string} id
 * @param {Object} data
 */
export function updateTermWord(id, data) {
  return request({
    url: `/agent/word/${id}`,
    method: "PUT",
    data,
  });
}

/**
 * 删除单条 term_word
 * @param {string} id
 */
export function deleteTermWord(id) {
  return request({
    url: `/agent/word/${id}`,
    method: "DELETE",
  });
}

/**
 * 批量删除 term_word
 * @param {string[]} ids
 */
export function batchDeleteTermWords(ids) {
  return request({
    url: "/agent/word/batch-delete",
    method: "POST",
    data: { ids },
  });
}

/**
 * 批量审阅 term_word（通过→已审核 / 驳回→审核不通过）
 * 仅处理当前为待审核(1)的行
 * @param {string[]} ids
 * @param {"approved"|"rejected"} action
 * @returns {Promise<{ data: { updated: number, skipped: number, missing: number, status: string } }>}
 */
export function batchReviewTermWords(ids, action) {
  return request({
    url: "/agent/word/batch-review",
    method: "POST",
    data: { ids, action },
  });
}

/**
 * 下载术语字典导入模板
 * @param {boolean} [withSample=true] - 是否带样例行
 * @returns {Promise<import('axios').AxiosResponse>}
 */
export function downloadTermWordTemplate(withSample = true) {
  return requestBinary({
    url: "/agent/word/import-template",
    method: "GET",
    params: { withSample: withSample ? 1 : 0 },
    responseType: "blob",
  });
}

/**
 * 导入术语字典 Excel
 * @param {File} file
 * @param {{ forcePendingWhenTranslated?: boolean }} [options]
 * @returns {Promise<{ data: { created: number, skipped: number, parseErrors: string[], skipDetails: string[] } }>}
 */
export function importTermWords(file, options = {}) {
  const formData = new FormData();
  formData.append("file", file);
  return requestMultipart({
    url: "/agent/word/import",
    method: "POST",
    params: {
      forcePendingWhenTranslated: options.forcePendingWhenTranslated ? true : false,
    },
    data: formData,
  });
}

/**
 * 导出术语字典 Excel（已选 ids 或筛选条件）
 * @param {{ ids?: string[], word?: string, translate?: string, targetLang?: string, department?: string, status?: string }} body
 * @returns {Promise<import('axios').AxiosResponse>}
 */
export function exportTermWords(body) {
  return requestBinary({
    url: "/agent/word/export",
    method: "POST",
    data: body || {},
    responseType: "blob",
    headers: { "Content-Type": "application/json;charset=UTF-8" },
  });
}

/**
 * 注意事项清单适配为标准行
 * @param {File} file
 * @param {string} [targetLang='英文']
 * @returns {Promise<{ data: { list: Object[], total: number } }>}
 */
export function adaptNotesToTermWordRows(file, targetLang = "英文") {
  const formData = new FormData();
  formData.append("file", file);
  return requestMultipart({
    url: "/agent/word/notes-adapt",
    method: "POST",
    params: { targetLang },
    data: formData,
  });
}

/**
 * 导出任意标准行（拆分结果等）为 Excel
 * @param {Object[]} rows
 * @param {boolean} [forcePending=true]
 * @returns {Promise<import('axios').AxiosResponse>}
 */
export function exportTermWordRows(rows, forcePending = true) {
  return requestBinary({
    url: "/agent/word/export-rows",
    method: "POST",
    data: { rows, forcePending },
    responseType: "blob",
    headers: { "Content-Type": "application/json;charset=UTF-8" },
  });
}

/**
 * 按标准行直接导入术语字典（拆分一键入库）
 * @param {Object[]} rows
 * @param {boolean} [forcePending=true]
 * @returns {Promise<{ data: { created: number, skipped: number, skipDetails?: string[] } }>}
 */
export function importTermWordRows(rows, forcePending = true) {
  return request({
    url: "/agent/word/import-rows",
    method: "POST",
    data: { rows, forcePending },
  });
}

/**
 * 术语词典拆分预览
 * @param {{ entry: string, translate?: string, targetLang: string, department?: string, comment?: string }[]} items
 * @param {{ fillWithLlm?: boolean }} [options]
 * @returns {Promise<{ data: { list: Array, total: number } }>}
 */
export function splitTermWordPreview(items, options = {}) {
  const fillWithLlm = options.fillWithLlm !== false;
  return request({
    url: "/agent/word/split-preview",
    method: "POST",
    data: { items, fillWithLlm },
  });
}

/**
 * 术语学习页 — 确认或拒绝
 * approved 时后端 MergeToStore 写入 t_translate
 * @param {string} auditId
 * @param {"approved"|"rejected"} action
 * @param {string} [comment] - 审核备注
 * @returns {Promise<{ data: AuditRecord }>}
 */
export function reviewTerm(auditId, action, comment) {
  return request({
    url: `/agent/term-learning/${auditId}/review`,
    method: "POST",
    data: { action, comment: comment || null },
  });
}

/**
 * 术语学习页 — 批量确认或拒绝
 * @param {string[]} ids - audit id 列表
 * @param {"approved"|"rejected"} action
 * @param {string} [comment] - 审核备注
 * @returns {Promise<{ data: { success_count: number, failed_count: number, failures: Array<{ id: string, reason: string }> } }>}
 */
export function batchReviewTerms(ids, action, comment) {
  return request({
    url: "/agent/term-learning/batch/review",
    method: "POST",
    data: { ids, action, comment: comment || null },
  });
}

/**
 * 工作台批量 Agent 预翻译（PreTranslateModal priority=agent）
 *
 * @param {Object} params
 * @param {string} params.taskID - 翻译任务 id
 * @param {number} [params.confidenceThreshold=0.8] - 置信度阈值
 * @param {string} [params.taskName] - 任务名称（写入 audit 供列表展示）
 * @param {string} [params.productName] - 产品名称
 * @param {string} [params.targetLang] - 目标语种，如「俄文」
 * @param {string} [params.department] - 部门所属
 * @param {Array<Object>} entries - 工作台词条，结构与 Java /workbench/preTranslate 一致
 * @returns {Promise<{ data: PreTranslateBatchResult }>}
 */
export function agentPreTranslate(params, entries) {
  return request({
    url: "/agent/pre-translate/batch",
    method: "POST",
    params: {
      taskID: params.taskID,
      confidenceThreshold: params.confidenceThreshold ?? 0.8,
    },
    data: {
      entries,
      task_name: params.taskName || null,
      product_name: params.productName || null,
      target_lang: params.targetLang || null,
      department: params.department || null,
    },
  });
}

/**
 * 术语学习已选术语切分预览 — 对已选术语执行 jieba 切分，返回候选词片
 * @param {Array<{entry:string, translate?:string, targetLang:string, department?:string}>} items
 * @returns {Promise<{ data: { list: Array, total: number } }>}
 */
export function splitTermAuditItems(items) {
  return request({
    url: "/agent/term-learning/split",
    method: "POST",
    data: { items },
  });
}

/**
 * 按 audit_ids 切分并回填 segment_trace
 * @param {string[]} auditIds
 * @returns {Promise<{ data: { success_count: number } }>}
 */
export function splitTermAuditItemsByIds(auditIds) {
  return request({
    url: "/agent/term-learning/split-by-ids",
    method: "POST",
    data: { audit_ids: auditIds },
  });
}

/**
 * 编辑保存审核记录
 * @param {string} id - audit id
 * @param {Object} data - 要更新的字段（仅传需要修改的字段）
 * @returns {Promise}
 */
export function updateTermAudit(id, data) {
  return request({
    url: `/agent/term-learning/${id}/edit`,
    method: "POST",
    data,
  });
}

/** Comment 规则分页列表 */
export function listCommentRules(params) {
  return request({
    url: "/agent/comment-rule/list",
    method: "GET",
    params,
  });
}

/** Comment 规则单条详情 */
export function getCommentRule(id) {
  return request({
    url: `/agent/comment-rule/${id}`,
    method: "GET",
  });
}

/** 新建 Comment 规则 */
export function createCommentRule(data) {
  return request({
    url: "/agent/comment-rule",
    method: "POST",
    data,
  });
}

/** 更新 Comment 规则 */
export function updateCommentRule(id, data) {
  return request({
    url: `/agent/comment-rule/${id}`,
    method: "PUT",
    data,
  });
}

/** 删除 Comment 规则 */
export function deleteCommentRule(id) {
  return request({
    url: `/agent/comment-rule/${id}`,
    method: "DELETE",
  });
}

/**
 * 导入 Comment 规则 Excel
 * @param {File} file
 * @param {boolean} [overwritePreferAbbr=false]
 */
export function importCommentRules(file, overwritePreferAbbr = false) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("overwritePreferAbbr", overwritePreferAbbr ? "true" : "false");
  return requestMultipart({
    url: "/agent/comment-rule/import",
    method: "POST",
    data: formData,
  });
}
