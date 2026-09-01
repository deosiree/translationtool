import { STAGE_ASSIGNEE_FIELD, STAGE_ASSIGNEE_LABEL, getStageLabel } from '@/constants/batchPreTranslateSteps'

/**
 * 批量预翻译「阶段权限判定」工具模块。
 *
 * 口径与手工流水线 timeLine/index.vue#isButtonDisabled 保持一致：
 * 仅当当前用户 userName 等于该阶段在任务上的指派人员时才可执行，不引入管理员绕过。
 * 指派字段为空 / 缺失、用户为空均视为无权限。
 */

/**
 * 判断当前用户是否有权执行某任务下的某阶段。
 *
 * @param {?Object} user 当前用户（store.state.user，含 userName）
 * @param {Object} task 任务对象（含 entryAuditor / translator / translationAuditor）
 * @param {string} stageKey 阶段 key（entryExamine / preTranslate / translateExamine）
 * @returns {boolean} 有权执行返回 true；用户为空、指派字段为空/缺失或不匹配返回 false
 */
export function canRunStage(user, task, stageKey) {
  const assigneeField = STAGE_ASSIGNEE_FIELD[stageKey]
  if (!assigneeField) return false
  const userName = user?.userName
  const assignee = task?.[assigneeField]
  if (!userName || !assignee) return false
  return userName === assignee
}

/**
 * 生成某阶段被权限跳过时的中文原因。
 *
 * @param {?Object} user 当前用户（store.state.user，含 userName）
 * @param {Object} task 任务对象（含指派字段）
 * @param {string} stageKey 阶段 key
 * @returns {string} 中文跳过原因；未知阶段返回通用文案
 */
export function getSkipReason(user, task, stageKey) {
  const assigneeField = STAGE_ASSIGNEE_FIELD[stageKey]
  const stageName = getStageLabel(stageKey)
  if (!assigneeField) return `无「${stageName}」执行权限，已跳过该阶段`
  const roleLabel = STAGE_ASSIGNEE_LABEL[stageKey] || stageName
  const assignee = task?.[assigneeField]
  if (!assignee) {
    return `未指定「${roleLabel}」，已跳过该阶段`
  }
  return `无「${roleLabel}」权限（指派：${assignee}），已跳过该阶段`
}
