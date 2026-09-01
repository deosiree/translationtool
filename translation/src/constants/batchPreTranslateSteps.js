/**
 * 批量预翻译「阶段—子步骤—接口」共享常量模块。
 *
 * 用途：作为 `useBatchPreTranslate`（执行逻辑）与 `BatchProgressOverlay`（进度展示）
 * 的唯一文案与接口映射来源，避免阶段名 / 子步骤文案在两处重复定义，
 * 并使代码审查时能快速定位「阶段 → 子步骤 → 接口」的对应关系。
 *
 * 每个阶段内部统一为 4 个子步骤（按执行顺序）：
 *   1. query      查询词条（后端 getEntryInfoList）
 *   2. selectAll  词条全选（前端，无后端调用）
 *   3. <动作>     批量通过（前端，无后端调用）或 预翻译（后端 preTranslate）
 *   4. save       保存（后端 updateEntryList）
 *
 * 接口名仅作为开发元数据用于保证映射正确，不在 UI 上展示；UI 只展示中文文案。
 */

/**
 * 阶段执行顺序（与后端流程一致：词条审核 → 翻译 → 翻译审核）。
 * @type {string[]}
 */
export const STAGE_ORDER = ['entryExamine', 'preTranslate', 'translateExamine']

/**
 * 阶段 key → 中文名称映射。
 * 注意：翻译阶段统一称为「翻译」，不再使用「翻译(预翻译)」这一写法。
 * @type {Object<string, string>}
 */
export const STAGE_NAME_MAP = {
  entryExamine: '词条审核',
  preTranslate: '翻译',
  translateExamine: '翻译审核'
}

/**
 * 阶段 key → 任务对象上的指派人员字段名。
 * 与手工流水线 timeLine/index.vue 读取 currentTask 的字段同源：
 *   - entryExamine     → task.entryAuditor（词条审核员）
 *   - preTranslate     → task.translator（翻译员）
 *   - translateExamine → task.translationAuditor（翻译审核员）
 * @type {Object<string, string>}
 */
export const STAGE_ASSIGNEE_FIELD = {
  entryExamine: 'entryAuditor',
  preTranslate: 'translator',
  translateExamine: 'translationAuditor'
}

/**
 * 阶段 key → 指派角色的中文文案（用于生成跳过原因）。
 * @type {Object<string, string>}
 */
export const STAGE_ASSIGNEE_LABEL = {
  entryExamine: '词条审核员',
  preTranslate: '翻译员',
  translateExamine: '翻译审核员'
}

/**
 * 子步骤的「前后端归属」枚举，用于接口映射的审查标注。
 * @type {Object<string, string>}
 */
export const STEP_KIND = {
  /** 该子步骤触发后端接口调用 */
  BACKEND: 'backend',
  /** 该子步骤仅为前端本地操作，不触发后端接口 */
  FRONTEND: 'frontend'
}

/**
 * 阶段 key → 子步骤定义数组。
 *
 * 每个子步骤对象结构：
 * @property {string} key   子步骤标识，用于 steps / stepCounts 的二级 key
 * @property {string} label 子步骤中文文案（UI 展示用）
 * @property {?string} api  对应后端接口函数名；前端操作为 null
 * @property {string} kind  前后端归属，取值见 {@link STEP_KIND}
 *
 * @type {Object<string, Array<{key: string, label: string, api: ?string, kind: string}>>}
 */
export const STAGE_STEPS = {
  entryExamine: [
    { key: 'query', label: '查询词条', api: 'getEntryInfoList', kind: STEP_KIND.BACKEND },
    { key: 'selectAll', label: '词条全选', api: null, kind: STEP_KIND.FRONTEND },
    { key: 'batchApprove', label: '批量通过', api: null, kind: STEP_KIND.FRONTEND },
    { key: 'save', label: '保存', api: 'updateEntryList', kind: STEP_KIND.BACKEND }
  ],
  preTranslate: [
    { key: 'query', label: '查询词条', api: 'getEntryInfoList', kind: STEP_KIND.BACKEND },
    { key: 'selectAll', label: '词条全选', api: null, kind: STEP_KIND.FRONTEND },
    { key: 'preTranslate', label: '预翻译', api: 'preTranslate', kind: STEP_KIND.BACKEND },
    { key: 'save', label: '保存', api: 'updateEntryList', kind: STEP_KIND.BACKEND }
  ],
  translateExamine: [
    { key: 'query', label: '查询词条', api: 'getEntryInfoList', kind: STEP_KIND.BACKEND },
    { key: 'selectAll', label: '词条全选', api: null, kind: STEP_KIND.FRONTEND },
    { key: 'batchApprove', label: '批量通过', api: null, kind: STEP_KIND.FRONTEND },
    { key: 'save', label: '保存', api: 'updateEntryList', kind: STEP_KIND.BACKEND }
  ]
}

/**
 * 各阶段第一步 query 调用 `getEntryInfoList(params, transStates)` 的统一入参。
 *
 * 第二参数 `transStates` 在后端（EntryTempServiceImpl#getEntryInfoList）是
 * t_translate.translate_state 的过滤列表：传空数组时仅按 entryState（词条状态）查询，
 * 非空时 join 翻译表按 translate_state in (...) + entry_state 过滤。
 * translate_state 语义：0/无翻译记录=未翻译、1=已翻译待审核、2=审核不通过、3=已审核。
 *
 * 口径与三处既有实现保持一致，禁止在调用处再写一份字面量：
 *   - 手工弹窗：examineModal / translateModal / examineTranslateModal
 *   - 流程节点角标：timeLine/index.vue（词条审核 / 翻译 / 翻译审核）
 *
 * @type {Object<string, {entryState: string, transStates: string[]}>}
 */
export const STAGE_QUERY_PARAMS = {
  /** 待审核词条尚无翻译记录，必须传空数组走纯词条状态查询，否则恒 0 条 */
  entryExamine: { entryState: '1', transStates: [] },
  /** 未翻译(0) + 翻译被驳回需重译(2) */
  preTranslate: { entryState: '3', transStates: ['0', '2'] },
  /** 已翻译待审核(1) */
  translateExamine: { entryState: '3', transStates: ['1'] }
}

/**
 * 获取某阶段的中文名称。
 * @param {string} stageKey 阶段 key（entryExamine / preTranslate / translateExamine）
 * @returns {string} 阶段中文名；未知 key 返回其本身
 */
export function getStageLabel(stageKey) {
  return STAGE_NAME_MAP[stageKey] || stageKey
}

/**
 * 获取某阶段的子步骤定义数组。
 * @param {string} stageKey 阶段 key
 * @returns {Array<{key: string, label: string, api: ?string, kind: string}>} 子步骤定义数组；未知 key 返回空数组
 */
export function getStageSteps(stageKey) {
  return STAGE_STEPS[stageKey] || []
}

/**
 * 获取某阶段内某个子步骤的中文文案。
 * @param {string} stageKey 阶段 key
 * @param {string} stepKey  子步骤 key
 * @returns {string} 子步骤中文文案；未找到返回 stepKey 本身
 */
export function getStepLabel(stageKey, stepKey) {
  const steps = STAGE_STEPS[stageKey] || []
  const step = steps.find(s => s.key === stepKey)
  return step ? step.label : stepKey
}
