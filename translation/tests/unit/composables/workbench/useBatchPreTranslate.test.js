import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useBatchPreTranslate } from '@/composables/workbench/useBatchPreTranslate'
import { STAGE_ORDER, STAGE_STEPS, ARCHIVE_MODE } from '@/constants/batchPreTranslateSteps'
import batchProgressModule from '@/store/modules/batchProgress'
import * as workbenchApi from '@/http/api/workbench'
import * as i18ServerApi from '@/http/api/i18Server'
import * as taskApi from '@/http/api/task'

vi.mock('@/http/api/workbench', () => ({
  getEntryInfoList: vi.fn(),
  updateEntryList: vi.fn(),
  preTranslate: vi.fn()
}))

vi.mock('@/http/api/i18Server', () => ({
  setInfo: vi.fn()
}))

vi.mock('@/http/api/task', () => ({
  updateTaskInfo: vi.fn()
}))

vi.mock('@/utils/dateUtils', () => ({
  getCurrentFormattedTime: () => '2026-09-20 12:00:00'
}))

/** 测试用当前用户 userName，任务指派人员默认与其一致（表示有权限）。 */
const CURRENT_USER = 'currentUser'

/**
 * 构造与 store START 一致的空初始子步骤状态（steps / stepCounts）。
 * @returns {{steps: Object, stepCounts: Object}}
 */
function buildInitialStepState() {
  const steps = {}
  const stepCounts = {}
  for (const stage of STAGE_ORDER) {
    steps[stage] = {}
    stepCounts[stage] = {}
    for (const step of STAGE_STEPS[stage]) {
      steps[stage][step.key] = 'pending'
      stepCounts[stage][step.key] = 0
    }
  }
  return { steps, stepCounts }
}

/**
 * 构造一个模拟的任务进度对象（模拟 store START 后的初始形态）。
 * @returns {Object}
 */
function buildMockProgress(overrides = {}) {
  const { steps, stepCounts } = buildInitialStepState()
  const stages = Object.fromEntries(STAGE_ORDER.map(sk => [sk, 'pending']))
  const stageCounts = Object.fromEntries(STAGE_ORDER.map(sk => [sk, { current: 0, total: 0 }]))
  return {
    taskId: 'task-1',
    taskName: 'Task 1',
    parentTaskId: null,
    splitMeta: null,
    isSplitParent: false,
    activeSubTaskIds: [],
    stages,
    stageCounts,
    steps,
    stepCounts,
    currentStage: null,
    currentStep: null,
    error: null,
    retryCount: 0,
    ...overrides
  }
}

/** 模拟 batchProgress store mutations（含 addSubTasks / removeSubTasks） */
function createMockBatchProgressState(initialProgresses) {
  const state = { phase: 'running', config: null, progresses: initialProgresses.map(p => ({ ...p })) }
  return state
}

function dispatchBatchProgress(state, action, payload) {
  const { mutations } = batchProgressModule
  if (action === 'batchProgress/updateProgress') {
    mutations.UPDATE_PROGRESS(state, payload)
  } else if (action === 'batchProgress/addSubTasks') {
    mutations.ADD_SUB_TASKS(state, payload)
  } else if (action === 'batchProgress/removeSubTasks') {
    mutations.REMOVE_SUB_TASKS(state, payload)
  } else if (action === 'batchProgress/complete') {
    mutations.COMPLETE(state)
  }
}

/**
 * 构造测试任务，默认三阶段指派人员均为当前用户（有权限）。
 * @param {Object} overrides 需要覆盖的任务字段
 * @returns {Object}
 */
function buildTask(overrides = {}) {
  return {
    id: 'task-1',
    name: 'Task 1',
    translateType: '英文',
    entryAuditor: CURRENT_USER,
    translator: CURRENT_USER,
    translationAuditor: CURRENT_USER,
    creator: CURRENT_USER,
    ...overrides
  }
}

describe('useBatchPreTranslate', () => {
  let mockStore

  beforeEach(() => {
    vi.clearAllMocks()
    const batchState = createMockBatchProgressState([buildMockProgress()])
    mockStore = {
      state: {
        user: { userName: CURRENT_USER, roleName: '翻译员' },
        batchProgress: batchState
      },
      dispatch: vi.fn((action, payload) => {
        dispatchBatchProgress(batchState, action, payload)
      })
    }
  })

  it('0 条词条：每个阶段查询词条 success(0条)，其余子步骤 skipped，阶段 success，不中断流程', async () => {
    const { execute } = useBatchPreTranslate()
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: [] } })

    const config = {
      tasks: [buildTask()],
      stages: { entryExamine: true, preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.entryExamine).toBe('success')
    expect(progress.stages.preTranslate).toBe('success')
    expect(progress.stages.translateExamine).toBe('success')
    expect(progress.currentStage).toBeNull()
    expect(progress.currentStep).toBeNull()
    expect(progress.error).toBeNull()
    expect(progress.stageCounts.preTranslate).toEqual({ current: 0, total: 0 })

    // 每个启用阶段：query 子步骤 success，其余子步骤 skipped
    const enabled = ['entryExamine', 'preTranslate', 'translateExamine']
    for (const stage of enabled) {
      expect(progress.steps[stage].query).toBe('success')
      for (const step of STAGE_STEPS[stage]) {
        if (step.key === 'query') continue
        expect(progress.steps[stage][step.key]).toBe('skipped')
      }
    }

    // 每个启用阶段都独立查询了一次
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(3)
  })

  it('预翻译：查询/词条全选/预翻译/保存 四个子步骤成功，计数正确', async () => {
    const { execute } = useBatchPreTranslate()
    const rawEntries = [{ id: 'e1', entry: '测试', english: '' }]
    const translatedEntries = [{ id: 'e1', entry: '测试', english: 'Test' }]

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: rawEntries } })
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: { list: translatedEntries } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    const config = {
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.preTranslate).toBe('success')
    expect(progress.stageCounts.preTranslate).toEqual({ current: 1, total: 1 })
    expect(progress.currentStage).toBeNull()
    expect(progress.currentStep).toBeNull()

    // 四个子步骤全部 success
    expect(progress.steps.preTranslate.query).toBe('success')
    expect(progress.steps.preTranslate.selectAll).toBe('success')
    expect(progress.steps.preTranslate.preTranslate).toBe('success')
    expect(progress.steps.preTranslate.save).toBe('success')

    // 子步骤计数
    expect(progress.stepCounts.preTranslate.query).toBe(1)
    expect(progress.stepCounts.preTranslate.selectAll).toBe(1)
    expect(progress.stepCounts.preTranslate.preTranslate).toBe(1)
    expect(progress.stepCounts.preTranslate.save).toBe(1)
  })

  it('翻译阶段达到重试上限：错误信息中文，currentStep/currentStage 清理', async () => {
    const { execute } = useBatchPreTranslate()
    workbenchApi.getEntryInfoList.mockRejectedValue(new Error('Network error'))

    const config = {
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.preTranslate).toBe('failed')
    expect(progress.error).toBe('翻译阶段失败，并达到重复上限')
    expect(progress.currentStage).toBeNull()
    expect(progress.currentStep).toBeNull()
    expect(progress.steps.preTranslate.query).toBe('failed')
  })

  it('预翻译 HTTP 200 返回空数组：不重试并继续独立执行翻译审核', async () => {
    const { execute } = useBatchPreTranslate()
    const queryEntries = [{ id: 'e1', entry: '测试', english: '' }]
    const reviewEntries = [{ id: 'e1', entry: '测试', english: 'Test' }]

    workbenchApi.getEntryInfoList
      .mockResolvedValueOnce({ data: { list: queryEntries } })
      .mockResolvedValueOnce({ data: { list: reviewEntries } })
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: { list: [] } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    const config = {
      tasks: [buildTask()],
      stages: { preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 3,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.preTranslate).toBe('warning')
    expect(progress.warning).toContain('空数组')
    expect(progress.steps.preTranslate.preTranslate).toBe('success')
    expect(progress.steps.preTranslate.save).toBe('skipped')
    expect(progress.stages.translateExamine).toBe('success')
    expect(workbenchApi.preTranslate).toHaveBeenCalledTimes(1)
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(2)
  })

  it('预翻译告警使用下拉框对应的优先级中文名称', async () => {
    const { execute } = useBatchPreTranslate()
    const queryEntries = [{ id: 'e1', entry: '测试', english: '' }]

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: queryEntries } })
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: { list: [{ id: 'e1', entry: '测试', english: '' }] } })

    const config = {
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'deepl',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.warning).toContain('DeepL翻译')
    expect(progress.warning).not.toContain('当前翻译方法')
  })

  it('预翻译 HTTP 200 缺少数组结构：不重试并以告警结束', async () => {
    const { execute } = useBatchPreTranslate()
    const queryEntries = [{ id: 'e1', entry: '测试', english: '' }]

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: queryEntries } })
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: {} })

    const config = {
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      maxRetries: 3,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.preTranslate).toBe('warning')
    expect(progress.warning).toContain('数据格式异常')
    expect(workbenchApi.preTranslate).toHaveBeenCalledTimes(1)
    expect(workbenchApi.updateEntryList).not.toHaveBeenCalled()
  })

  it('预翻译返回数量不一致且含空译文：仅保存有效子集，阶段告警', async () => {
    const { execute } = useBatchPreTranslate()
    const queryEntries = [
      { id: 'e1', entry: '测试1', english: '' },
      { id: 'e2', entry: '测试2', english: '' }
    ]
    const translatedEntries = [
      { id: 'e1', entry: '测试1', english: '' },
      { id: 'e2', entry: '测试2', english: 'Test 2' },
      { id: 'extra', entry: '多余词条', english: 'Extra' }
    ]

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: queryEntries } })
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: { list: translatedEntries } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    const config = {
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.preTranslate).toBe('warning')
    expect(progress.warning).toContain('无译文')
    expect(progress.warning).toContain('数量不一致')
    expect(progress.stageCounts.preTranslate).toEqual({ current: 1, total: 2 })
    expect(workbenchApi.updateEntryList).toHaveBeenCalledWith(
      { taskID: 'task-1' },
      [{ id: 'e2', entry: '测试2', english: 'Test 2', englishTranslateState: '1' }]
    )
  })

  it('预翻译结果未通过校验：不保存失败词条并以告警结束', async () => {
    const { execute } = useBatchPreTranslate()
    const queryEntries = [{ id: 'e1', entry: '测试', english: '', maxLength: 1 }]
    const translatedEntries = [{ id: 'e1', entry: '测试', english: 'Test' }]

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: queryEntries } })
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: { list: translatedEntries } })

    const config = {
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [
        { key: 'toLong', checked: true },
        { key: 'special', checked: false }
      ],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.preTranslate).toBe('warning')
    expect(progress.warning).toContain('校验不通过')
    expect(progress.stageCounts.preTranslate).toEqual({ current: 0, total: 1 })
    expect(workbenchApi.updateEntryList).not.toHaveBeenCalled()
  })

  it('翻译阶段 5xx/网络失败重试耗尽后仍继续独立查询翻译审核', async () => {
    const { execute } = useBatchPreTranslate()
    const reviewEntries = [{ id: 'e1', entry: '测试', english: 'Test' }]

    workbenchApi.getEntryInfoList
      .mockRejectedValueOnce(new Error('pre-translate query failed'))
      .mockResolvedValueOnce({ data: { list: reviewEntries } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    const config = {
      tasks: [buildTask()],
      stages: { preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.preTranslate).toBe('failed')
    expect(progress.stages.translateExamine).toBe('success')
    expect(progress.error).toBe('翻译阶段失败，并达到重复上限')
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(2)
    expect(workbenchApi.updateEntryList).toHaveBeenCalledTimes(1)
  })

  it('词条审核接口映射：查询→getEntryInfoList，保存→updateEntryList，词条全选/批量通过不触发后端', async () => {
    const { execute } = useBatchPreTranslate()
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: [{ id: 'e1', entry: '测试', english: '' }] } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    const config = {
      tasks: [buildTask()],
      stages: { entryExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    // 词条全选 / 批量通过为前端操作，不应触发后端；后端仅 query 与 save
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(1)
    expect(workbenchApi.updateEntryList).toHaveBeenCalledTimes(1)
    expect(workbenchApi.preTranslate).not.toHaveBeenCalled()
  })

  it('0 条阶段不中断后续阶段：每个阶段各自重新查询待处理词条', async () => {
    const { execute } = useBatchPreTranslate()
    const rawEntries = [{ id: 'e1', entry: '测试', english: '' }]
    const translatedEntries = [{ id: 'e1', entry: '测试', english: 'Test' }]

    workbenchApi.getEntryInfoList
      .mockResolvedValueOnce({ data: { list: [] } }) // entryExamine 查询 0 条
      .mockResolvedValueOnce({ data: { list: rawEntries } }) // preTranslate 查询 1 条
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: { list: translatedEntries } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    const config = {
      tasks: [buildTask()],
      stages: { entryExamine: true, preTranslate: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    // 词条审核 0 条：query success，其余 skipped，阶段 success
    expect(progress.stages.entryExamine).toBe('success')
    expect(progress.steps.entryExamine.query).toBe('success')
    expect(progress.steps.entryExamine.selectAll).toBe('skipped')
    expect(progress.steps.entryExamine.batchApprove).toBe('skipped')
    expect(progress.steps.entryExamine.save).toBe('skipped')

    // 翻译阶段继续执行并成功
    expect(progress.stages.preTranslate).toBe('success')
    expect(progress.steps.preTranslate.save).toBe('success')

    // 两个阶段各查询一次
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(2)
  })

  it('三阶段查询口径：entryExamine 传空 transStates，preTranslate 传 [0,2]，translateExamine 传 [1]', async () => {
    const { execute } = useBatchPreTranslate()
    const rawEntries = [{ id: 'e1', entry: '测试', english: '' }]
    const translatedEntries = [{ id: 'e1', entry: '测试', english: 'Test' }]

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: rawEntries } })
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: { list: translatedEntries } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    const config = {
      tasks: [buildTask()],
      stages: { entryExamine: true, preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    // 词条审核阶段必须传空 transStates（后端走纯词条状态查询），传非空会因待审词条无翻译记录而恒 0 条
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(3)
    expect(workbenchApi.getEntryInfoList).toHaveBeenNthCalledWith(
      1, { taskID: 'task-1', entryState: '1', entry: '' }, []
    )
    // 翻译阶段需覆盖未翻译(0)与翻译被驳回需重译(2)，与流程节点角标口径一致
    expect(workbenchApi.getEntryInfoList).toHaveBeenNthCalledWith(
      2, { taskID: 'task-1', entryState: '3', entry: '' }, ['0', '2']
    )
    expect(workbenchApi.getEntryInfoList).toHaveBeenNthCalledWith(
      3, { taskID: 'task-1', entryState: '3', entry: '' }, ['1']
    )
  })

  it('查询返回结构异常（list 非数组）：query 步骤 failed、阶段 failed，不得静默按 0 条跳过', async () => {
    const { execute } = useBatchPreTranslate()
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: null } })

    const config = {
      tasks: [buildTask()],
      stages: { entryExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.entryExamine).toBe('failed')
    expect(progress.steps.entryExamine.query).toBe('failed')
    expect(progress.error).toBe('词条审核阶段失败，并达到重复上限')
    // 异常响应不是"没有词条"：后续子步骤保持 pending，而不是 ⊘ skipped
    expect(progress.steps.entryExamine.selectAll).toBe('pending')
    expect(progress.steps.entryExamine.save).toBe('pending')
  })

  it('query 子步骤文案统一为「查询词条」', () => {
    for (const stage of STAGE_ORDER) {
      const queryStep = STAGE_STEPS[stage].find(s => s.key === 'query')
      expect(queryStep).toBeDefined()
      expect(queryStep.label).toBe('查询词条')
    }
  })

  it('并发数 2 + 两任务：两任务都完整执行，各自保存成功', async () => {
    const { execute } = useBatchPreTranslate()
    const rawEntries = [{ id: 'e1', entry: '测试', english: '' }]
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: rawEntries } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    // 两个任务的进度对象（模拟 store START 按任务数组初始化）
    mockStore.state.batchProgress.progresses = [buildMockProgress(), {
      ...buildMockProgress(), taskId: 'task-2', taskName: 'Task 2'
    }]

    const config = {
      tasks: [
        buildTask(),
        buildTask({ id: 'task-2', name: 'Task 2' })
      ],
      stages: { entryExamine: true },
      concurrency: 2,
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    // 两任务均执行了 查询→全选→批量通过→保存，阶段 success
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(2)
    expect(workbenchApi.updateEntryList).toHaveBeenCalledTimes(2)
    for (const p of mockStore.state.batchProgress.progresses) {
      expect(p.stages.entryExamine).toBe('success')
      expect(p.steps.entryExamine.save).toBe('success')
      expect(p.error).toBeNull()
    }
    // 各自的查询带各自的 taskID
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledWith(
      { taskID: 'task-1', entryState: '1', entry: '' }, []
    )
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledWith(
      { taskID: 'task-2', entryState: '1', entry: '' }, []
    )
  })

  it('并发数 2：任务 A 失败不影响任务 B 成功（互不干扰）', async () => {
    const { execute } = useBatchPreTranslate()
    const rawEntries = [{ id: 'e1', entry: '测试', english: '' }]
    // 按 taskID 区分：task-1 查询恒失败，task-2 正常
    workbenchApi.getEntryInfoList.mockImplementation((params) => {
      if (params.taskID === 'task-1') return Promise.reject(new Error('Network error'))
      return Promise.resolve({ data: { list: rawEntries } })
    })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    mockStore.state.batchProgress.progresses = [buildMockProgress(), {
      ...buildMockProgress(), taskId: 'task-2', taskName: 'Task 2'
    }]

    const config = {
      tasks: [
        buildTask(),
        buildTask({ id: 'task-2', name: 'Task 2' })
      ],
      stages: { entryExamine: true },
      concurrency: 2,
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const [p1, p2] = mockStore.state.batchProgress.progresses
    expect(p1.stages.entryExamine).toBe('failed')
    expect(p1.steps.entryExamine.query).toBe('failed')
    expect(p1.error).toBe('词条审核阶段失败，并达到重复上限')
    expect(p2.stages.entryExamine).toBe('success')
    expect(p2.steps.entryExamine.save).toBe('success')
    expect(p2.error).toBeNull()
    // 失败任务也保存了进度状态（phase complete 在 finally 中触发）
    expect(mockStore.dispatch).toHaveBeenCalledWith('batchProgress/complete')
  })

  it('中段无权限（翻译员非本人）：词条审核与翻译审核独立执行，翻译阶段跳过', async () => {
    const { execute } = useBatchPreTranslate()
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: [] } })

    const config = {
      tasks: [buildTask({ translator: 'otherUser' })],
      stages: { entryExamine: true, preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    // 词条审核、翻译审核正常执行（0 条 → success）
    expect(progress.stages.entryExamine).toBe('success')
    expect(progress.stages.translateExamine).toBe('success')
    // 翻译阶段因无权限跳过
    expect(progress.stages.preTranslate).toBe('skipped')
    for (const step of STAGE_STEPS.preTranslate) {
      expect(progress.steps.preTranslate[step.key]).toBe('skipped')
    }
    expect(progress.stageCounts.preTranslate).toEqual({ current: 0, total: 0 })
    expect(progress.stageMessages.preTranslate).toContain('翻译员')
    expect(progress.stageMessages.preTranslate).toContain('otherUser')
    expect(progress.error).toBeNull()

    // 跳过阶段不触发后端：仅词条审核 + 翻译审核各查询一次
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(2)
    expect(workbenchApi.preTranslate).not.toHaveBeenCalled()
    expect(workbenchApi.updateEntryList).not.toHaveBeenCalled()
  })

  it('首阶段无权限、后续有权限：首阶段跳过，后续阶段独立执行', async () => {
    const { execute } = useBatchPreTranslate()
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: [] } })

    const config = {
      tasks: [buildTask({ entryAuditor: 'otherUser' })],
      stages: { entryExamine: true, preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.entryExamine).toBe('skipped')
    expect(progress.stages.preTranslate).toBe('success')
    expect(progress.stages.translateExamine).toBe('success')
    expect(progress.stageMessages.entryExamine).toContain('词条审核员')
    expect(progress.error).toBeNull()
    // 词条审核跳过未查询，翻译与翻译审核各查询一次
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(2)
  })

  it('指派字段为空：该阶段跳过', async () => {
    const { execute } = useBatchPreTranslate()
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: [] } })

    const config = {
      tasks: [buildTask({ translator: '' })],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.preTranslate).toBe('skipped')
    expect(progress.stageMessages.preTranslate).toContain('未指定')
    expect(progress.stageMessages.preTranslate).toContain('翻译员')
    expect(progress.error).toBeNull()
    expect(workbenchApi.getEntryInfoList).not.toHaveBeenCalled()
  })

  it('user 为空：全部启用阶段跳过，无任何后端调用', async () => {
    const { execute } = useBatchPreTranslate()
    mockStore.state.user = null
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: [] } })

    const config = {
      tasks: [buildTask()],
      stages: { entryExamine: true, preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }

    await execute(config, mockStore)

    const progress = mockStore.state.batchProgress.progresses[0]
    expect(progress.stages.entryExamine).toBe('skipped')
    expect(progress.stages.preTranslate).toBe('skipped')
    expect(progress.stages.translateExamine).toBe('skipped')
    expect(progress.error).toBeNull()
    expect(workbenchApi.getEntryInfoList).not.toHaveBeenCalled()
    expect(workbenchApi.preTranslate).not.toHaveBeenCalled()
    expect(workbenchApi.updateEntryList).not.toHaveBeenCalled()
  })

  it('未启用 enhancedSplit：preTranslate 只调用一次', async () => {
    const { execute } = useBatchPreTranslate()
    const entries = Array.from({ length: 1500 }, (_, i) => ({
      id: `e${i}`, entry: `词条${i}`, english: ''
    }))
    const translated = entries.map(e => ({ ...e, english: 'Test' }))

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: entries } })
    workbenchApi.preTranslate.mockResolvedValue({ code: 200, data: { list: translated } })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    await execute({
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      enhancedSplit: false,
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }, mockStore)

    expect(workbenchApi.preTranslate).toHaveBeenCalledTimes(1)
    expect(workbenchApi.preTranslate.mock.calls[0][1]).toHaveLength(1500)
  })

  it('启用 enhancedSplit + 1600 条 + limit 1000：preTranslate 调用 2 次，body 1000+600', async () => {
    const { execute } = useBatchPreTranslate()
    const entries = Array.from({ length: 1600 }, (_, i) => ({
      id: `e${i}`, entry: `词条${i}`, english: ''
    }))

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: entries } })
    workbenchApi.preTranslate.mockImplementation((_params, body) => {
      const list = body.map(e => ({ ...e, english: 'Test' }))
      return Promise.resolve({ code: 200, data: { list } })
    })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    await execute({
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      enhancedSplit: true,
      splitLimit: 1000,
      concurrency: 2,
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }, mockStore)

    expect(workbenchApi.preTranslate).toHaveBeenCalledTimes(2)
    expect(workbenchApi.preTranslate.mock.calls[0][1]).toHaveLength(1000)
    expect(workbenchApi.preTranslate.mock.calls[1][1]).toHaveLength(600)

    const progress = mockStore.state.batchProgress.progresses.find(p => p.taskId === 'task-1')
    expect(progress.stages.preTranslate).toBe('success')
    expect(progress.stageCounts.preTranslate).toEqual({ current: 1600, total: 1600 })
    expect(progress.stepCounts.preTranslate.preTranslate).toBe(1600)
    // 子块执行完毕后应清理
    expect(mockStore.state.batchProgress.progresses.some(p => p.parentTaskId === 'task-1')).toBe(false)
  })

  it('拆分增强 0 条：不创建子块', async () => {
    const { execute } = useBatchPreTranslate()
    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: [] } })

    await execute({
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      enhancedSplit: true,
      splitLimit: 1000,
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }, mockStore)

    expect(mockStore.state.batchProgress.progresses.filter(p => p.parentTaskId).length).toBe(0)
  })

  it('拆分子块失败重试耗尽：主任务 preTranslate stage = failed', async () => {
    const { execute } = useBatchPreTranslate()
    const entries = Array.from({ length: 1200 }, (_, i) => ({
      id: `e${i}`, entry: `词条${i}`, english: ''
    }))

    workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: entries } })
    workbenchApi.preTranslate.mockImplementation((_params, body) => {
      if (body[0].id === 'e0') return Promise.reject(new Error('chunk fail'))
      const list = body.map(e => ({ ...e, english: 'Test' }))
      return Promise.resolve({ code: 200, data: { list } })
    })

    await execute({
      tasks: [buildTask()],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      enhancedSplit: true,
      splitLimit: 1000,
      concurrency: 2,
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }, mockStore)

    const progress = mockStore.state.batchProgress.progresses.find(p => p.taskId === 'task-1')
    expect(progress.stages.preTranslate).toBe('failed')
    expect(progress.error).toContain('翻译阶段失败')
  })

  it('全局并发：A 拆 4 块 + B 未拆，concurrency=2 时 in-flight 不超过 2', async () => {
    const { execute } = useBatchPreTranslate()
    let inFlight = 0
    let maxInFlight = 0

    const makeEntries = (n, prefix) => Array.from({ length: n }, (_, i) => ({
      id: `${prefix}${i}`, entry: `词条${i}`, english: ''
    }))

    workbenchApi.getEntryInfoList.mockImplementation((params) => {
      if (params.taskID === 'task-1') {
        return Promise.resolve({ data: { list: makeEntries(800, 'a') } })
      }
      return Promise.resolve({ data: { list: makeEntries(1, 'b') } })
    })

    workbenchApi.preTranslate.mockImplementation(async (_params, body) => {
      inFlight++
      maxInFlight = Math.max(maxInFlight, inFlight)
      await new Promise(r => setTimeout(r, 20))
      inFlight--
      const list = body.map(e => ({ ...e, english: 'Test' }))
      return { code: 200, data: { list } }
    })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    mockStore.state.batchProgress.progresses = [
      buildMockProgress(),
      buildMockProgress({ taskId: 'task-2', taskName: 'Task 2' })
    ]

    await execute({
      tasks: [
        buildTask(),
        buildTask({ id: 'task-2', name: 'Task 2' })
      ],
      stages: { preTranslate: true },
      translatePriority: 'shuyuku',
      enhancedSplit: true,
      splitLimit: 200,
      concurrency: 2,
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }, mockStore)

    expect(maxInFlight).toBeLessThanOrEqual(2)
    expect(workbenchApi.preTranslate.mock.calls.length).toBeGreaterThan(1)
  })

  it('拆分贯通：翻译+翻译审核，每块译完即审，主任务再独立 query 审核', async () => {
    const { execute } = useBatchPreTranslate()
    const translateEntries = Array.from({ length: 1200 }, (_, i) => ({
      id: `e${i}`, entry: `词条${i}`, english: ''
    }))

    workbenchApi.getEntryInfoList.mockImplementation((_params, transStates) => {
      // 翻译阶段 transStates=['0','2']；翻译审核 ['1']
      if (Array.isArray(transStates) && transStates.includes('0')) {
        return Promise.resolve({ data: { list: translateEntries } })
      }
      // 主任务二次审核：贯通已审完，剩余 0
      return Promise.resolve({ data: { list: [] } })
    })
    workbenchApi.preTranslate.mockImplementation((_params, body) => {
      const list = body.map(e => ({ ...e, english: 'Test' }))
      return Promise.resolve({ code: 200, data: { list } })
    })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    await execute({
      tasks: [buildTask()],
      stages: { preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      enhancedSplit: true,
      splitLimit: 1000,
      concurrency: 2,
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }, mockStore)

    // 2 块 preTranslate
    expect(workbenchApi.preTranslate).toHaveBeenCalledTimes(2)
    // 每块：翻译保存 + 贯通审核保存 = 4；主任务二次审核 0 条不保存
    expect(workbenchApi.updateEntryList).toHaveBeenCalledTimes(4)
    // 翻译 query + 主任务审核 query
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(2)

    const progress = mockStore.state.batchProgress.progresses.find(p => p.taskId === 'task-1')
    expect(progress.stages.preTranslate).toBe('success')
    // 二次审核 query 0 条 → success（跳过剩余步骤）
    expect(progress.stages.translateExamine).toBe('success')
    expect(progress.steps.translateExamine.query).toBe('success')
  })

  it('拆分贯通后主任务二次审核再拆分：翻译 1200 + 审核残留 2000', async () => {
    const { execute } = useBatchPreTranslate()
    const translateEntries = Array.from({ length: 1200 }, (_, i) => ({
      id: `t${i}`, entry: `译${i}`, english: ''
    }))
    const examineLeft = Array.from({ length: 2000 }, (_, i) => ({
      id: `r${i}`, entry: `审${i}`, english: 'Pending'
    }))

    workbenchApi.getEntryInfoList.mockImplementation((_params, transStates) => {
      if (Array.isArray(transStates) && transStates.includes('0')) {
        return Promise.resolve({ data: { list: translateEntries } })
      }
      return Promise.resolve({ data: { list: examineLeft } })
    })
    workbenchApi.preTranslate.mockImplementation((_params, body) => {
      const list = body.map(e => ({ ...e, english: 'Test' }))
      return Promise.resolve({ code: 200, data: { list } })
    })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    await execute({
      tasks: [buildTask()],
      stages: { preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      enhancedSplit: true,
      splitLimit: 1000,
      concurrency: 2,
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }, mockStore)

    expect(workbenchApi.preTranslate).toHaveBeenCalledTimes(2)
    // 贯通：2 翻译保存 + 2 审核保存；二次审核拆 2 块：2 审核保存 = 6
    expect(workbenchApi.updateEntryList).toHaveBeenCalledTimes(6)
    expect(workbenchApi.getEntryInfoList).toHaveBeenCalledTimes(2)

    const progress = mockStore.state.batchProgress.progresses.find(p => p.taskId === 'task-1')
    expect(progress.stages.preTranslate).toBe('success')
    expect(progress.stages.translateExamine).toBe('success')
    expect(progress.stageCounts.translateExamine).toEqual({ current: 2000, total: 2000 })
  })

  it('翻译部分 warning：贯通审核只处理已保存子集', async () => {
    const { execute } = useBatchPreTranslate()
    const entries = Array.from({ length: 1200 }, (_, i) => ({
      id: `e${i}`, entry: `词条${i}`, english: ''
    }))

    workbenchApi.getEntryInfoList.mockImplementation((_params, transStates) => {
      if (Array.isArray(transStates) && transStates.includes('0')) {
        return Promise.resolve({ data: { list: entries } })
      }
      return Promise.resolve({ data: { list: [] } })
    })
    workbenchApi.preTranslate.mockImplementation((_params, body) => {
      // 第一块：一半无译文
      const list = body.map((e, i) => ({
        ...e,
        english: i < body.length / 2 ? 'Test' : ''
      }))
      return Promise.resolve({ code: 200, data: { list } })
    })
    workbenchApi.updateEntryList.mockResolvedValue({ code: 200 })

    await execute({
      tasks: [buildTask()],
      stages: { preTranslate: true, translateExamine: true },
      translatePriority: 'shuyuku',
      enhancedSplit: true,
      splitLimit: 1000,
      concurrency: 1,
      maxRetries: 1,
      rules: [],
      stepDelayMs: 0
    }, mockStore)

    // 翻译保存次数：有译文的块仍会 save；贯通审核只对 savedEntries
    const saveCalls = workbenchApi.updateEntryList.mock.calls
    // 至少有翻译保存与审核保存；审核保存条数应小于等于翻译保存条数
    const examineSaves = saveCalls.filter(c =>
      Array.isArray(c[1]) && c[1].some(e => e.englishTranslateState === '3')
    )
    const translateSaves = saveCalls.filter(c =>
      Array.isArray(c[1]) && c[1].some(e => e.englishTranslateState === '1')
    )
    expect(translateSaves.length).toBeGreaterThan(0)
    expect(examineSaves.length).toBeGreaterThan(0)
    const examineCount = examineSaves.reduce((n, c) => n + c[1].length, 0)
    const translateCount = translateSaves.reduce((n, c) => n + c[1].length, 0)
    expect(examineCount).toBe(translateCount)

    const progress = mockStore.state.batchProgress.progresses.find(p => p.taskId === 'task-1')
    expect(progress.stages.preTranslate).toBe('warning')
  })

  describe('archive 阶段', () => {
    function archiveProgress() {
      return buildMockProgress({
        stages: {
          entryExamine: 'skipped',
          preTranslate: 'skipped',
          translateExamine: 'skipped',
          archive: 'pending'
        }
      })
    }

    beforeEach(() => {
      const batchState = createMockBatchProgressState([archiveProgress()])
      mockStore.state.batchProgress = batchState
      mockStore.dispatch = vi.fn((action, payload) => {
        dispatchBatchProgress(batchState, action, payload)
      })
      i18ServerApi.setInfo.mockResolvedValue({ code: 200 })
      taskApi.updateTaskInfo.mockResolvedValue({ code: 200 })
    })

    it('entriesDone：全部完成返回 true，否则 false', () => {
      const { entriesDone } = useBatchPreTranslate()
      expect(entriesDone([], 'englishTranslateState')).toBe(true)
      expect(entriesDone([
        { entryState: 3, englishTranslateState: 3 }
      ], 'englishTranslateState')).toBe(true)
      expect(entriesDone([
        { entryState: 3, englishTranslateState: 1 }
      ], 'englishTranslateState')).toBe(false)
    })

    it('write：全量 setInfo，不调用 updateTaskInfo', async () => {
      const { execute } = useBatchPreTranslate()
      const entries = [
        { id: 'e1', entryState: 3, englishTranslateState: 3 },
        { id: 'e2', entryState: 3, englishTranslateState: 3 }
      ]
      workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: entries } })

      await execute({
        tasks: [buildTask()],
        stages: { archive: true },
        archiveIp: '10.0.0.1',
        archiveMode: ARCHIVE_MODE.WRITE,
        maxRetries: 1,
        stepDelayMs: 0
      }, mockStore)

      expect(i18ServerApi.setInfo).toHaveBeenCalledTimes(1)
      expect(i18ServerApi.setInfo.mock.calls[0][0]).toMatchObject({
        taskID: 'task-1',
        i18nUrl: '10.0.0.1'
      })
      expect(i18ServerApi.setInfo.mock.calls[0][1]).toHaveLength(2)
      expect(taskApi.updateTaskInfo).not.toHaveBeenCalled()

      const progress = mockStore.state.batchProgress.progresses[0]
      expect(progress.stages.archive).toBe('success')
      expect(progress.steps.archive.writeBack).toBe('success')
      expect(progress.steps.archive.endTask).toBe('skipped')
      expect(workbenchApi.getEntryInfoList.mock.calls[0][0]).toEqual({ taskID: 'task-1', entry: '' })
    })

    it('writeEnd + 未完成词条：跳过，不回写不结束', async () => {
      const { execute } = useBatchPreTranslate()
      workbenchApi.getEntryInfoList.mockResolvedValue({
        data: { list: [{ id: 'e1', entryState: 3, englishTranslateState: 1 }] }
      })

      await execute({
        tasks: [buildTask()],
        stages: { archive: true },
        archiveIp: '10.0.0.1',
        archiveMode: ARCHIVE_MODE.WRITE_END,
        maxRetries: 1,
        stepDelayMs: 0
      }, mockStore)

      expect(i18ServerApi.setInfo).not.toHaveBeenCalled()
      expect(taskApi.updateTaskInfo).not.toHaveBeenCalled()
      const progress = mockStore.state.batchProgress.progresses[0]
      expect(progress.stages.archive).toBe('skipped')
      expect(progress.stageMessages.archive).toContain('未处理完')
    })

    it('writeEnd + 全部完成：setInfo 后 updateTaskInfo(state=6)', async () => {
      const { execute } = useBatchPreTranslate()
      const task = buildTask({ state: '5' })
      workbenchApi.getEntryInfoList.mockResolvedValue({
        data: { list: [{ id: 'e1', entryState: 3, englishTranslateState: 3 }] }
      })

      await execute({
        tasks: [task],
        stages: { archive: true },
        archiveIp: '10.0.0.1',
        archiveMode: ARCHIVE_MODE.WRITE_END,
        maxRetries: 1,
        stepDelayMs: 0
      }, mockStore)

      expect(i18ServerApi.setInfo).toHaveBeenCalledTimes(1)
      expect(taskApi.updateTaskInfo).toHaveBeenCalledTimes(1)
      expect(taskApi.updateTaskInfo.mock.calls[0][0].state).toBe('6')
      expect(task.state).toBe('6')

      const progress = mockStore.state.batchProgress.progresses[0]
      expect(progress.stages.archive).toBe('success')
      expect(progress.steps.archive.endTask).toBe('success')
    })

    it('非 creator：权限跳过归档', async () => {
      const { execute } = useBatchPreTranslate()
      workbenchApi.getEntryInfoList.mockResolvedValue({ data: { list: [{ id: 'e1' }] } })

      await execute({
        tasks: [buildTask({ creator: 'otherAdmin' })],
        stages: { archive: true },
        archiveIp: '10.0.0.1',
        archiveMode: ARCHIVE_MODE.WRITE,
        maxRetries: 1,
        stepDelayMs: 0
      }, mockStore)

      expect(i18ServerApi.setInfo).not.toHaveBeenCalled()
      expect(workbenchApi.getEntryInfoList).not.toHaveBeenCalled()
      const progress = mockStore.state.batchProgress.progresses[0]
      expect(progress.stages.archive).toBe('skipped')
      expect(progress.stageMessages.archive).toContain('任务管理员')
    })

    it('只勾归档：isContinuous 为 true', () => {
      const { isContinuous } = useBatchPreTranslate()
      expect(isContinuous({ archive: true })).toBe(true)
    })
  })
})
