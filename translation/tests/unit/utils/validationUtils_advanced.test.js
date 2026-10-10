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
        { id: '1', entry: 'test1%1', english: 'test1 % 1', classfy1: 'category1' },
        { id: '2', entry: 'test2%1', english: 'test2 % 1', classfy1: 'category1' },
        { id: '3', entry: 'test3%1', english: 'test3 % 1', classfy1: 'category1' },
        { id: '4', entry: 'test4%1', english: 'test4 % 1', classfy1: 'category1' }
      ]
      mockVm.rulesOptions = [
        { key: 'special', checked: true },
        { key: 'toLong', checked: false },
      ]
      const pagination = { current: 1, pageSize: 2 }
      mockVm.$refs = {
        'form1english': { validate: vi.fn().mockResolvedValue(undefined) },
        'form2english': { validate: vi.fn().mockResolvedValue(undefined) }
      }

      await verifyArray_workbench_page(pagination, 'english', mockVm)

      // 本地 special：仅当前页 1、2 进编辑；3、4 不动
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
      expect(mockVm.editableData['1']).toBeDefined()
      expect(mockVm.editableData['2']).toBeDefined()
      expect(mockVm.editableData['3']).toBeUndefined()
      expect(mockVm.editableData['4']).toBeUndefined()
    })

    it('rulesOptions 关闭 special 时不应调用 checkSykEntryBeforeSave', async () => {
      mockVm.dataSource = [
        { id: '1', entry: 'Press %1', english: 'Press % 1', classfy1: 'category1' },
        { id: '2', entry: 'test2', english: 'test2', classfy1: 'category1' },
      ]
      mockVm.rulesOptions = [
        { key: 'special', checked: false },
        { key: 'toLong', checked: true },
      ]
      const pagination = { current: 1, pageSize: 2 }
      checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: '1' }] })

      await verifyArray_workbench_page(pagination, 'english', mockVm)

      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
      expect(mockVm.editableData['1']).toBeUndefined()
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

      await expect(validator({}, 'test% 1')).rejects.toContain('特殊字符不一致')
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
    })

    it('应该使用 editableData 中的 entry 进行占位校验', async () => {
      const record = { id: '1', entry: 'test', english: 'old', classfy1: 'category1' }
      mockVm.editableData['1'] = { entry: 'test%1', english: 'new' }
      const validator = validateRefRules(record, mockVm, 'foreignMaxByte', 'english')

      await expect(validator({}, 'new')).rejects.toContain('特殊字符不一致')
      expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
    })
  })
})
