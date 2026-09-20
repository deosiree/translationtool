import { STAGE_ORDER, getStageSteps } from '@/constants/batchPreTranslateSteps'
import { buildSubTaskId } from '@/utils/batchSplitAggregate'

/**
 * 单个任务的进度对象（Progress）。
 *
 * @typedef {Object} StageCount
 * @property {number} current 当前已处理数量
 * @property {number} total   总数量
 *
 * @typedef {'pending'|'running'|'success'|'warning'|'failed'|'skipped'} Status
 *
 * @typedef {Object} CurrentStep
 * @property {string} stage 当前执行中的阶段 key
 * @property {string} step  当前执行中的子步骤 key
 *
 * @typedef {Object} Progress
 * @property {string} taskId   任务 id
 * @property {string} taskName 任务名称
 * @property {string|null} parentTaskId 子块指向主任务 id
 * @property {Object|null} splitMeta 子块元信息
 * @property {boolean} isSplitParent 主任务当前阶段是否在拆分中
 * @property {string[]} activeSubTaskIds 主任务当前阶段的子块 id 列表
 * @property {Object<string, Status>} stages 阶段级状态
 * @property {Object<string, StageCount>} stageCounts 阶段级词条计数
 * @property {Object<string, Object<string, Status>>} steps 子步骤级状态
 * @property {Object<string, Object<string, number>>} stepCounts 子步骤级词条计数
 * @property {string|null} currentStage 当前阶段 key
 * @property {CurrentStep|null} currentStep 当前子步骤
 * @property {string|null} error 失败时的中文错误信息
 * @property {string|null} warning 阶段告警时的中文提示
 * @property {Object<string, string>} stageMessages 阶段级告警文案
 * @property {number} retryCount 已重试次数
 */

/**
 * 依据阶段启用配置初始化某任务的子步骤状态对象。
 * @param {Object<string, boolean>} stageConfig
 * @returns {Object<string, Object<string, Status>>}
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
 * 初始化子块 progress。
 * continuumStages 内阶段为 pending（query 子步骤 skipped）；其余 skipped。
 * @param {Object} parent 主任务 progress
 * @param {string} stageKey 触发拆分的阶段 key
 * @param {number} chunkIndex 1-based
 * @param {number} chunkTotal
 * @param {Object<string, boolean>} stageConfig
 * @param {string[]} continuumStages 从触发阶段起向后的贯通阶段列表
 * @returns {Progress}
 */
function buildSubTaskProgress(parent, stageKey, chunkIndex, chunkTotal, stageConfig, continuumStages) {
  const continuumSet = new Set(continuumStages?.length ? continuumStages : [stageKey])
  const steps = {}
  const stepCounts = {}
  for (const sk of STAGE_ORDER) {
    const stageSteps = getStageSteps(sk)
    steps[sk] = {}
    stepCounts[sk] = {}
    for (const step of stageSteps) {
      if (continuumSet.has(sk)) {
        steps[sk][step.key] = step.key === 'query' ? 'skipped' : 'pending'
        stepCounts[sk][step.key] = 0
      } else {
        steps[sk][step.key] = 'skipped'
        stepCounts[sk][step.key] = 0
      }
    }
  }

  const stages = {}
  for (const sk of STAGE_ORDER) {
    if (!stageConfig[sk] || !continuumSet.has(sk)) {
      stages[sk] = 'skipped'
    } else {
      stages[sk] = 'pending'
    }
  }

  return {
    taskId: buildSubTaskId(parent.taskId, stageKey, chunkIndex),
    taskName: `${parent.taskName}(${chunkIndex})`,
    parentTaskId: parent.taskId,
    splitMeta: { stageKey, chunkIndex, chunkTotal, continuumStages: [...continuumSet] },
    isSplitParent: false,
    activeSubTaskIds: [],
    stages,
    stageCounts: Object.fromEntries(
      STAGE_ORDER.map(sk => [sk, { current: 0, total: 0 }])
    ),
    steps,
    stepCounts,
    currentStage: stageKey,
    currentStep: null,
    error: null,
    warning: null,
    stageMessages: {},
    retryCount: 0
  }
}

/**
 * 初始化某任务的子步骤计数对象，全部置 0。
 * @returns {Object<string, Object<string, number>>}
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
 * 对 progress 中的子步骤状态做深拷贝。
 * @param {Progress} progress
 * @returns {{steps: Object, stepCounts: Object, currentStep: CurrentStep|null, stageMessages: Object<string, string>}}
 */
function cloneStepState(progress) {
  const steps = {}
  const stepCounts = {}
  for (const stageKey of STAGE_ORDER) {
    steps[stageKey] = { ...(progress.steps?.[stageKey] || {}) }
    stepCounts[stageKey] = { ...(progress.stepCounts?.[stageKey] || {}) }
  }
  const currentStep = progress.currentStep ? { ...progress.currentStep } : null
  const stageMessages = { ...(progress.stageMessages || {}) }
  return { steps, stepCounts, currentStep, stageMessages }
}

function emptyStageCounts() {
  return Object.fromEntries(STAGE_ORDER.map(sk => [sk, { current: 0, total: 0 }]))
}

function cloneStageCounts(stageCounts) {
  const next = emptyStageCounts()
  for (const sk of STAGE_ORDER) {
    next[sk] = { ...(stageCounts?.[sk] || { current: 0, total: 0 }) }
  }
  return next
}

function cloneProgressFields(progress) {
  const stepState = cloneStepState(progress)
  return {
    ...progress,
    stages: { ...progress.stages },
    stageCounts: cloneStageCounts(progress.stageCounts),
    steps: stepState.steps,
    stepCounts: stepState.stepCounts,
    currentStep: stepState.currentStep,
    stageMessages: stepState.stageMessages,
    activeSubTaskIds: [...(progress.activeSubTaskIds || [])],
    splitMeta: progress.splitMeta ? { ...progress.splitMeta } : null
  }
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
    /** 仅主任务参与「全部完成」判定 */
    allCompleted: state => state.progresses
      .filter(p => !p.parentTaskId)
      .every(p =>
        Object.values(p.stages).every(s => s === 'success' || s === 'warning' || s === 'skipped')
      ),
    /** 用于 Overlay 展示：主任务及其活跃子块按序排列 */
    displayProgresses: state => {
      const mains = state.progresses.filter(p => !p.parentTaskId)
      const result = []
      for (const main of mains) {
        result.push(main)
        const subs = state.progresses.filter(
          p => p.parentTaskId === main.taskId && main.activeSubTaskIds?.includes(p.taskId)
        )
        subs.sort((a, b) => (a.splitMeta?.chunkIndex || 0) - (b.splitMeta?.chunkIndex || 0))
        result.push(...subs)
      }
      return result
    }
  },
  mutations: {
    START(state, { config, tasks }) {
      state.phase = 'running'
      state.config = config
      state.progresses = tasks.map(t => ({
        taskId: t.id,
        taskName: t.name,
        parentTaskId: null,
        splitMeta: null,
        isSplitParent: false,
        activeSubTaskIds: [],
        stages: Object.fromEntries(
          STAGE_ORDER.map(sk => [sk, config.stages[sk] ? 'pending' : 'skipped'])
        ),
        stageCounts: emptyStageCounts(),
        steps: buildInitialSteps(config.stages),
        stepCounts: buildInitialStepCounts(),
        currentStage: null,
        currentStep: null,
        error: null,
        warning: null,
        stageMessages: {},
        retryCount: 0
      }))
    },
    UPDATE_PROGRESS(state, progress) {
      const index = state.progresses.findIndex(p => p.taskId === progress.taskId)
      if (index !== -1) {
        state.progresses.splice(index, 1, cloneProgressFields(progress))
      }
    },
    ADD_SUB_TASKS(state, { parentTaskId, stageKey, chunkTotal, stageConfig, continuumStages }) {
      const parentIndex = state.progresses.findIndex(p => p.taskId === parentTaskId)
      if (parentIndex === -1) return

      const parent = state.progresses[parentIndex]
      const subTasks = []
      const subTaskIds = []
      const continuum = continuumStages?.length ? continuumStages : [stageKey]

      for (let i = 1; i <= chunkTotal; i++) {
        const sub = buildSubTaskProgress(parent, stageKey, i, chunkTotal, stageConfig, continuum)
        subTasks.push(sub)
        subTaskIds.push(sub.taskId)
      }

      parent.isSplitParent = true
      parent.activeSubTaskIds = subTaskIds
      state.progresses.splice(parentIndex, 1, cloneProgressFields(parent))
      state.progresses.splice(parentIndex + 1, 0, ...subTasks.map(cloneProgressFields))
    },
    REMOVE_SUB_TASKS(state, { parentTaskId }) {
      const parentIndex = state.progresses.findIndex(p => p.taskId === parentTaskId)
      if (parentIndex === -1) return

      const parent = { ...state.progresses[parentIndex] }
      parent.isSplitParent = false
      parent.activeSubTaskIds = []
      state.progresses.splice(parentIndex, 1, cloneProgressFields(parent))
      state.progresses = state.progresses.filter(p => p.parentTaskId !== parentTaskId)
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
    updateProgress({ commit }, progress) { commit('UPDATE_PROGRESS', progress) },
    addSubTasks({ commit }, payload) { commit('ADD_SUB_TASKS', payload) },
    removeSubTasks({ commit }, payload) { commit('REMOVE_SUB_TASKS', payload) },
    complete({ commit }) { commit('COMPLETE') },
    reset({ commit }) { commit('RESET') }
  }
}
