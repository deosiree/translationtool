/**
 * Agent 预翻译结果 → 工作台语种字段回填
 *
 * 工作台翻译列绑定 language.value（如 english、russian），
 * 而非顶层 translate；API 返回的 translate / suggested_translation 需映射到该字段。
 * auto_approved 时另将 agent_meta.reasoning 拷贝到对应语种审核意见字段（可人工再编辑）。
 */

import commonParam from "@/constants/commonParam.js";

/** @type {Record<string, string>} language.value → auditSuggest 字段名 */
const LANG_TO_AUDIT_SUGGEST = Object.fromEntries(
  commonParam.languageList.map((lang) => [lang.value, lang.auditSuggest])
);

/**
 * 目标语字段名 → 审核意见字段名（如 english → englishAuditSuggest）
 * @param {string} [langField]
 * @returns {string|undefined}
 */
export function resolveAuditSuggestField(langField) {
  if (!langField) return undefined;
  return LANG_TO_AUDIT_SUGGEST[langField];
}

/**
 * 从 Agent 预翻译 API 条目解析建议译文
 * @param {Object} item - API 返回的词条项
 * @param {string} [langField] - 目标语字段名（如 english）
 * @returns {string}
 */
export function resolveAgentSuggestedTranslation(item, langField) {
  const meta = item.agent_meta;
  return (
    meta?.suggested_translation ||
    item.translate ||
    (langField && item[langField]) ||
    ""
  );
}

/**
 * auto_approved 时将建议译文回填到任务目标语字段与 translate
 * @param {Object} item - 词条项（会被原地修改）
 * @param {string} [langField] - 目标语字段名（如 english、russian）
 * @returns {Object} 回填后的 item
 */
export function applyAgentBackfill(item, langField) {
  const meta = item.agent_meta;
  const autoApproved =
    !meta || meta.review_status === "auto_approved";
  if (!autoApproved || !langField) {
    return item;
  }

  const suggested = resolveAgentSuggestedTranslation(item, langField);
  if (suggested) {
    item[langField] = suggested;
    item.translate = suggested;
  }

  const auditField = resolveAuditSuggestField(langField);
  if (meta?.reasoning && auditField) {
    item[auditField] = meta.reasoning;
  }

  return item;
}
