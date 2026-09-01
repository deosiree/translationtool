import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useBatchPreTranslate } from '@/composables/workbench/useBatchPreTranslate'
import { STAGE_ORDER, STAGE_STEPS } from '@/constants/batchPreTranslateSteps'
import * as workbenchApi from '@/http/api/workbench'

vi.mock('@/http/api/workbench', () => ({
  getEntryInfoList: vi.fn(),
  updateEntryList: vi.fn(),
  preTranslate: vi.fn()
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
function buildMockProgress() {
  const { steps, stepCounts } = buildInitialStepState()
  return {
    taskId: 'task-1',
    taskName: 'Task 1',
    stages: { entryExamine: 'pending', preTranslate: 'pending', translateExamine: 'pending' },
    stageCounts: {
      entryExamine: { current: 0, total: 0 },
      preTranslate: { current: 0, total: 0 },
      translateExamine: { current: 0, total: 0 }
    },
    steps,
    stepCounts,
    currentStage: null,
    currentStep: null,
    error: null,
    retryCount: 0
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
    ...overrides
  }
}

describe('useBatchPreTranslate', () => {
  let mockStore

  beforeEach(() => {
    vi.clearAllMocks()
    mockStore = {
      state: {
        user: { userName: CURRENT_USER, roleName: '翻译员' },
        batchProgress: {
          progresses: [buildMockProgress()]
        }
      },
      dispatch: vi.fn((action, payload) => {
        if (action === 'batchProgress/updateProgress') {
          const index = mockStore.state.batchProgress.progresses.findIndex(p => p.taskId === payload.taskId)
          if (index !== -1) {
            mockStore.state.batchProgress.progresses[index] = { ...payload }
          }
        }
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

    // 每个阶段：query 子步骤 success，其余子步骤 skipped
    for (const stage of STAGE_ORDER) {
      expect(progress.steps[stage].query).toBe('success')
      for (const step of STAGE_STEPS[stage]) {
        if (step.key === 'query') continue
        expect(progress.steps[stage][step.key]).toBe('skipped')
      }
    }

    // 每个阶段都独立查询了一次
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
})
