/**
 * Agent 预翻译待审核记录的 localStorage 桥接。
 *
 * Phase 3a：``formatRetrievalMethod`` / ``formatRetrievalSource`` 支持 Grep 线展示。
 *
 * @typedef {import('../http/api/terminologyAgent').AuditRecord} AuditRecord
 * @typedef {import('../http/api/terminologyAgent').AgentMeta} AgentMeta
 */

/** localStorage 键名，存储工作台 Agent 预翻译产生的待审核条目 */
export const AGENT_PENDING_AUDITS_KEY = "agent-pending-audits";

/** Phase 1：API 返回空列表时是否回退 mock 数据；后端就绪后设为 false */
export const USE_AUDIT_MOCK = false;

/** retrieval_method 代码 → 中文标签（含 Phase 3a Grep / hybrid） */
const RETRIEVAL_METHOD_LABELS = {
  exact: "精确匹配",
  fuzzy: "模糊匹配",
  grep: "Grep 关键字",
  hybrid: "混合检索",
  decomposed: "拆解拼装",
  none: "术语库未命中",
  mock_hybrid: "混合 Mock",
};

/**
 * 参考术语检索来源 → 中文展示（Popover「来源」列）
 * @param {string} [source] - rag | grep | rag+grep
 * @returns {string}
 */
export function formatRetrievalSource(source) {
  if (source === "grep") return "Grep";
  if (source === "rag") return "RAG";
  if (source === "rag+grep") return "RAG+Grep";
  return source || "-";
}

/**
 * 检索方式代码 → 中文展示标签
 * @param {string} [method] - exact | fuzzy | grep | hybrid | decomposed | none | mock_hybrid
 * @returns {string}
 */
export function formatRetrievalMethod(method) {
  if (!method) return "-";
  return RETRIEVAL_METHOD_LABELS[method] || method;
}

/** translation_source / llm_reasoning 前缀 → 中文展示 */
const TRANSLATION_SOURCE_LABELS = {
  term: "术语库",
  llm: "LLM 机翻",
  hybrid: "混合检索",
};

/**
 * 翻译来源 → 中文展示（优先 record.translation_source，否则从 llm_reasoning 推断）
 * @param {Partial<AuditRecord>} record
 * @returns {string}
 */
export function formatTranslationSource(record) {
  const direct = record?.translation_source;
  if (direct && TRANSLATION_SOURCE_LABELS[direct]) {
    return TRANSLATION_SOURCE_LABELS[direct];
  }
  const reasoning = record?.llm_reasoning || "";
  if (reasoning.startsWith("基于术语")) return TRANSLATION_SOURCE_LABELS.term;
  if (reasoning.startsWith("基于LLM")) return TRANSLATION_SOURCE_LABELS.llm;
  if (reasoning.startsWith("基于混合")) return TRANSLATION_SOURCE_LABELS.hybrid;
  return "-";
}

/** 术语学习搜索 — 检索方式下拉（不含 mock） */
const RETRIEVAL_METHOD_FILTER_KEYS = [
  "exact",
  "fuzzy",
  "grep",
  "hybrid",
  "decomposed",
  "none",
];

/**
 * 检索方式筛选项（value=代码，label=中文）
 * @returns {{ label: string, value: string }[]}
 */
export function getRetrievalMethodOptions() {
  return RETRIEVAL_METHOD_FILTER_KEYS.map((value) => ({
    value,
    label: RETRIEVAL_METHOD_LABELS[value] || value,
  }));
}

/**
 * 写入去重指纹 — 与后端 audit_write_fingerprint 规则一致
 * @param {Partial<AuditRecord>} record
 * @returns {string}
 */
export function auditWriteFingerprint(record) {
  const norm = (v) => (v != null ? String(v).trim() : "");
  const conf = record.confidence;
  const confNorm =
    conf == null || Number.isNaN(Number(conf))
      ? null
      : Math.round(Number(conf) * 1000) / 1000;
  return JSON.stringify([
    norm(record.source_text),
    norm(record.entry_comment),
    norm(record.suggested_translation),
    norm(record.target_lang),
    norm(record.department),
    norm(record.retrieval_method),
    confNorm,
  ]);
}

/**
 * 术语学习默认 search 对象
 * @returns {Object}
 */
export function createDefaultAuditSearch() {
  return {
    sourceText: null,
    entryComment: null,
    targetLang: null,
    taskName: null,
    productName: null,
    department: null,
    confidenceMin: null,
    confidenceMax: null,
    retrievalMethod: null,
  };
}

/**
 * 将 search 转为 GET /agent/term-learning/list 的 query params
 * @param {Object} [search]
 * @param {{ current?: number, pageSize?: number }} [pagination]
 * @returns {Object}
 */
export function buildAuditListParams(search = {}, pagination = {}) {
  const params = {};
  if (pagination.current != null) params.page = pagination.current;
  if (pagination.pageSize != null) params.pageSize = pagination.pageSize;

  const text = (v) => (v != null && String(v).trim() ? String(v).trim() : null);

  const sourceText = text(search.sourceText);
  if (sourceText) params.sourceText = sourceText;
  const entryComment = text(search.entryComment);
  if (entryComment) params.entryComment = entryComment;
  if (search.targetLang) params.targetLang = search.targetLang;
  const taskName = text(search.taskName);
  if (taskName) params.taskName = taskName;
  const productName = text(search.productName);
  if (productName) params.productName = productName;
  if (search.department) params.department = search.department;
  if (search.retrievalMethod) params.retrievalMethod = search.retrievalMethod;

  let confMin =
    search.confidenceMin != null && search.confidenceMin !== ""
      ? Number(search.confidenceMin)
      : null;
  let confMax =
    search.confidenceMax != null && search.confidenceMax !== ""
      ? Number(search.confidenceMax)
      : null;
  if (confMin != null && Number.isNaN(confMin)) confMin = null;
  if (confMax != null && Number.isNaN(confMax)) confMax = null;
  if (confMin != null && confMax != null && confMin > confMax) {
    [confMin, confMax] = [confMax, confMin];
  }
  if (confMin != null) params.confidenceMin = confMin / 100;
  if (confMax != null) params.confidenceMax = confMax / 100;
  return params;
}

/**
 * 提取纯筛选字段（不含分页），供本地项客户端过滤
 * @param {Object} [search]
 * @returns {Object|null}
 */
export function extractAuditFilters(search = {}) {
  const params = buildAuditListParams(search);
  delete params.page;
  delete params.pageSize;
  return Object.keys(params).length ? params : null;
}

/**
 * 本地/mock 待审核项是否匹配筛选条件
 * @param {AuditRecord} item
 * @param {Object|null} filters - buildAuditListParams 产出（confidence 已为 0~1）
 * @returns {boolean}
 */
export function matchesAuditFilters(item, filters) {
  if (!filters) return true;

  if (filters.sourceText) {
    const src = String(item.source_text || "");
    if (!src.includes(filters.sourceText)) return false;
  }
  if (filters.entryComment) {
    const comment = String(item.entry_comment || "");
    if (!comment.includes(filters.entryComment)) return false;
  }
  if (filters.targetLang && item.target_lang !== filters.targetLang) return false;
  if (filters.taskName) {
    const name = String(item.task_name || "");
    if (!name.includes(filters.taskName)) return false;
  }
  if (filters.productName) {
    const name = String(item.product_name || "");
    if (!name.includes(filters.productName)) return false;
  }
  if (filters.department && item.department !== filters.department) return false;
  if (filters.retrievalMethod && item.retrieval_method !== filters.retrievalMethod) {
    return false;
  }
  if (filters.confidenceMin != null || filters.confidenceMax != null) {
    const conf = item.confidence;
    if (conf == null || Number.isNaN(Number(conf))) return false;
    const num = Number(conf);
    if (filters.confidenceMin != null && num < filters.confidenceMin) return false;
    if (filters.confidenceMax != null && num > filters.confidenceMax) return false;
  }
  return true;
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
 * 清除 localStorage 中 _local / _mock 条目，保留其他本地项
 * @returns {number} 清除条数
 */
export function clearLocalMockPendingAudits() {
  const existing = loadLocalPendingAudits();
  const next = existing.filter((item) => !item._local && !item._mock);
  const removed = existing.length - next.length;
  saveLocalPendingAudits(next);
  return removed;
}

/**
 * 筛选后的 localStorage 待审核项（不去重）
 * @param {Object|null} [filters]
 * @returns {AuditRecord[]}
 */
export function listLocalPendingAudits(filters = null) {
  const items = loadLocalPendingAudits();
  if (!filters) return items;
  return items.filter((item) => matchesAuditFilters(item, filters));
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
  const existingFingerprints = new Set(existing.map(auditWriteFingerprint));
  const now = new Date().toISOString().replace("T", " ").slice(0, 19);
  const pendingItems = [];

  entries.forEach((item) => {
    const meta = item.agent_meta;
    if (!meta || meta.review_status !== "needs_human") return;

    const mockTranslation =
      meta.suggested_translation ||
      item.agent_meta?.similar_terms?.[0]?.translate ||
      "";

    const candidate = {
      id: `local-${item.id}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      entry_info_id: item.id,
      task_id: task?.id || "",
      task_name: task?.name || "",
      product_name: task?.productName || "",
      target_lang: targetLang || "",
      department: department || task?.department || "",
      source_text: item.entry || "",
      entry_comment: item.comment || "",
      suggested_translation: mockTranslation,
      confidence: meta.confidence,
      similar_terms: meta.similar_terms || [],
      retrieval_method: meta.retrieval_method || "",
      llm_reasoning: meta.reasoning || "",
      review_status: "pending",
      source_type: "workbench_agent",
      created_at: now,
      _local: true,
    };

    const fp = auditWriteFingerprint(candidate);
    if (existingFingerprints.has(fp)) return;
    existingFingerprints.add(fp);
    pendingItems.push(candidate);
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
      entry_comment: "",
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
      entry_comment: "",
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
 * 合并 API、localStorage 与可选 mock 待审核列表；展示不去重，全量拼接
 * @param {Object} options
 * @param {AuditRecord[]} [options.apiItems=] - 后端返回的待审核条目
 * @param {boolean} [options.useMockFallback=USE_AUDIT_MOCK] - 合并结果为空时是否回退 mock
 * @param {Object|null} [options.filters=null] - 客户端筛选（仅作用于 local/mock 项）
 * @returns {AuditRecord[]}
 */
export function mergePendingAudits({
  apiItems = [],
  useMockFallback = USE_AUDIT_MOCK,
  filters = null,
}) {
  const localItems = listLocalPendingAudits(filters);
  let result = [...localItems, ...apiItems];
  if (result.length === 0 && useMockFallback) {
    result = getMockPendingAudits().filter((item) =>
      matchesAuditFilters(item, filters),
    );
  }
  return result;
}
