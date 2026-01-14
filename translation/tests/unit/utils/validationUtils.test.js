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
      const vm = {}

      expect(getMaxLength(record, vm)).toBe(200)
    })

    it('应该返回 null 当没有分类且没有 maxLength 时', () => {
      const record = {}
      const vm = {}

      expect(getMaxLength(record, vm)).toBeNull()
    })

    it('应该处理 classifyLimit 中不存在分类的情况', () => {
      const record = { classfy1: 'unknownCategory' }
      const vm = {
        classifyLimit: {
          category1: {
            foreignMaxByte: 100
          }
        }
      }

      expect(getMaxLength(record, vm, 'foreignMaxByte')).toBeNull()
    })
  })

  describe('setRefRules', () => {
    it('应该为 entry 列设置校验规则', () => {
      const vm = {
        rules: {}
      }
      const record = { id: 'test-id', entry: 'test' }
      const cols = ['entry']

      setRefRules(vm, record, cols)

      expect(vm.rules['test-id']).toBeDefined()
      expect(vm.rules['test-id'].entry).toHaveLength(2)
      expect(vm.rules['test-id'].entry[1].required).toBe(true)
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

      expect(vm.rules['test-id'].entry).toBeDefined()
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
