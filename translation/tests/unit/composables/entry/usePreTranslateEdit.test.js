/**
 * usePreTranslateEdit 编排器单测（纯 vm 模式，mock API 层）
 * 覆盖：快照/并行合并/校验驱动编辑态（toLong 超长红字、special 批量判定、空白译文跳过）/
 *       取消回滚/保存（批量 updateEntryInfoList、成功行移除、校验失败拦截）
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
  },
}))

vi.mock('@/http/api/entryManage', () => ({
  preTranslateEntry: vi.fn(),
  updateEntryInfoList: vi.fn(),
}))

vi.mock('@/http/api/glossary', () => ({
  checkSykEntryBeforeSave: vi.fn(),
}))

import { preTranslateEntry, updateEntryInfoList } from '@/http/api/entryManage'
import { checkSykEntryBeforeSave } from '@/http/api/glossary'
import { usePreTranslateEdit } from '@/composables/entry/usePreTranslateEdit'

const { execute, cancelAll, save, discardRow, confirmRow } = usePreTranslateEdit()

/** 构造批量更新成功响应 */
function batchUpdateRes(list, code = 200) {
  return {
    code,
    type: code === 200 ? 'OK' : 'INTERNAL_ERROR',
    data: { list, totalNum: list.length },
    message: '更新完成',
  }
}

/** 构造测试 vm（壳组件的编辑态相关状态子集） */
function buildVm(entries, { classifyLimit = {} } = {}) {
  return {
    dataSource: entries,
    classifyLimit,
    editableData: {},
    rules: {},
    cellErrors: {},
    columns: [],
    preTranslateActive: false,
    preTranslateSnapshot: null,
    fieldsNeedSave: ['entry', 'english', 'russian'],
    _pendingOk: null,
  }
}

/** 词条工厂：默认 100 上限，不受分类限制 */
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

/** mock 单语种预翻译响应 */
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
  checkSykEntryBeforeSave.mockResolvedValue({ data: [] })
})

describe('usePreTranslateEdit - execute', () => {
  it('多语种并行：逐语种调用接口，响应合并后写入对应列（浏览态）', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockImplementation((params) => {
      if (params.translateType === '英文') {
        return Promise.resolve(preTranslateResponse('english', [{ id: 'e1', english: 'circuit breaker' }]))
      }
      return Promise.resolve(preTranslateResponse('russian', [{ id: 'e1', russian: 'автоматический выключатель' }]))
    })

    await execute(vm, { language: ['英文', '俄文'], priority: 'shuyuku', verifyMethods: [] })

    // 两个语种各调用一次，query 参数正确
    expect(preTranslateEntry).toHaveBeenCalledTimes(2)
    const calls = preTranslateEntry.mock.calls // [params, data] 元组数组
    expect(calls.map((c) => c[0].translateType).sort()).toEqual(['俄文', '英文'])
    expect(calls.every((c) => c[0].priority === 'shuyuku')).toBe(true)
    // data 均为已选词条数组
    expect(calls.every((c) => Array.isArray(c[1]) && c[1].length === 1)).toBe(true)

    // 译文写入对应语种列，行保持浏览态
    expect(entries[0].english).toBe('circuit breaker')
    expect(entries[0].russian).toBe('автоматический выключатель')
    expect(vm.editableData.e1).toBeUndefined()
    // 快照已保存 + 编辑模式激活
    expect(vm.preTranslateActive).toBe(true)
    expect(vm.preTranslateSnapshot).toHaveLength(1)
    expect(vm.preTranslateSnapshot[0].english).toBe('') // 快照是预翻译前的值
  })

  it('部分语种请求失败：失败语种汇总提示，成功语种正常落地', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockImplementation((params) => {
      if (params.translateType === '英文') {
        return Promise.resolve(preTranslateResponse('english', [{ id: 'e1', english: 'OK' }]))
      }
      return Promise.reject(new Error('后端异常'))
    })

    const result = await execute(vm, { language: ['英文', '俄文'], priority: 'shuyuku', verifyMethods: [] })

    expect(result.failedLanguages).toEqual(['俄文'])
    expect(result.successLanguages).toEqual(['英文'])
    expect(entries[0].english).toBe('OK')
    expect(entries[0].russian).toBe('')
  })

  it('toLong 校验失败：译文进编辑态并打超长红字，不写入 record', async () => {
    // 100 上限：7 个中文 = 14 字节不够超；直接用 maxLength: 2 的词条
    const entries = [entry({ maxLength: 2 })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'long long text' }])
    )

    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['toLong'] })

    // 超长：编辑态 + 红字，浏览值不变
    expect(vm.editableData.e1?.english).toBe('long long text')
    expect(vm.cellErrors.e1?.english).toContain('允许最大字符数为2')
    expect(entries[0].english).toBe('')
  })

  it('special 校验失败：译文进编辑态并打特殊字符红字；通过行写入列', async () => {
    const entries = [entry({ id: 'e1' }), entry({ id: 'e2', entry: '隔离开关' })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [
        { id: 'e1', english: 'breaker %1' },
        { id: 'e2', english: 'disconnector' },
      ])
    )
    // 后端判定 e1 特殊字符不通过
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'e1' }] })

    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['special'] })

    // e1：编辑态 + 红字
    expect(vm.editableData.e1?.english).toBe('breaker %1')
    expect(vm.cellErrors.e1?.english).toContain('特殊字符不一致')
    expect(entries[0].english).toBe('')
    // e2：通过写列
    expect(entries[1].english).toBe('disconnector')
    expect(vm.editableData.e2).toBeUndefined()
    // special 批量接口只调用一次（一次判多行）
    expect(checkSykEntryBeforeSave).toHaveBeenCalledTimes(1)
  })

  it('空白译文：跳过不动（无写入、无编辑态）', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: '   ' }])
    )

    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })

    expect(entries[0].english).toBe('')
    expect(vm.editableData.e1).toBeUndefined()
  })

  it('勾选了 special 才调 checkSykEntryBeforeSave；未勾选不调', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'breaker' }])
    )

    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['toLong'] })

    expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
    expect(entries[0].english).toBe('breaker')
  })
})

describe('usePreTranslateEdit - cancelAll', () => {
  it('取消：dataSource 恢复快照，清编辑态/红字/规则，退出编辑模式', async () => {
    const entries = [entry({ english: 'old value' })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'new value' }])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })
    expect(entries[0].english).toBe('new value')

    const restored = cancelAll(vm)

    // 返回的是快照（旧值）
    expect(restored[0].english).toBe('old value')
    expect(vm.preTranslateActive).toBe(false)
    expect(vm.preTranslateSnapshot).toBeNull()
    expect(vm.editableData).toEqual({})
    expect(vm.cellErrors).toEqual({})
    expect(vm.rules).toEqual({})
  })
})

describe('usePreTranslateEdit - save', () => {
  it('保存：改动行一次 updateEntryInfoList(notes=预翻译)，成功行移除，全部保存且清空则 remainingCount=0', async () => {
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
    // 传入的是已写入译文的词条数组 + notes
    const [dataArg, paramsArg] = updateEntryInfoList.mock.calls[0]
    expect(Array.isArray(dataArg)).toBe(true)
    expect(dataArg[0].english).toBe('saved value')
    expect(paramsArg).toEqual({ notes: '预翻译' })
    // 返回剩余 0（全部移除）
    expect(result.allPassed).toBe(true)
    expect(result.savedRecords).toHaveLength(1)
    expect(result.remainingCount).toBe(0)
    expect(result.remaining).toHaveLength(0)
    expect(vm.preTranslateActive).toBe(false)
  })

  it('无改动（如语种全失败）：不调保存接口，退出编辑模式', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockRejectedValue(new Error('网络错误'))
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })

    const result = await save(vm)

    expect(updateEntryInfoList).not.toHaveBeenCalled()
    expect(result.savedRecords).toHaveLength(0)
    expect(result.remainingCount).toBe(1)
    expect(vm.preTranslateActive).toBe(false)
  })

  it('部分词条保存失败：失败行保留在 remaining 中', async () => {
    const entries = [entry({ id: 'e1' }), entry({ id: 'e2' })]
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

    expect(updateEntryInfoList).toHaveBeenCalledTimes(1)
    expect(result.savedRecords.map((r) => r.id)).toEqual(['e1'])
    expect(result.remaining.map((r) => r.id)).toEqual(['e2'])
    expect(result.remainingCount).toBe(1)
  })

  it('存在编辑态校验失败行：不落库，红字保留，allPassed=false', async () => {
    // 构造超长失败场景
    const entries = [entry({ id: 'e1', maxLength: 2 })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'too long value' }])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['toLong'] })
    expect(vm.editableData.e1).toBeDefined() // 超长行在编辑态

    const result = await save(vm)

    expect(result.allPassed).toBe(false)
    expect(updateEntryInfoList).not.toHaveBeenCalled()
    expect(vm.editableData.e1).toBeDefined() // 校验失败保持编辑态
    expect(vm.cellErrors.e1?.english).toContain('允许最大字符数为2')
  })
})

describe('usePreTranslateEdit - 行内操作', () => {
  it('confirmRow：编辑行校验通过则提交进 record 并退出编辑态', async () => {
    const entries = [entry()]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'valid' }])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })
    // 手动把该行拉进编辑态（模拟用户双击修改）
    vm.editableData.e1 = { ...entries[0], english: 'edited value' }
    vm.rules.e1 = {
      english: [{ validator: async (rule, value) => { if (value === 'bad') throw new Error('bad'); } }],
    }

    const ok = await confirmRow(vm, entries[0])

    expect(ok).toBe(true)
    expect(entries[0].english).toBe('edited value')
    expect(vm.editableData.e1).toBeUndefined()
  })

  it('discardRow：丢弃该行译文，恢复快照原值并退出编辑态', async () => {
    const entries = [entry({ id: 'e1', english: 'origin' })]
    const vm = buildVm(entries)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'translated' }])
    )
    await execute(vm, { language: ['英文'], priority: 'shuyuku', verifyMethods: [] })
    // 长度失败进编辑态场景
    entries[0].maxLength = 2
    const entriesFail = [entry({ id: 'e1', english: 'origin', maxLength: 2 })]
    const vmFail = buildVm(entriesFail)
    preTranslateEntry.mockResolvedValue(
      preTranslateResponse('english', [{ id: 'e1', english: 'too long translated' }])
    )
    await execute(vmFail, { language: ['英文'], priority: 'shuyuku', verifyMethods: ['toLong'] })
    expect(vmFail.editableData.e1).toBeDefined()

    discardRow(vmFail, entriesFail[0])

    // 恢复快照原值
    expect(entriesFail[0].english).toBe('origin')
    expect(vmFail.editableData.e1).toBeUndefined()
  })
})
