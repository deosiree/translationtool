import { STAGE_ORDER, getStageSteps } from '@/constants/batchPreTranslateSteps'

/**
 * 将数组按固定大小切分为多个子块。
 * @param {Array} arr 源数组
 * @param {number} size 每块最大条数
 * @returns {Array<Array>}
 */
export function chunkArray(arr, size) {
  if (!Array.isArray(arr) || size <= 0) return []
  const chunks = []
  for (let i = 0; i < arr.length; i += size) {
    chunks.push(arr.slice(i, i + size))
  }
  return chunks
}

/**
 * 构造拆分子任务的 progress taskId。
 * @param {string} parentId 主任务 id
 * @param {string} stageKey 阶段 key
 * @param {number} index 1-based 子块序号
 * @returns {string}
 */
export function buildSubTaskId(parentId, stageKey, index) {
  return `${parentId}__split_${stageKey}_${index}`
}

/**
 * 聚合多个子步骤状态为一个主任务子步骤状态。
 * @param {string[]} statuses 子块状态列表
 * @returns {string}
 */
export function aggregateStepStatus(statuses) {
  if (statuses.length === 0) return 'pending'
  if (statuses.some(s => s === 'failed')) return 'failed'
  if (statuses.some(s => s === 'running')) return 'running'
  if (statuses.every(s => s === 'skipped')) return 'skipped'
  if (statuses.every(s => s === 'success' || s === 'skipped')) return 'success'
  if (statuses.some(s => s === 'success' || s === 'warning')) return 'running'
  return 'pending'
}

/**
 * 聚合子块 stage 终态。
 * @param {string[]} statuses 子块 stage 状态
 * @param {boolean} hasWarnings 是否存在告警
 * @returns {string}
 */
export function aggregateStageStatus(statuses, hasWarnings) {
  if (statuses.some(s => s === 'failed')) return 'failed'
  if (statuses.some(s => s === 'running')) return 'running'
  if (statuses.every(s => s === 'skipped')) return 'skipped'
  if (hasWarnings && statuses.every(s => s === 'success' || s === 'warning' || s === 'skipped')) {
    return 'warning'
  }
  if (statuses.every(s => s === 'success' || s === 'skipped' || s === 'warning')) {
    return hasWarnings ? 'warning' : 'success'
  }
  return 'running'
}

/**
 * 将子块 progress 聚合回主任务指定阶段。
 * @param {Object} parent 主任务 progress（将被原地修改）
 * @param {Object[]} subProgresses 子块 progress 列表
 * @param {string} stageKey 阶段 key
 * @param {number} totalEntries query 总条数
 * @returns {{ hasFailed: boolean, hasWarning: boolean, processedTotal: number, warningReasons: string[] }}
 */
export function aggregateToParent(parent, subProgresses, stageKey, totalEntries) {
  const stageSteps = getStageSteps(stageKey)
  const postQuerySteps = stageSteps.filter(s => s.key !== 'query')

  for (const step of postQuerySteps) {
    const subStatuses = subProgresses.map(sp => sp.steps?.[stageKey]?.[step.key] || 'pending')
    parent.steps[stageKey][step.key] = aggregateStepStatus(subStatuses)
    const sum = subProgresses.reduce((acc, sp) => {
      const c = sp.stepCounts?.[stageKey]?.[step.key]
      return acc + (typeof c === 'number' ? c : 0)
    }, 0)
    parent.stepCounts[stageKey][step.key] = sum
  }

  const processedTotal = subProgresses.reduce((acc, sp) => {
    return acc + (sp.stageCounts?.[stageKey]?.current || 0)
  }, 0)

  parent.stageCounts[stageKey] = { current: processedTotal, total: totalEntries }

  const subStageStatuses = subProgresses.map(sp => sp.stages?.[stageKey] || 'pending')
  const warningReasons = subProgresses
    .map(sp => sp.stageMessages?.[stageKey] || sp.warning)
    .filter(Boolean)
  const hasWarning = warningReasons.length > 0 || subStageStatuses.includes('warning')
  const hasFailed = subStageStatuses.includes('failed')

  parent.stages[stageKey] = aggregateStageStatus(subStageStatuses, hasWarning)

  parent.retryCount = Math.max(
    parent.retryCount || 0,
    ...subProgresses.map(sp => sp.retryCount || 0)
  )

  if (hasWarning) {
    const unique = [...new Set(warningReasons)]
    parent.stageMessages = parent.stageMessages || {}
    parent.stageMessages[stageKey] = unique.join('；')
    parent.warning = unique.length > 0 ? `翻译阶段告警：${unique.join('；')}` : parent.warning
  }

  return { hasFailed, hasWarning, processedTotal, warningReasons }
}

/**
 * 判断 progress 是否为主任务（非拆分子块）。
 * @param {Object} progress
 * @returns {boolean}
 */
export function isMainProgress(progress) {
  return !progress.parentTaskId
}
