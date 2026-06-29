/**
 * Agent 预翻译结果 → 工作台语种字段回填
 *
 * 工作台翻译列绑定 language.value（如 english、russian），
 * 而非顶层 translate；API 返回的 translate / suggested_translation 需映射到该字段。
 */

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
  const autoApproved =
    !item.agent_meta ||
    item.agent_meta.review_status === "auto_approved";
  if (!autoApproved || !langField) {
    return item;
  }
  const suggested = resolveAgentSuggestedTranslation(item, langField);
  if (!suggested) {
    return item;
  }
  item[langField] = suggested;
  item.translate = suggested;
  return item;
}
