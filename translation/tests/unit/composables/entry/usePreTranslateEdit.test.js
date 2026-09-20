/**
 * usePreTranslateEdit 编排器单测（纯 vm 模式，mock API 层）
 * special 本地 checkPlace，不再调 checkSykEntryBeforeSave
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warn: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
  notification: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('@/http/api/entryManage', () => ({
  preTranslateEntry: vi.fn(),
  updateEntryInfoList: vi.fn(),
}))

import { preTranslateEntry, updateEntryInfoList } from '@/http/api/entryManage'
import { message, notification } from 'ant-design-vue'
import { usePreTranslateEdit } from '@/views/entry/composables/usePreTranslateEdit'
import { confirmRow } from '@/views/entry/composables/useSaveFlow'

const { execute, cancelAll, save, discardRow } = usePreTranslateEdit()

function batchUpdateRes(list, code = 200) {
  return {
    code,
    type: code === 200 ? 'OK' : 'INTERNAL_ERROR',
    data: { list, totalNum: list.length },
    message: '更新完成',
  }
}

function buildVm(entries, { classifyLimit = {} } = {}) {
  return {
    dataSource: entries,
    classifyLimit,
    editableData: {},
    rules: {},
    cellErrors: {},
    columns: [],
    reviewActive: false,
    preTranslateSnapshot: null,
    fieldsNeedSave: ['entry', 'english', 'russian'],
    _pendingOk: null,
    rulesOptions: [
      { key: 'toLong', checked: false },
      { key: 'special', checked: false },
    ],
  }
}

function entry(overrides = {}) {
  return {
    id: 'e1',
    entry: '断路器',
    english: '',
    russian: '',
    maxLength: 100,
    classfy1: null,
    ...overrides,
  }
}

function preTranslateResponse(langCol, rows) {
  return {
    code: 200,
    data: {
      list: rows.map((r) => ({ id: r.id, [langCol]: r[langCol] })),
    },
  }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('usePreTranslateEdit - execute', () => {
  it('多语种并行：逐语种调用接口，响应合并后写入对应列（浏览态）', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockImplementation((params) => {
      if (params.translateType === '英文') {
        return Promise.resolve(preTranslateResponse('english', [{ id: 'e1', english: 'circuit breaker' }]))
      }
      if (params.translateType === '俄文') {
        return Promise.resolve(preTranslateResponse('russian', [{ id: 'e1', russian: 'выключатель' }]))
      }
      return Promise.resolve(preTranslateResponse('english', []))
    })

    await execute(vm, {
      language: ['英文', '俄文'],
      priority: 'shuyuku',
      verifyMethods: [],
    })

    expect(preTranslateEntry).toHaveBeenCalledTimes(2)
    expect(entries[0].english).toBe('circuit breaker')
    expect(entries[0].russian).toBe('выключатель')
    expect(vm.editableData.e1).toBeUndefined()
    expect(vm.reviewActive).toBe(true)
  })

  it('toLong 超长：进编辑态打红字', async () => {
    const entries = [entry({ maxLength: 2 })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'long long text' }])
    )

    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['toLong'] })

    expect(vm.editableData.e1?.english).toBe('long long text')
    expect(vm.cellErrors.e1?.english).toContain('允许最大字符数为2')
    expect(entries[0].english).toBe('')
  })

  it('special 本地失败：译文进编辑态并打特殊字符红字；通过行写入列', async () => {
    const entries = [entry({ id: 'e1' }), entry({ id: 'e2', entry: '隔离开关' })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [
        { id: 'e1', english: 'breaker %1' },
        { id: 'e2', english: 'disconnector' },
      ])
    )

    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['special'] })

    expect(vm.editableData.e1?.english).toBe('breaker %1')
    expect(vm.cellErrors.e1?.english).toContain('特殊字符不一致')
    expect(entries[0].english).toBe('')
    expect(entries[1].english).toBe('disconnector')
    expect(vm.editableData.e2).toBeUndefined()
  })

  it('空白译文：跳过不动', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: '   ' }])
    )

    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })

    expect(entries[0].english).toBe('')
    expect(vm.editableData.e1).toBeUndefined()
  })
})

describe('usePreTranslateEdit - cancelAll', () => {
  it('取消：dataSource 恢复快照，清编辑态/红字，退出处理态', async () => {
    const entries = [entry({ english: 'old value' })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'new value' }])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })
    expect(entries[0].english).toBe('new value')

    const restored = cancelAll(vm)

    expect(restored[0].english).toBe('old value')
    expect(vm.reviewActive).toBe(false)
    expect(vm.preTranslateSnapshot).toBeNull()
    expect(vm.editableData).toEqual({})
    expect(vm.cellErrors).toEqual({})
  })
})

describe('usePreTranslateEdit - save', () => {
  it('保存：改动行一次 update，成功行移除', async () => {
    const entries = [entry({ id: 'e1' })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'saved value' }])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })

    updateEntryInfoList.mockResolvedValue(
      batchUpdateRes([{ id: 'e1', success: true, message: 'OK' }])
    )
    const result = await save(vm)

    expect(updateEntryInfoList).toHaveBeenCalledTimes(1)
    const [dataArg, paramsArg] = updateEntryInfoList.mock.calls[0]
    expect(dataArg[0].english).toBe('saved value')
    expect(paramsArg).toEqual({ notes: '批量编辑' })
    expect(result.allPassed).toBe(true)
    expect(result.savedRecords).toHaveLength(1)
    expect(result.remainingCount).toBe(0)
    expect(vm.reviewActive).toBe(false)
  })

  it('无改动：不调保存接口，退出处理态', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockRejectedValue(new Error('网络错误'))
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })

    const result = await save(vm)

    expect(updateEntryInfoList).not.toHaveBeenCalled()
    expect(result.savedRecords).toHaveLength(0)
    expect(vm.reviewActive).toBe(false)
  })

  it('部分词条保存失败：失败行保留在 remaining，保持处理态', async () => {
    const entries = [entry({ id: 'e1', english: '' }), entry({ id: 'e2', english: '' })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [
        { id: 'e1', english: 'v1' },
        { id: 'e2', english: 'v2' },
      ])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })

    updateEntryInfoList.mockResolvedValue(
      batchUpdateRes(
        [
          { id: 'e1', success: true, message: 'OK' },
          { id: 'e2', success: false, message: '落库失败' },
        ],
        203
      )
    )
    const result = await save(vm)

    expect(result.savedRecords.map((r) => r.id)).toEqual(['e1'])
    expect(result.remaining.map((r) => r.id)).toEqual(['e2'])
    expect(vm.reviewActive).toBe(true)
    expect(vm.editableData.e2).toBeDefined()
    expect(vm.editableData.e2.english).toBe('v2')
  })

  it('存在编辑态校验失败行：不落库，allPassed=false', async () => {
    const entries = [entry({ id: 'e1', maxLength: 2 })]
    const vm = buildVm(entries)
    vm.rulesOptions = [
      { key: 'toLong', checked: true },
      { key: 'special', checked: false },
    ]
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'too long value' }])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['toLong'] })

    const result = await save(vm)

    expect(result.allPassed).toBe(false)
    expect(updateEntryInfoList).not.toHaveBeenCalled()
    expect(vm.editableData.e1).toBeDefined()
    expect(notification.error).toHaveBeenCalled()
  })
})

describe('confirmRow', () => {
  it('√：本地校验通过才退出编辑态', () => {
    const row = entry({ english: 'ok' })
    const vm = buildVm([row])
    vm.rulesOptions = [
      { key: 'toLong', checked: true },
      { key: 'special', checked: false },
    ]
    vm.editableData = { e1: { ...row, english: 'ok' } }

    expect(confirmRow(vm, row)).toBe(true)
    expect(vm.editableData.e1).toBeUndefined()
    expect(row.english).toBe('ok')
  })

  it('√：占位失败留下红字', () => {
    const row = entry({ entry: 'x%1', english: '' })
    const vm = buildVm([row])
    vm.rulesOptions = [
      { key: 'toLong', checked: false },
      { key: 'special', checked: true },
    ]
    vm.editableData = { e1: { ...row, english: 'no place' } }

    expect(confirmRow(vm, row)).toBe(false)
    expect(vm.editableData.e1).toBeDefined()
    expect(vm.cellErrors.e1.english).toContain('特殊字符不一致')
  })
})

describe('discardRow', () => {
  it('×：恢复快照列并退编辑', async () => {
    const entries = [entry({ english: 'old' })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'new' }])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['toLong'] })
    // force edit
    vm.editableData = { e1: { ...entries[0], english: 'draft' } }
    vm.preTranslateSnapshot = [{ id: 'e1', entry: '断路器', english: 'old', russian: '', maxLength: 100, classfy1: null }]

    discardRow(vm, entries[0])
    expect(vm.editableData.e1).toBeUndefined()
    expect(message.info).not.toHaveBeenCalled()
  })
})
