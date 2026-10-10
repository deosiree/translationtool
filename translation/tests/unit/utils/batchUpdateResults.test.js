import { describe, it, expect } from 'vitest'
import { partitionBatchUpdateResults } from '@/utils/batchUpdateResults'

describe('partitionBatchUpdateResults', () => {
  it('解析全成功 list', () => {
    const res = {
      code: 200,
      data: {
        list: [
          { id: 'e1', success: true, message: 'OK' },
          { id: 'e2', success: true, message: 'OK' },
        ],
        totalNum: 2,
      },
    }
    const result = partitionBatchUpdateResults(res)
    expect(result.successIds).toEqual(['e1', 'e2'])
    expect(result.failed).toEqual([])
    expect(result.successCount).toBe(2)
    expect(result.failedCount).toBe(0)
    expect(result.totalNum).toBe(2)
  })

  it('解析部分失败 list（203）', () => {
    const res = {
      code: 203,
      data: {
        list: [
          { id: 'e1', success: true, message: 'OK' },
          { id: 'e2', success: false, message: '落库失败' },
        ],
        totalNum: 2,
      },
    }
    const result = partitionBatchUpdateResults(res)
    expect(result.successIds).toEqual(['e1'])
    expect(result.failed).toEqual([{ id: 'e2', message: '落库失败' }])
    expect(result.failedCount).toBe(1)
  })

  it('空 list / 缺 data 时安全降级', () => {
    expect(partitionBatchUpdateResults({ code: 200, data: { list: [], totalNum: 0 } })).toEqual({
      successIds: [],
      failed: [],
      successCount: 0,
      failedCount: 0,
      totalNum: 0,
    })
    expect(partitionBatchUpdateResults({})).toEqual({
      successIds: [],
      failed: [],
      successCount: 0,
      failedCount: 0,
      totalNum: 0,
    })
  })
})
