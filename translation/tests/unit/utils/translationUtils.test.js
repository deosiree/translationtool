import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  interpretation2value
} from '@/utils/translationUtils'
import commonParam from '@/utils/commonParam'

// Mock commonParam
vi.mock('@/utils/commonParam', () => ({
  default: {
    languageList: [
      {
        value: 'english',
        interpretation: 'englishInterpretation'
      },
      {
        value: 'chinese',
        interpretation: 'chineseInterpretation'
      }
    ]
  }
}))

// Mock validationUtils
vi.mock('@/utils/validationUtils', () => ({
  verifyArray_workbench: vi.fn()
}))

describe('translationUtils - 翻译相关工具函数', () => {
  let mockVm

  beforeEach(() => {
    vi.clearAllMocks()

    mockVm = {
      selectedRows: [
        {
          id: 1,
          entry: 'test',
          englishInterpretation: 'test interpretation',
          chineseInterpretation: '测试释义'
        },
        {
          id: 2,
          entry: 'test2',
          englishInterpretation: 'test2 interpretation'
        }
      ],
      dataSource: [
        { id: 1, entry: 'test', english: 'old english' },
        { id: 2, entry: 'test2', english: 'old english2' }
      ],
      allData: [
        { id: 1, entry: 'test', english: 'old english' },
        { id: 2, entry: 'test2', english: 'old english2' }
      ],
      $nextTick: vi.fn((callback) => {
        callback()
      }),
      $refs: {
        workTable: {
          reload: vi.fn()
        }
      }
    }
  })

  describe('interpretation2value', () => {
    it('应该将释义替换为对应语种的值', () => {
      interpretation2value(mockVm)

      // 检查是否替换了值
      expect(mockVm.selectedRows[0].english).toBe('test interpretation')
      expect(mockVm.selectedRows[0].chinese).toBe('测试释义')
      expect(mockVm.selectedRows[1].english).toBe('test2 interpretation')
    })

    it('应该更新 dataSource', () => {
      interpretation2value(mockVm)

      expect(mockVm.dataSource[0].english).toBe('test interpretation')
      expect(mockVm.dataSource[1].english).toBe('test2 interpretation')
    })

    it('应该更新 allData', () => {
      interpretation2value(mockVm)

      expect(mockVm.allData[0].english).toBe('test interpretation')
      expect(mockVm.allData[1].english).toBe('test2 interpretation')
    })

    it('应该调用 $nextTick 和 reload', () => {
      interpretation2value(mockVm)

      expect(mockVm.$nextTick).toHaveBeenCalled()
      expect(mockVm.$refs.workTable.reload).toHaveBeenCalled()
    })

    it('应该处理没有 dataSource 的情况', () => {
      delete mockVm.dataSource
      interpretation2value(mockVm)

      // 不应该报错
      expect(mockVm.selectedRows[0].english).toBe('test interpretation')
    })

    it('应该处理没有 allData 的情况', () => {
      delete mockVm.allData
      interpretation2value(mockVm)

      // 不应该报错
      expect(mockVm.selectedRows[0].english).toBe('test interpretation')
    })

    it('应该处理没有 workTable ref 的情况', () => {
      delete mockVm.$refs.workTable
      interpretation2value(mockVm)

      // 不应该报错
      expect(mockVm.selectedRows[0].english).toBe('test interpretation')
    })

    it('应该只替换存在释义字段的行', () => {
      mockVm.selectedRows = [
        {
          id: 1,
          entry: 'test',
          englishInterpretation: 'test interpretation'
        },
        {
          id: 2,
          entry: 'test2'
          // 没有 englishInterpretation
        }
      ]

      interpretation2value(mockVm)

      expect(mockVm.selectedRows[0].english).toBe('test interpretation')
      expect(mockVm.selectedRows[1].english).toBeUndefined()
    })
  })
})
