import { describe, it, expect, afterEach } from 'vitest'
import { nextTick } from 'vue'
import { loading, isLoading, startLoading, endLoading, resetLoading, withLoading } from '@/composables/useLoading'

describe('useLoading - 全局单一引用计数', () => {
  afterEach(() => {
    resetLoading() // 用例间隔离
  })

  it('初始不处于 loading', () => {
    expect(isLoading()).toBe(false)
    expect(loading.value).toBe(false)
  })

  it('startLoading 置位、endLoading 归位', () => {
    startLoading()
    expect(isLoading()).toBe(true)
    expect(loading.value).toBe(true)

    endLoading()
    expect(isLoading()).toBe(false)
    expect(loading.value).toBe(false)
  })

  it('多个任务叠加：全部 end 才恢复', () => {
    startLoading()
    startLoading()
    expect(isLoading()).toBe(true)

    endLoading()
    expect(isLoading()).toBe(true) // 仍有一个任务在跑

    endLoading()
    expect(isLoading()).toBe(false)
  })

  it('endLoading 不低于 0（多余的 end 不破坏状态）', () => {
    startLoading()
    endLoading()
    endLoading() // 多余
    expect(isLoading()).toBe(false)
  })

  it('loading 为响应式：计数变化后值同步更新', async () => {
    startLoading()
    await nextTick()
    expect(loading.value).toBe(true)

    endLoading()
    await nextTick()
    expect(loading.value).toBe(false)
  })

  it('withLoading：任务前后开关、返回结果、异常时 finally 复位', async () => {
    const result = await withLoading(async () => {
      expect(isLoading()).toBe(true)
      return 'ok'
    })
    expect(result).toBe('ok')
    expect(isLoading()).toBe(false)

    await expect(
      withLoading(async () => {
        throw new Error('boom')
      })
    ).rejects.toThrow('boom')
    expect(isLoading()).toBe(false)
  })

  it('resetLoading 兜底清空计数', () => {
    startLoading()
    startLoading()
    resetLoading()
    expect(isLoading()).toBe(false)
  })
})
