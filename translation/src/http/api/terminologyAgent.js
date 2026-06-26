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

import request from "../request";

/**
 * RAG 检索命中的参考术语
 * @typedef {Object} SimilarTerm
 * @property {string} entry - 参考词条原文
 * @property {string} translate - 参考词条已有译文
 * @property {number} [score] - 相似度 0~1
 */

/**
 * 单条词条 Agent 预翻译元数据（嵌在 list 项的 agent_meta 字段）
 * @typedef {Object} AgentMeta
 * @property {number} confidence - 置信度 0~1
 * @property {"auto_approved"|"needs_human"} review_status - 分流结果
 * @property {string} [suggested_translation] - 建议译文
 * @property {SimilarTerm[]} similar_terms - 参考术语
 * @property {string} retrieval_method - exact | fuzzy | hybrid | mock_hybrid
 * @property {string} reasoning - Agent 说明
 */

/**
 * 术语学习待审核列表单行（GET /agent/term-learning/list）
 * @typedef {Object} AuditRecord
 * @property {string} id - audit 主键
 * @property {string} source_text - 词条原文
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
 * @property {"pending"|"approved"|"rejected"} review_status
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
 * @param {Object} params - 查询参数，如 { page, pageSize }
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
