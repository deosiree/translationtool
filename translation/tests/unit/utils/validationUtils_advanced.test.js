import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  verifyArray_workbench,
  verifyArray_workbench_page,
  verifyRecord_entry,
  validateRefRules,
  useRefRules,
  openSetEdit
} from '@/utils/validationUtils'
import { checkSykEntryBeforeSave } from '@/http/api/glossary'

// Mock API 调用
vi.mock('@/http/api/glossary', () => ({
  checkSykEntryBeforeSave: vi.fn()
}))

// Mock lodash
vi.mock('lodash', () => ({
  cloneDeep: vi.fn((obj) => JSON.parse(JSON.stringify(obj)))
}))

describe('validationUtils - 高级校验功能', () => {
  let mockVm

  beforeEach(() => {
    vi.clearAllMocks()

    mockVm = {
      editableData: {},
      rules: {},
      classifyLimit: {
        category1: {
          foreignMaxByte: 100,
          maxByte: 50
        }
      },
      $refs: {
        'form1english': {
          validate: vi.fn().mockResolvedValue(undefined)
        }
      }
    }
  })

  describe('verifyArray_workbench', () => {
    it('应该校验通过所有词条', async () => {
      const array = [
        { id: '1', entry: 'test', english: 'test', classfy1: 'category1' },
        { id: '2', entry: 'test2', english: 'test2', classfy1: 'category1' }
      ]
      checkSykEntryBeforeSave.mockResolvedValue({ data: [] })
      mockVm.$refs = {
        'form1english': { validate: vi.fn().mockResolvedValue(undefined) },
        'form2english': { validate: vi.fn().mockResolvedValue(undefined) }
      }

      const result = await verifyArray_workbench(mockVm, array, 'english', ['toLong', 'special'])

      expect(result.acceptIds.size).toBe(2)
      expect(result.errorIds.size).toBe(0)
      expect(result.toLongIds.size).toBe(0)
      expect(result.specialIds.size).toBe(0)
    })

    it('应该识别长度超标的词条', async () => {
      const array = [
        { id: '1', entry: 'test', english: 'a'.repeat(101), classfy1: 'category1' }
      ]
      mockVm.$refs = {
        'form1english': { validate: vi.fn().mockResolvedValue(undefined) }
      }

      const result = await verifyArray_workbench(mockVm, array, 'english', ['toLong'])

      expect(result.toLongIds.has('1')).toBe(true)
      expect(result.acceptIds.has('1')).toBe(false)
    })

    it('应该识别特殊字符不一致的词条', async () => {
      const array = [
        { id: '1', entry: 'test%1', english: 'test% 1', classfy1: 'category1' }
      ]
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: '1' }] })
      mockVm.$refs = {
        'form1english': { validate: vi.fn().mockResolvedValue(undefined) }
      }

      const result = await verifyArray_workbench(mockVm, array, 'english', ['special'])

      expect(result.specialIds.has('1')).toBe(true)
      expect(result.acceptIds.has('1')).toBe(false)
    })

    it('应该只执行指定的校验方法', async () => {
      const array = [
        { id: '1', entry: 'test', english: 'a'.repeat(101), classfy1: 'category1' }
      ]
      mockVm.$refs = {
        'form1english': { validate: vi.fn().mockResolvedValue(undefined) }
      }

      const result = await verifyArray_workbench(mockVm, array, 'english', ['toLong'])

      expect(result.toLongIds.has('1')).toBe(true)
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
    })
  })

  describe('verifyArray_workbench_page', () => {
    it('应该只校验当前页的数据', async () => {
      mockVm.dataSource = [
        { id: '1', entry: 'test1', english: 'test1', classfy1: 'category1' },
        { id: '2', entry: 'test2', english: 'test2', classfy1: 'category1' },
        { id: '3', entry: 'test3', english: 'test3', classfy1: 'category1' },
        { id: '4', entry: 'test4', english: 'test4', classfy1: 'category1' }
      ]
      const pagination = { current: 1, pageSize: 2 }
      mockVm.$refs = {
        'form1english': { validate: vi.fn().mockResolvedValue(undefined) },
        'form2english': { validate: vi.fn().mockResolvedValue(undefined) }
      }
      checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

      // 直接验证函数行为：应该只处理前2条数据
      await verifyArray_workbench_page(pagination, 'english', mockVm)

      // 验证 verifyArray_workbench 被调用，并且传入的数组长度为2
      // 由于无法直接 spy，我们通过检查 mockVm 的状态来验证
      expect(checkSykEntryBeforeSave).toHaveBeenCalled()
      const callArgs = checkSykEntryBeforeSave.mock.calls[0][0]
      expect(callArgs.length).toBeLessThanOrEqual(2) // 最多2条数据
    })
  })

  describe('verifyRecord_entry', () => {
    it('应该在校验通过时返回 true', async () => {
      const record = {
        id: '1',
        entry: 'test',
        english: 'test',
        chinese: '测试',
        classfy1: 'category1'
      }
      checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

      const result = await verifyRecord_entry(mockVm, record, ['entry', 'english', 'chinese'])

      expect(result).toBe(true)
    })

    it('应该在校验失败时返回 false 并打开编辑态', async () => {
      const record = {
        id: '1',
        entry: 'test',
        english: 'a'.repeat(101),
        classfy1: 'category1'
      }
      mockVm.$refs = {
        'form1english': { validate: vi.fn().mockResolvedValue(undefined) }
      }

      const result = await verifyRecord_entry(mockVm, record, ['english'], ['toLong'])

      expect(result).toBe(false)
      expect(mockVm.editableData['1']).toBeDefined()
    })

    it('应该校验词条长度', async () => {
      const record = {
        id: '1',
        entry: 'a'.repeat(51), // 超过 maxByte (50)
        classfy1: 'category1'
      }
      mockVm.$refs = {
        'form1entry': { validate: vi.fn().mockResolvedValue(undefined) }
      }

      const result = await verifyRecord_entry(mockVm, record, ['entry'], ['toLong'])

      expect(result).toBe(false)
    })
  })

  describe('validateRefRules', () => {
    it('应该在校验通过时 resolve', async () => {
      const record = { id: '1', entry: 'test', english: 'test', classfy1: 'category1' }
      const validator = validateRefRules(record, mockVm, 'foreignMaxByte', 'english')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

      await expect(validator({}, 'test')).resolves.toBeUndefined()
    })

    it('应该在长度超限时 reject', async () => {
      const record = { id: '1', entry: 'test', classfy1: 'category1' }
      const validator = validateRefRules(record, mockVm, 'foreignMaxByte', 'english')

      await expect(validator({}, 'a'.repeat(101))).rejects.toContain('允许最大字符数为100')
    })

    it('应该在特殊字符不一致时 reject', async () => {
      const record = { id: '1', entry: 'test%1', classfy1: 'category1' }
      const validator = validateRefRules(record, mockVm, 'foreignMaxByte', 'english')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: '1' }] })

      await expect(validator({}, 'test% 1')).rejects.toContain('特殊字符不一致')
    })

    it('应该使用 editableData 中的值进行校验', async () => {
      const record = { id: '1', entry: 'test', english: 'old', classfy1: 'category1' }
      mockVm.editableData['1'] = { english: 'new' }
      const validator = validateRefRules(record, mockVm, 'foreignMaxByte', 'english')
      checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

      await validator({}, 'new')

      expect(checkSykEntryBeforeSave).toHaveBeenCalledWith([
        { id: '1', entry: 'test', translate: 'new' }
      ])
    })
  })
})
