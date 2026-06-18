/**
 * Terminology Learning Agent API module
 *
 * Communicates with the Python LangGraph agent service via /agent/*
 * (proxied by nginx to the terminology-agent container).
 */
import request from "../request";

/**
 * Submit a Chinese term for terminology learning.
 * If the term already exists, returns immediately.
 * If new, the agent runs context analysis + LLM suggestion and creates a pending audit record.
 *
 * @param {string} sourceText - The Chinese term to check
 * @param {string} [context] - Optional surrounding context
 * @returns {Promise<{task_id: string, status: string, message: string}>}
 */
export function runTermLearning(sourceText, context) {
  return request({
    url: "/agent/term-learning/run",
    method: "POST",
    data: { source_text: sourceText, context: context || null },
  });
}

/**
 * Fetch a single audit record by ID.
 *
 * @param {string} auditId
 * @returns {Promise<Object>} Audit record with all fields
 */
export function getAuditRecord(auditId) {
  return request({
    url: `/agent/term-learning/${auditId}`,
    method: "GET",
  });
}

/**
 * List all audit records awaiting human review.
 *
 * @param {number} [limit=50]
 * @returns {Promise<{items: Array, total: number}>}
 */
export function listPendingAudits(limit = 50) {
  return request({
    url: "/agent/term-learning/pending",
    method: "GET",
    params: { limit },
  });
}

/**
 * Submit a human review decision for a pending term suggestion.
 *
 * @param {string} auditId
 * @param {"approved"|"rejected"} action - Review decision
 * @param {string} [comment] - Optional reviewer comment
 * @returns {Promise<Object>} Updated audit record
 */
export function reviewTerm(auditId, action, comment) {
  return request({
    url: `/agent/term-learning/${auditId}/review`,
    method: "POST",
    data: { action, comment: comment || null },
  });
}
