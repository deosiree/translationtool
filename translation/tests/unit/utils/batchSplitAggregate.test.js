import { describe, it, expect } from 'vitest'
import {
  chunkArray,
  buildSubTaskId,
  aggregateStepStatus,
  aggregateStageStatus,
  aggregateToParent,
  isMainProgress
} from '@/utils/batchSplitAggregate'

describe('batchSplitAggregate', () => {
  it('chunkArray 按上限切分', () => {
    const arr = Array.from({ length: 5 }, (_, i) => i)
    expect(chunkArray(arr, 2)).toEqual([[0, 1], [2, 3], [4]])
    expect(chunkArray(arr, 10)).toEqual([[0, 1, 2, 3, 4]])
    expect(chunkArray([], 100)).toEqual([])
  })

  it('buildSubTaskId 格式正确', () => {
    expect(buildSubTaskId('t1', 'preTranslate', 3)).toBe('t1__split_preTranslate_3')
  })

  it('aggregateStepStatus 优先级', () => {
    expect(aggregateStepStatus(['success', 'success'])).toBe('success')
    expect(aggregateStepStatus(['success', 'running'])).toBe('running')
    expect(aggregateStepStatus(['success', 'failed'])).toBe('failed')
    expect(aggregateStepStatus(['skipped', 'skipped'])).toBe('skipped')
  })

  it('aggregateStageStatus 含 warning', () => {
    expect(aggregateStageStatus(['success', 'success'], false)).toBe('success')
    expect(aggregateStageStatus(['success', 'warning'], true)).toBe('warning')
    expect(aggregateStageStatus(['failed', 'success'], false)).toBe('failed')
  })

  it('aggregateToParent 聚合计数与步骤状态', () => {
    const parent = {
      taskId: 't1',
      stages: { preTranslate: 'running' },
      stageCounts: { preTranslate: { current: 0, total: 0 } },
      steps: {
        preTranslate: {
          query: 'success',
          selectAll: 'pending',
          preTranslate: 'pending',
          save: 'pending'
        }
      },
      stepCounts: {
        preTranslate: { query: 100, selectAll: 0, preTranslate: 0, save: 0 }
      },
      stageMessages: {},
      retryCount: 0
    }

    const subs = [
      {
        stages: { preTranslate: 'success' },
        stageCounts: { preTranslate: { current: 50, total: 50 } },
        steps: {
          preTranslate: { selectAll: 'success', preTranslate: 'success', save: 'success' }
        },
        stepCounts: {
          preTranslate: { selectAll: 50, preTranslate: 50, save: 50 }
        },
        retryCount: 0
      },
      {
        stages: { preTranslate: 'success' },
        stageCounts: { preTranslate: { current: 50, total: 50 } },
        steps: {
          preTranslate: { selectAll: 'success', preTranslate: 'success', save: 'success' }
        },
        stepCounts: {
          preTranslate: { selectAll: 50, preTranslate: 50, save: 50 }
        },
        retryCount: 1
      }
    ]

    const result = aggregateToParent(parent, subs, 'preTranslate', 100)
    expect(result.hasFailed).toBe(false)
    expect(result.processedTotal).toBe(100)
    expect(parent.steps.preTranslate.selectAll).toBe('success')
    expect(parent.steps.preTranslate.preTranslate).toBe('success')
    expect(parent.stepCounts.preTranslate.preTranslate).toBe(100)
    expect(parent.stageCounts.preTranslate).toEqual({ current: 100, total: 100 })
    expect(parent.retryCount).toBe(1)
  })

  it('isMainProgress 区分主/子', () => {
    expect(isMainProgress({ parentTaskId: null })).toBe(true)
    expect(isMainProgress({ parentTaskId: 't1' })).toBe(false)
  })
})
