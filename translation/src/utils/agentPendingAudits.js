/**
 * Agent 预翻译待审核记录的 localStorage 桥接（Phase 1 演示）。
 *
 * @typedef {import('../http/api/terminologyAgent').AuditRecord} AuditRecord
 * @typedef {import('../http/api/terminologyAgent').AgentMeta} AgentMeta
 */

/** localStorage 键名，存储工作台 Agent 预翻译产生的待审核条目 */
export const AGENT_PENDING_AUDITS_KEY = "agent-pending-audits";

/** Phase 1：API 返回空列表时是否回退 mock 数据；后端就绪后设为 false */
export const USE_AUDIT_MOCK = false;

const RETRIEVAL_METHOD_LABELS = {
  exact: "精确匹配",
  fuzzy: "模糊匹配",
  none: "术语库未命中",
  mock_hybrid: "混合 Mock",
};

/**
 * 检索方式代码 → 中文展示标签
 * @param {string} [method] - exact | fuzzy | hybrid | mock_hybrid
 * @returns {string}
 */
export function formatRetrievalMethod(method) {
  if (!method) return "-";
  return RETRIEVAL_METHOD_LABELS[method] || method;
}

/**
 * 置信度 0~1 → 百分比字符串
 * @param {number|null|undefined} confidence
 * @returns {string}
 */
export function formatConfidence(confidence) {
  if (confidence == null || Number.isNaN(Number(confidence))) return "-";
  return `${Math.round(Number(confidence) * 100)}%`;
}

/**
 * 词条文本展示：换行符转为字面量 `\n`
 * @param {string} [text]
 * @returns {string|undefined}
 */
export function formatEntryText(text) {
  if (!text) return text;
  return String(text).replace(/\n/g, "\\n");
}

/**
 * 从 localStorage 读取本地待审核队列
 * @returns {AuditRecord[]}
 */
export function loadLocalPendingAudits() {
  try {
    const raw = localStorage.getItem(AGENT_PENDING_AUDITS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

/**
 * 将待审核队列写入 localStorage
 * @param {AuditRecord[]} audits
 * @returns {void}
 */
export function saveLocalPendingAudits(audits) {
  try {
    localStorage.setItem(AGENT_PENDING_AUDITS_KEY, JSON.stringify(audits));
  } catch {
    // ignore storage errors
  }
}

/**
 * 按 audit id 从 localStorage 删除一条待审核记录
 * @param {string} auditId
 * @returns {void}
 */
export function removeLocalPendingAudit(auditId) {
  const next = loadLocalPendingAudits().filter((item) => item.id !== auditId);
  saveLocalPendingAudits(next);
}

/**
 * 从 Agent 预翻译结果中提取 needs_human 条目，追加到本地待审核队列
 * @param {Object} options
 * @param {Array<Object & { agent_meta?: AgentMeta }>} options.entries - 预翻译结果列表
 * @param {{ id?: string, name?: string, productName?: string, department?: string }} [options.task] - 当前翻译任务
 * @param {string} [options.targetLang] - 目标语种
 * @param {string} [options.department] - 部门所属
 * @returns {number} 新增待审核条数
 */
export function appendPendingFromPreTranslate({
  entries,
  task,
  targetLang,
  department,
}) {
  const existing = loadLocalPendingAudits();
  const existingIds = new Set(existing.map((item) => item.id));
  const now = new Date().toISOString().replace("T", " ").slice(0, 19);
  const pendingItems = [];

  entries.forEach((item) => {
    const meta = item.agent_meta;
    if (!meta || meta.review_status !== "needs_human") return;

    const auditId = `local-${item.id}-${Date.now()}`;
    if (existingIds.has(auditId)) return;

    const mockTranslation =
      meta.suggested_translation ||
      item.agent_meta?.similar_terms?.[0]?.translate ||
      "";

    pendingItems.push({
      id: auditId,
      entry_info_id: item.id,
      task_id: task?.id || "",
      task_name: task?.name || "",
      product_name: task?.productName || "",
      target_lang: targetLang || "",
      department: department || task?.department || "",
      source_text: item.entry || "",
      suggested_translation: mockTranslation,
      confidence: meta.confidence,
      similar_terms: meta.similar_terms || [],
      retrieval_method: meta.retrieval_method || "",
      llm_reasoning: meta.reasoning || "",
      review_status: "pending",
      source_type: "workbench_agent",
      created_at: now,
      _local: true,
    });
  });

  if (pendingItems.length === 0) return 0;
  saveLocalPendingAudits([...pendingItems, ...existing]);
  return pendingItems.length;
}

/**
 * Phase 1 演示用 mock 待审核数据
 * @returns {AuditRecord[]}
 */
export function getMockPendingAudits() {
  return [
    {
      id: "mock-001",
      entry_info_id: "mock-entry-001",
      task_id: "mock-task-001",
      source_text: "正在查询第 %1/%2 个路径的OID...",
      suggested_translation: "Запрос OID пути %1/%2...",
      target_lang: "俄文",
      task_name: "【Mock】俄文翻译任务",
      product_name: "通用平台部产品",
      department: "通用平台部",
      confidence: 0.62,
      similar_terms: [
        { entry: "查询路径 OID", translate: "Запрос OID пути" },
        { entry: "OID 路径查询", translate: "Запрос пути OID" },
      ],
      retrieval_method: "mock_hybrid",
      llm_reasoning: "Mock：基于术语库相似词条生成的预翻译建议",
      review_status: "pending",
      source_type: "workbench_agent",
      created_at: "2026-06-23 10:00:00",
      _mock: true,
    },
    {
      id: "mock-002",
      entry_info_id: "mock-entry-002",
      task_id: "mock-task-001",
      source_text: "admin",
      suggested_translation: "[Agent Mock] admin",
      target_lang: "俄文",
      task_name: "【Mock】俄文翻译任务",
      product_name: "通用平台部产品",
      department: "通用平台部",
      confidence: 0.55,
      similar_terms: [],
      retrieval_method: "fuzzy",
      llm_reasoning: "Mock：未找到高相似度术语，建议人工确认",
      review_status: "pending",
      source_type: "workbench_agent",
      created_at: "2026-06-23 10:05:00",
      _mock: true,
    },
  ];
}

/**
 * 合并 API、localStorage 与可选 mock 待审核列表；按 entry_info_id + source_text 去重，API 优先
 * @param {Object} options
 * @param {AuditRecord[]} [options.apiItems=] - 后端返回的待审核条目
 * @param {boolean} [options.useMockFallback=USE_AUDIT_MOCK] - 合并结果为空时是否回退 mock
 * @returns {AuditRecord[]}
 */
export function mergePendingAudits({ apiItems = [], useMockFallback = USE_AUDIT_MOCK }) {
  const localItems = loadLocalPendingAudits();
  const merged = new Map();

  const addItem = (item) => {
    const key = `${item.entry_info_id || ""}::${item.source_text || ""}`;
    if (!merged.has(key)) {
      merged.set(key, item);
    }
  };

  apiItems.forEach(addItem);
  localItems.forEach(addItem);

  const result = Array.from(merged.values());
  if (result.length === 0 && useMockFallback) {
    getMockPendingAudits().forEach(addItem);
    return Array.from(merged.values());
  }
  return result;
}
