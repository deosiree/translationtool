import { STAGE_ORDER, getStageSteps } from '@/constants/batchPreTranslateSteps'

/**
 * 单个任务的进度对象（Progress）。
 *
 * @typedef {Object} StageCount
 * @property {number} current 当前已处理数量
 * @property {number} total   总数量
 *
 * @typedef {'pending'|'running'|'success'|'failed'|'skipped'} Status
 *
 * @typedef {Object} CurrentStep
 * @property {string} stage 当前执行中的阶段 key
 * @property {string} step  当前执行中的子步骤 key
 *
 * @typedef {Object} Progress
 * @property {string} taskId   任务 id
 * @property {string} taskName 任务名称
 * @property {Object<string, Status>} stages 阶段级状态（阶段图标与「全部完成」判定）
 * @property {Object<string, StageCount>} stageCounts 阶段级词条计数
 * @property {Object<string, Object<string, Status>>} steps 子步骤级状态
 * @property {Object<string, Object<string, number>>} stepCounts 子步骤级词条计数（用于「x条」展示）
 * @property {string|null} currentStage 当前阶段 key（保留兼容）
 * @property {CurrentStep|null} currentStep 当前子步骤（驱动蓝色「执行中」文字与高亮）
 * @property {string|null} error 失败时的中文错误信息
 * @property {number} retryCount 已重试次数
 */

/**
 * 依据阶段启用配置初始化某任务的子步骤状态对象。
 * 启用的阶段：其全部子步骤为 pending；未启用的阶段：全部子步骤为 skipped。
 *
 * @param {Object<string, boolean>} stageConfig 阶段启用配置（如 { entryExamine: true, ... }）
 * @returns {Object<string, Object<string, Status>>} steps 初始状态
 */
function buildInitialSteps(stageConfig) {
  const steps = {}
  for (const stageKey of STAGE_ORDER) {
    const stageSteps = getStageSteps(stageKey)
    const enabled = stageConfig[stageKey] === true
    const status = enabled ? 'pending' : 'skipped'
    steps[stageKey] = {}
    for (const step of stageSteps) {
      steps[stageKey][step.key] = status
    }
  }
  return steps
}

/**
 * 初始化某任务的子步骤计数对象，全部置 0。
 *
 * @returns {Object<string, Object<string, number>>} stepCounts 初始状态
 */
function buildInitialStepCounts() {
  const stepCounts = {}
  for (const stageKey of STAGE_ORDER) {
    const stageSteps = getStageSteps(stageKey)
    stepCounts[stageKey] = {}
    for (const step of stageSteps) {
      stepCounts[stageKey][step.key] = 0
    }
  }
  return stepCounts
}

/**
 * 对 progress 中的子步骤状态做深拷贝，避免跨任务共享引用破坏 Vue 响应式。
 *
 * @param {Progress} progress 源进度对象
 * @returns {{steps: Object, stepCounts: Object, currentStep: CurrentStep|null}} 深拷贝后的新字段
 */
function cloneStepState(progress) {
  const steps = {}
  const stepCounts = {}
  for (const stageKey of STAGE_ORDER) {
    steps[stageKey] = { ...(progress.steps?.[stageKey] || {}) }
    stepCounts[stageKey] = { ...(progress.stepCounts?.[stageKey] || {}) }
  }
  const currentStep = progress.currentStep ? { ...progress.currentStep } : null
  return { steps, stepCounts, currentStep }
}

export default {
  namespaced: true,
  state: {
    phase: 'idle',
    config: null,
    progresses: []
  },
  getters: {
    visible: state => state.phase !== 'idle',
    isRunning: state => state.phase === 'running',
    allCompleted: state => state.progresses.every(p =>
      Object.values(p.stages).every(s => s === 'success' || s === 'skipped')
    )
  },
  mutations: {
    /**
     * 启动批量执行，初始化每个任务的进度对象。
     * @param {Object} state 模块 state
     * @param {{config: Object, tasks: Array<{id: string, name: string}>}} payload
     */
    START(state, { config, tasks }) {
      state.phase = 'running'
      state.config = config
      state.progresses = tasks.map(t => ({
        taskId: t.id,
        taskName: t.name,
        stages: {
          entryExamine: config.stages.entryExamine ? 'pending' : 'skipped',
          preTranslate: config.stages.preTranslate ? 'pending' : 'skipped',
          translateExamine: config.stages.translateExamine ? 'pending' : 'skipped'
        },
        stageCounts: {
          entryExamine: { current: 0, total: 0 },
          preTranslate: { current: 0, total: 0 },
          translateExamine: { current: 0, total: 0 }
        },
        steps: buildInitialSteps(config.stages),
        stepCounts: buildInitialStepCounts(),
        currentStage: null,
        currentStep: null,
        error: null,
        retryCount: 0
      }))
    },
    /**
     * 更新某个任务的进度对象（原地替换，并深拷贝嵌套结构避免共享引用）。
     * @param {Object} state 模块 state
     * @param {Progress} progress 最新进度对象
     */
    UPDATE_PROGRESS(state, progress) {
      const index = state.progresses.findIndex(p => p.taskId === progress.taskId)
      if (index !== -1) {
        const stepState = cloneStepState(progress)
        state.progresses.splice(index, 1, {
          ...progress,
          stages: { ...progress.stages },
          stageCounts: {
            entryExamine: { ...(progress.stageCounts?.entryExamine || { current: 0, total: 0 }) },
            preTranslate: { ...(progress.stageCounts?.preTranslate || { current: 0, total: 0 }) },
            translateExamine: { ...(progress.stageCounts?.translateExamine || { current: 0, total: 0 }) }
          },
          steps: stepState.steps,
          stepCounts: stepState.stepCounts,
          currentStep: stepState.currentStep
        })
      }
    },
    COMPLETE(state) {
      state.phase = 'completed'
    },
    RESET(state) {
      state.phase = 'idle'
      state.config = null
      state.progresses = []
    }
  },
  actions: {
    start({ commit }, { config, tasks }) { commit('START', { config, tasks }) },
    updateProgress({ commit }, progresses) { commit('UPDATE_PROGRESS', progresses) },
    complete({ commit }) { commit('COMPLETE') },
    reset({ commit }) { commit('RESET') }
  }
}
