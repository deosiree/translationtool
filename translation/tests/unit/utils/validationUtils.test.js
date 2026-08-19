import { describe, it, expect, vi, beforeEach } from 'vitest'
import { 
  byteLength, 
  getMaxLength, 
  setRefRules, 
  openSetEdit,
  useRefRules,
  validateEditableCell,
  setCellError,
  clearCellError,
  clearCellErrorsForRecords,
  onEditableCellInput,
  applyCell,
  verifyArray_workbench,
  verifyArray_workbench_page,
  isBlankTranslation,
  getMethods,
  classifyArr,
  openFailRows,
  revalidateLoaded,
  saveEdit,
  cancelEdit,
  showEditOperation,
} from '@/utils/validationUtils'
import { cloneDeep } from 'lodash'

// Mock API 调用
vi.mock('@/http/api/glossary', () => ({
  checkSykEntryBeforeSave: vi.fn()
}))

// Mock lodash
vi.mock('lodash', () => ({
  cloneDeep: vi.fn((obj) => JSON.parse(JSON.stringify(obj)))
}))

describe('validationUtils - 表单校验工具函数', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('byteLength', () => {
    it('应该正确计算中文字符的字节长度', () => {
      expect(byteLength('测试')).toBe(4) // 每个中文2字节
      expect(byteLength('hello')).toBe(5) // 每个英文1字节
      expect(byteLength('hello测试')).toBe(9) // 5 + 4
    })

    it('应该处理 null 和 undefined', () => {
      expect(byteLength(null)).toBe(0)
      expect(byteLength(undefined)).toBe(0)
    })

    it('应该去除首尾空格', () => {
      expect(byteLength('  test  ')).toBe(4)
    })

    it('应该处理数字类型', () => {
      expect(byteLength(123)).toBe(3)
    })

    it('应该处理混合字符', () => {
      // test = 4, 123 = 3, 测试 = 4 (每个中文2字节)
      expect(byteLength('test123测试')).toBe(11) // 4 + 3 + 4 = 11
    })
  })

  describe('getMaxLength', () => {
    it('应该从 classifyLimit 中获取最大长度', () => {
      const record = { classfy1: 'category1' }
      const vm = {
        // 提供 editableData 以匹配运行时 vm 接口（避免 vm.editableData 未定义时报错）
        editableData: {},
        classifyLimit: {
          category1: {
            foreignMaxByte: 100,
            maxByte: 50
          }
        }
      }

      expect(getMaxLength(record, vm, 'foreignMaxByte')).toBe(100)
      expect(getMaxLength(record, vm, 'maxByte')).toBe(50)
    })

    it('应该返回 record.maxLength 当没有分类时', () => {
      const record = { maxLength: 200 }
      // 提供 editableData 以匹配运行时 vm 接口（避免 vm.editableData 未定义时报错）
      const vm = { editableData: {} }

      expect(getMaxLength(record, vm)).toBe(200)
    })

    it('应该返回 null 当没有分类且没有 maxLength 时', () => {
      const record = {}
      const vm = { editableData: {} }

      expect(getMaxLength(record, vm)).toBeNull()
    })

    it('应该处理 classifyLimit 中不存在分类的情况', () => {
      const record = { classfy1: 'unknownCategory' }
      const vm = {
        // 提供 editableData 以匹配运行时 vm 接口（避免 vm.editableData 未定义时报错）
        editableData: {},
        classifyLimit: {
          category1: {
            foreignMaxByte: 100
          }
        }
      }

      expect(getMaxLength(record, vm, 'foreignMaxByte')).toBeNull()
    })
    it('编辑态存在时应以编辑态的分类限制为准（foreignMaxByte/maxByte）', () => {
      const record = { id: 'r1', classfy1: 'origCat', maxLength: 50 }
      const vm = {
        editableData: {
          r1: { id: 'r1', classfy1: 'editCat' } // 编辑态不同分类
        },
        classifyLimit: {
          editCat: { foreignMaxByte: 120, maxByte: 60 },
          origCat: { foreignMaxByte: 999, maxByte: 999 }
        }
      }

      expect(getMaxLength(record, vm, 'foreignMaxByte')).toBe(120)
      expect(getMaxLength(record, vm, 'maxByte')).toBe(60)
    })

    it('无编辑态时应以原始 record 的分类限制为准', () => {
      const record = { id: 'r2', classfy1: 'origCat' }
      const vm = {
        editableData: {},
        classifyLimit: {
          origCat: { foreignMaxByte: 88, maxByte: 44 }
        }
      }

      expect(getMaxLength(record, vm, 'foreignMaxByte')).toBe(88)
      expect(getMaxLength(record, vm, 'maxByte')).toBe(44)
    })

    it('无分类但有 record.maxLength 时应返回 record.maxLength', () => {
      const record = { id: 'r3', maxLength: 77 }
      const vm = { editableData: {} }

      expect(getMaxLength(record, vm)).toBe(77)
    })

    it('既无分类也无 maxLength 时应返回 null', () => {
      const record = { id: 'r4' }
      const vm = { editableData: {} }

      expect(getMaxLength(record, vm)).toBeNull()
    })

    it('分类存在但 classifyLimit 中缺失该分类时应返回 null', () => {
      const record = { id: 'r5', classfy1: 'missingCat' }
      const vm = { editableData: {}, classifyLimit: { otherCat: { foreignMaxByte: 10 } } }

      expect(getMaxLength(record, vm, 'foreignMaxByte')).toBeNull()
    })

    it('编辑态存在但编辑态没有 classfy1 且有 maxLength，应返回编辑态的 maxLength', () => {
      const record = { id: 'r6', classfy1: 'origCat', maxLength: 30 }
      const vm = {
        editableData: {
          r6: { id: 'r6', maxLength: 200 } // 编辑态修改了 maxLength 且没有 classfy1
        },
        classifyLimit: {
          origCat: { foreignMaxByte: 15, maxByte: 7 }
        }
      }

      expect(getMaxLength(record, vm)).toBe(200)
    })
  })

  describe('setRefRules', () => {
    it('entry 列在当前实现中不会设置表单规则（跳过）', () => {
      const vm = {
        rules: {}
      }
      const record = { id: 'test-id', entry: 'test' }
      const cols = ['entry']

      setRefRules(vm, record, cols)

      expect(vm.rules['test-id']).toBeDefined()
      // 代码实现中对 entry 字段有意跳过，因此应为 undefined
      expect(vm.rules['test-id'].entry).toBeUndefined()
    })

    it('应该为翻译列设置校验规则', () => {
      const vm = {
        rules: {}
      }
      const record = { id: 'test-id', english: 'test' }
      const cols = ['english']

      setRefRules(vm, record, cols)

      expect(vm.rules['test-id']).toBeDefined()
      expect(vm.rules['test-id'].english).toHaveLength(1)
      expect(vm.rules['test-id'].english[0].validator).toBeDefined()
    })

    it('应该为多列设置校验规则', () => {
      const vm = {
        rules: {}
      }
      const record = { id: 'test-id', entry: 'test', english: 'test', chinese: '测试' }
      const cols = ['entry', 'english', 'chinese']

      setRefRules(vm, record, cols)

      // entry 字段被跳过（不设置规则），翻译列仍应存在规则
      expect(vm.rules['test-id'].entry).toBeUndefined()
      expect(vm.rules['test-id'].english).toBeDefined()
      expect(vm.rules['test-id'].chinese).toBeDefined()
    })
  })

  describe('openSetEdit', () => {
    it('应该打开编辑态并设置校验规则', async () => {
      const vm = {
        editableData: {},
        rules: {}
      }
      const record = { id: 'test-id', entry: 'test', english: 'test' }
      const cols = ['entry', 'english']

      await openSetEdit(record, cols, vm)

      expect(vm.editableData['test-id']).toBeDefined()
      expect(vm.rules['test-id']).toBeDefined()
    })

    it('应该保留已有的编辑数据', async () => {
      const vm = {
        editableData: {
          'test-id': { id: 'test-id', entry: 'old', english: 'old' }
        },
        rules: {}
      }
      const record = { id: 'test-id', entry: 'new', english: 'new' }
      const cols = ['entry']

      await openSetEdit(record, cols, vm)

      expect(vm.editableData['test-id'].entry).toBe('old')
    })
  })

  describe('validateEditableCell', () => {
    it('无规则时应直接通过', async () => {
      const vm = {
        editableData: { r1: { english: 'ok' } },
        rules: { r1: {} },
      }
      await expect(validateEditableCell(vm, 'r1', 'english')).resolves.toBeUndefined()
    })

    it('required 失败时应 reject 结构化错误', async () => {
      const vm = {
        editableData: { r1: { english: '' } },
        rules: {
          r1: {
            english: [{ required: true, message: '请输入!' }],
          },
        },
      }
      await expect(validateEditableCell(vm, 'r1', 'english')).rejects.toMatchObject({
        errorMessage: '请输入!',
      })
    })

    it('validator 超长失败时应 reject errorMessage', async () => {
      const vm = {
        editableData: { r1: { english: 'x'.repeat(20) } },
        rules: {
          r1: {
            english: [
              {
                validator: async (_rule, value) => {
                  if (value.length > 10) {
                    return Promise.reject('允许最大字符数为10！')
                  }
                },
              },
            ],
          },
        },
      }
      await expect(validateEditableCell(vm, 'r1', 'english')).rejects.toMatchObject({
        errorMessage: '允许最大字符数为10！',
      })
    })
  })

  describe('setCellError / clearCellError', () => {
    it('应写入并清除单元格错误', () => {
      const vm = { cellErrors: {} }
      setCellError(vm, 'r1', 'english', '错误')
      expect(vm.cellErrors.r1.english).toBe('错误')
      clearCellError(vm, 'r1', 'english')
      expect(vm.cellErrors.r1).toBeUndefined()
    })
  })

  describe('onEditableCellInput', () => {
    it('值变化时应写入 editableData 并清除 cellError', () => {
      const vm = {
        editableData: { r1: { english: 'old' } },
        cellErrors: { r1: { english: '旧错误' } },
      }
      onEditableCellInput(vm, 'r1', 'english', 'new')
      expect(vm.editableData.r1.english).toBe('new')
      expect(vm.cellErrors.r1).toBeUndefined()
    })

    it('同值回放时不应清除 cellError', () => {
      const vm = {
        editableData: { r1: { english: 'bad' } },
        cellErrors: { r1: { english: '允许最大字符数为20' } },
      }
      onEditableCellInput(vm, 'r1', 'english', 'bad')
      expect(vm.editableData.r1.english).toBe('bad')
      expect(vm.cellErrors.r1.english).toBe('允许最大字符数为20')
    })
  })

  describe('clearCellErrorsForRecords', () => {
    it('应批量清除指定行错误', () => {
      const vm = {
        cellErrors: {
          r1: { english: 'e1' },
          r2: { english: 'e2' },
        },
      }
      clearCellErrorsForRecords(vm, ['r1'])
      expect(vm.cellErrors.r1).toBeUndefined()
      expect(vm.cellErrors.r2.english).toBe('e2')
    })
  })

  describe('applyCell', () => {
    it('校验失败应写入 cellErrors 且不 throw', async () => {
      const vm = {
        editableData: { r1: { english: '' } },
        rules: {
          r1: {
            english: [{ required: true, message: '请输入!' }],
          },
        },
        cellErrors: {},
      }
      await expect(
        applyCell(vm, 'r1', 'english')
      ).resolves.toBeUndefined()
      expect(vm.cellErrors.r1.english).toBe('请输入!')
    })

    it('校验通过应清除对应 cellError', async () => {
      const vm = {
        editableData: { r1: { english: 'ok' } },
        rules: { r1: {} },
        cellErrors: { r1: { english: '旧错误' } },
      }
      await applyCell(vm, 'r1', 'english')
      expect(vm.cellErrors.r1).toBeUndefined()
    })
  })

  describe('verifyArray_workbench', () => {
    it('长度失败行应 openSetEdit 并写入 cellErrors', async () => {
      const record = {
        id: 'r-long',
        entry: 'test',
        english: 'a'.repeat(30),
        maxLength: 20,
      }
      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [record],
        showEditOperation: vi.fn(),
      }
      const arr = await verifyArray_workbench(vm, [record], 'english', ['toLong'])
      expect(arr.errorIds.has('r-long')).toBe(true)
      expect(vm.editableData['r-long']).toBeDefined()
      expect(vm.cellErrors['r-long']?.english).toContain('允许最大字符数为20')
      expect(vm.showEditOperation).toHaveBeenCalled()
    })

    it('未传 verifyMethods 且 rulesOptions 关闭 special 时不调 API、不进编辑态', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'r-special' }] })

      const record = {
        id: 'r-special',
        entry: 'Press %1 to continue',
        english: 'Press % 1 to continue',
      }
      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [record],
        rulesOptions: [
          { key: 'special', checked: false },
          { key: 'toLong', checked: true },
        ],
        showEditOperation: vi.fn(),
      }
      const arr = await verifyArray_workbench(vm, [record], 'english')
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
      expect(arr.acceptIds.has('r-special')).toBe(true)
      expect(arr.errorIds.has('r-special')).toBe(false)
      expect(vm.editableData['r-special']).toBeUndefined()
      expect(vm.cellErrors['r-special']).toBeUndefined()
      expect(vm.showEditOperation).not.toHaveBeenCalled()
    })

    it('未传 methods 且 special 勾选时才调 API', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'r-special' }] })

      const record = {
        id: 'r-special',
        entry: 'Press %1 to continue',
        english: 'Press % 1 to continue',
        maxLength: 200,
      }
      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [record],
        columns: [{ dataIndex: 'english' }],
        rulesOptions: [
          { key: 'special', checked: true },
          { key: 'toLong', checked: true },
        ],
        showEditOperation: vi.fn(),
      }
      const arr = await verifyArray_workbench(vm, [record], 'english')
      expect(checkSykEntryBeforeSave).toHaveBeenCalledTimes(1)
      expect(arr.specialIds.has('r-special')).toBe(true)
      expect(arr.errorIds.has('r-special')).toBe(true)
      expect(vm.cellErrors['r-special']?.english).toContain('特殊字符不一致')
    })
  })

  describe('verifyArray_workbench_page rulesOptions SSOT', () => {
    it('未传 verifyMethods 且 special 未勾选时不调 API', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: '1' }] })

      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [
          { id: '1', entry: 'Press %1', english: 'Press % 1' },
          { id: '2', entry: 'ok', english: 'ok' },
        ],
        rulesOptions: [
          { key: 'special', checked: false },
          { key: 'toLong', checked: true },
        ],
        showEditOperation: vi.fn(),
      }
      await verifyArray_workbench_page({ current: 1, pageSize: 10 }, 'english', vm)
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
      expect(vm.editableData['1']).toBeUndefined()
    })

    it('未传 methods 且 special 勾选时才调 API', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: '1' }] })

      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [
          { id: '1', entry: 'Press %1', english: 'Press % 1', maxLength: 200 },
          { id: '2', entry: 'ok', english: 'ok', maxLength: 200 },
        ],
        columns: [{ dataIndex: 'english' }],
        rulesOptions: [
          { key: 'special', checked: true },
          { key: 'toLong', checked: true },
        ],
        showEditOperation: vi.fn(),
      }
      await verifyArray_workbench_page({ current: 1, pageSize: 10 }, 'english', vm)
      expect(checkSykEntryBeforeSave).toHaveBeenCalledTimes(1)
      expect(vm.cellErrors['1']?.english).toContain('特殊字符不一致')
      expect(vm.editableData['2']).toBeUndefined()
    })
  })

  describe('useRefRules', () => {
    it('传入 vm 与 columnValue 时应走命令式 validateEditableCell', async () => {
      const vm = {
        editableData: { 'entry-1': { english: '' } },
        rules: {
          'entry-1': {
            english: [{ required: true, message: '请输入!' }],
          },
        },
      }
      await expect(
        useRefRules({}, 'formentry1english', 'english', vm)
      ).rejects.toMatchObject({ errorMessage: '请输入!' })
    })

    it('应该在验证成功时返回 resolved Promise', async () => {
      const refs = {
        'form1': {
          validate: vi.fn().mockResolvedValue(undefined)
        }
      }

      await expect(useRefRules(refs, 'form1')).resolves.toBeUndefined()
      expect(refs['form1'].validate).toHaveBeenCalled()
    })

    it('应该在验证失败时返回 rejected Promise', async () => {
      const refs = {
        'form1': {
          validate: vi.fn().mockRejectedValue({
            errorFields: [{ errors: ['验证失败'] }]
          })
        }
      }

      await expect(useRefRules(refs, 'form1')).rejects.toContain('编辑-保存校验失败')
    })

    it('应该在找不到 ref 时返回 rejected Promise', async () => {
      const refs = {}

      await expect(useRefRules(refs, 'form1')).rejects.toThrow('未找到 ref 名称为 "form1" 的表单引用')
    })
  })

  describe('isBlankTranslation', () => {
    it('null / undefined / "" / 纯空白 均视为空白', () => {
      expect(isBlankTranslation(null)).toBe(true)
      expect(isBlankTranslation(undefined)).toBe(true)
      expect(isBlankTranslation('')).toBe(true)
      expect(isBlankTranslation('   ')).toBe(true)
    })
    it('有实际字符返回 false', () => {
      expect(isBlankTranslation('hello')).toBe(false)
      expect(isBlankTranslation(' x ')).toBe(false)
    })
  })

  describe('validateEditableCell 空白翻译早退', () => {
    it('非 required 规则 + 空白值 → 直接通过不触发 validator', async () => {
      const vm = {
        rules: { r1: { english: [{ validator: vi.fn().mockRejectedValue('fail') }] } },
        editableData: { r1: { english: '' } },
        cellErrors: {},
      }
      await validateEditableCell(vm, 'r1', 'english')
      expect(vm.rules.r1.english[0].validator).not.toHaveBeenCalled()
    })
  })

  describe('verifyArray_workbench 空白翻译跳过', () => {
    it('空白翻译行直接进入 acceptIds，不调用 API', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

      const vm = {
        editableData: { r1: { english: '' } },
        cellErrors: {},
        rules: {},
        dataSource: [],
      }
      const array = [{ id: 'r1', entry: 'test', english: '', maxByte: 100 }]
      const result = await verifyArray_workbench(vm, array, 'english', ['toLong', 'special'])
      expect(result.acceptIds.has('r1')).toBe(true)
      expect(result.errorIds.has('r1')).toBe(false)
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
    })

    it('编辑框清空时按空串跳过，不回落到浏览态旧译文', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'r1' }] })
      const record = { id: 'r1', entry: '%1', english: 'Press % 1 to continue', maxLength: 200 }
      const vm = {
        editableData: { r1: { ...record, english: '' } },
        cellErrors: {},
        rules: {},
        dataSource: [record],
      }
      const result = await classifyArr(vm, [record], 'english', ['toLong', 'special'])
      expect(result.acceptIds.has('r1')).toBe(true)
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
    })
  })

  describe('getMethods', () => {
    it('无 rulesOptions 时回退 toLong + special', () => {
      expect(getMethods({})).toEqual(['toLong', 'special'])
    })
    it('rulesOptions 关 special 时只返回 toLong', () => {
      expect(getMethods({
        rulesOptions: [
          { key: 'special', checked: false },
          { key: 'toLong', checked: true },
        ],
      })).toEqual(['toLong'])
    })
  })

  describe('classifyArr / openFailRows', () => {
    it('classifyArr 不打开编辑态', async () => {
      const record = {
        id: 'r-long',
        entry: 'test',
        english: 'a'.repeat(30),
        maxLength: 20,
      }
      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [record],
        columns: [{ dataIndex: 'english' }],
      }
      const arr = await classifyArr(vm, [record], 'english', ['toLong'])
      expect(arr.errorIds.has('r-long')).toBe(true)
      expect(vm.editableData['r-long']).toBeUndefined()
    })

    it('openFailRows 只处理 errorIds', async () => {
      const fail = { id: 'fail', entry: 'a', english: 'b', maxLength: 20 }
      const ok = { id: 'ok', entry: 'a', english: 'ok', maxLength: 200 }
      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [fail, ok],
        columns: [{ dataIndex: 'english' }],
        showEditOperation: vi.fn(),
      }
      const arr = {
        acceptIds: new Set(['ok']),
        errorIds: new Set(['fail']),
        toLongIds: new Set(['fail']),
        specialIds: new Set(),
      }
      await openFailRows(vm, [fail, ok], arr, 'english')
      expect(vm.editableData.fail).toBeDefined()
      expect(vm.editableData.ok).toBeUndefined()
      expect(vm.showEditOperation).toHaveBeenCalled()
    })

    it('openFailRows 已有 specialIds 时不再请求 special 接口，仍写红字', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'fail' }] })

      const fail = { id: 'fail', entry: '%1', english: '% 1', maxLength: 200 }
      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [fail],
        columns: [{ dataIndex: 'english' }],
        showEditOperation: vi.fn(),
      }
      const arr = {
        acceptIds: new Set(),
        errorIds: new Set(['fail']),
        toLongIds: new Set(),
        specialIds: new Set(['fail']),
      }
      await openFailRows(vm, [fail], arr, 'english')
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
      expect(vm.editableData.fail).toBeDefined()
      expect(vm.cellErrors.fail.english).toContain('特殊字符不一致')
    })

    it('openFailRows 同时超长与 special 时优先超长文案且不调 API', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      const fail = { id: 'fail', entry: '%1', english: 'a'.repeat(30), maxLength: 20 }
      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [fail],
        columns: [{ dataIndex: 'english' }],
        showEditOperation: vi.fn(),
      }
      await openFailRows(vm, [fail], {
        acceptIds: new Set(),
        errorIds: new Set(['fail']),
        toLongIds: new Set(['fail']),
        specialIds: new Set(['fail']),
      }, 'english')
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
      expect(vm.cellErrors.fail.english).toContain('允许最大字符数为20')
    })
  })

  describe('勾选快照 vs 运行时 getMethods', () => {
    it('setRefRules 后取消勾选，saveEdit 按当前勾选通过且不调 API', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'r1' }] })
      const record = { id: 'r1', entry: '%1', english: '% 1', maxLength: 200 }
      const vm = {
        editableData: { r1: { ...record } },
        rules: {},
        cellErrors: {},
        columns: [{ dataIndex: 'english' }, { dataIndex: 'editOperation' }],
        rulesOptions: [
          { key: 'special', checked: true },
          { key: 'toLong', checked: true },
        ],
      }
      setRefRules(vm, record, ['english'])
      vm.rulesOptions = [
        { key: 'special', checked: false },
        { key: 'toLong', checked: false },
      ]
      checkSykEntryBeforeSave.mockClear()
      const ok = await saveEdit(vm, record, {
        transCol: 'english',
        commit: (rec, row) => { rec.english = row.english },
      })
      expect(ok).toBe(true)
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
      expect(vm.editableData.r1).toBeUndefined()
    })
  })

  describe('revalidateLoaded', () => {
    it('全关后退出编辑无红字；再勾 special 进编辑打红字且 API 一次', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      const record = { id: 'fail', entry: '%1', english: '% 1', maxLength: 200 }
      const vm = {
        editableData: { fail: { ...record } },
        rules: {},
        cellErrors: { fail: { english: '特殊字符不一致\r\n(如%1翻译成% 1)' } },
        dataSource: [record],
        columns: [{ dataIndex: 'english' }, { dataIndex: 'editOperation' }],
        showEditOperation: vi.fn(),
        rulesOptions: [
          { key: 'special', checked: false },
          { key: 'toLong', checked: false },
        ],
      }
      await revalidateLoaded(vm, 'english')
      expect(vm.cellErrors.fail).toBeUndefined()
      expect(vm.editableData.fail).toBeUndefined()
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()

      vm.rulesOptions = [
        { key: 'special', checked: true },
        { key: 'toLong', checked: true },
      ]
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'fail' }] })
      await revalidateLoaded(vm, 'english')
      expect(checkSykEntryBeforeSave).toHaveBeenCalledTimes(1)
      expect(vm.cellErrors.fail.english).toContain('特殊字符不一致')
      expect(vm.editableData.fail).toBeDefined()
    })

    it('同一 id 只按编辑展示值校验，special 请求该 id 只出现一次', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'r1' }] })
      const record = { id: 'r1', entry: '%1', english: 'Press %1 to continue', maxLength: 200 }
      const vm = {
        editableData: { r1: { ...record, english: 'Press % 1 to continue' } },
        rules: {},
        cellErrors: {},
        dataSource: [record],
        columns: [{ dataIndex: 'english' }],
        showEditOperation: vi.fn(),
        rulesOptions: [
          { key: 'special', checked: true },
          { key: 'toLong', checked: true },
        ],
      }
      await revalidateLoaded(vm, 'english')
      expect(checkSykEntryBeforeSave).toHaveBeenCalledTimes(1)
      expect(checkSykEntryBeforeSave.mock.calls[0][0]).toEqual([
        { id: 'r1', entry: '%1', translate: 'Press % 1 to continue', maxLength: 200 },
      ])
      expect(vm.editableData.r1).toBeDefined()
      expect(vm.cellErrors.r1.english).toContain('特殊字符不一致')
      expect(record.english).toBe('Press %1 to continue')
    })

    it('浏览态失败行勾上 special 后进编辑出红字', async () => {
      const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'r1' }] })
      const record = { id: 'r1', entry: '%1', english: 'Press % 1', maxLength: 200 }
      const vm = {
        editableData: {},
        rules: {},
        cellErrors: {},
        dataSource: [record],
        columns: [{ dataIndex: 'english' }],
        showEditOperation: vi.fn(),
        rulesOptions: [
          { key: 'special', checked: true },
          { key: 'toLong', checked: true },
        ],
      }
      await revalidateLoaded(vm, 'english')
      expect(vm.editableData.r1).toBeDefined()
      expect(vm.cellErrors.r1.english).toContain('特殊字符不一致')
    })

    it('编辑态通过行写回展示译文并退回浏览', async () => {
      const record = { id: 'ok', entry: 'hello', english: 'old', maxLength: 200 }
      const vm = {
        editableData: { ok: { ...record, english: 'new ok' } },
        rules: {},
        cellErrors: {},
        dataSource: [record],
        columns: [{ dataIndex: 'english' }, { dataIndex: 'editOperation' }],
        showEditOperation: vi.fn(),
        rulesOptions: [
          { key: 'special', checked: false },
          { key: 'toLong', checked: true },
        ],
      }
      await revalidateLoaded(vm, 'english')
      expect(vm.editableData.ok).toBeUndefined()
      expect(record.english).toBe('new ok')
    })
  })

  describe('saveEdit / cancelEdit / showEditOperation', () => {
    it('saveEdit 失败保留编辑态并写红字', async () => {
      const record = { id: 'r1', english: '' }
      const vm = {
        editableData: { r1: { english: '' } },
        rules: { r1: { english: [{ required: true, message: '请输入!' }] } },
        cellErrors: {},
        columns: [{ dataIndex: 'english' }],
      }
      const ok = await saveEdit(vm, record, { transCol: 'english', commit: () => {} })
      expect(ok).toBe(false)
      expect(vm.editableData.r1).toBeDefined()
      expect(vm.cellErrors.r1.english).toBe('请输入!')
    })

    it('saveEdit 成功则 commit 并退出编辑态', async () => {
      const record = { id: 'r1', english: 'old' }
      const vm = {
        editableData: { r1: { english: 'new' } },
        rules: { r1: {} },
        cellErrors: { r1: { english: '旧' } },
        columns: [{ dataIndex: 'english' }, { dataIndex: 'editOperation' }],
      }
      const ok = await saveEdit(vm, record, {
        transCol: 'english',
        commit: (rec, row) => { rec.english = row.english },
      })
      expect(ok).toBe(true)
      expect(record.english).toBe('new')
      expect(vm.editableData.r1).toBeUndefined()
      expect(vm.columns.some((c) => c.dataIndex === 'editOperation')).toBe(false)
    })

    it('cancelEdit 丢弃 editableData', () => {
      const vm = {
        editableData: { r1: { english: 'x' } },
        columns: [{ dataIndex: 'english' }, { dataIndex: 'editOperation' }],
      }
      cancelEdit(vm, 'r1')
      expect(vm.editableData.r1).toBeUndefined()
    })

    it('showEditOperation 不重复加列', () => {
      const vm = { columns: [{ dataIndex: 'english' }] }
      showEditOperation(vm)
      showEditOperation(vm)
      expect(vm.columns.filter((c) => c.dataIndex === 'editOperation')).toHaveLength(1)
    })
  })
})
