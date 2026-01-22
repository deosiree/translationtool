import { describe, it, expect, vi, beforeEach } from 'vitest'
import { 
  byteLength, 
  getMaxLength, 
  setRefRules, 
  openSetEdit,
  useRefRules
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

  describe('useRefRules', () => {
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
})
