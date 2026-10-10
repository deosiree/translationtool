import { describe, it, expect, vi } from 'vitest'
import { randomMsg, randomError } from '@/utils/testUtils'

describe('testUtils - 测试工具函数', () => {
  describe('randomMsg', () => {
    it('应该返回数组中的一个消息', async () => {
      const msgs = ['消息1', '消息2', '消息3']
      const result = await randomMsg(msgs, [0.33, 0.66])
      
      expect(msgs).toContain(result)
    })

    it('应该使用默认参数', async () => {
      const result = await randomMsg()
      
      expect(['执行中', '未执行']).toContain(result)
    })

    it('应该处理单个概率值', async () => {
      const msgs = ['选项A', '选项B']
      const result = await randomMsg(msgs, [0.5])
      
      expect(msgs).toContain(result)
    })
  })

  describe('randomError', () => {
    it('应该根据概率抛出错误', async () => {
      // Mock Math.random 返回 0.3（小于 0.5，应该抛出错误）
      const originalRandom = Math.random
      Math.random = vi.fn(() => 0.3)
      
      await expect(randomError('测试错误', 0.5)).rejects.toThrow('测试错误')
      
      Math.random = originalRandom
    })

    it('应该根据概率不抛出错误', async () => {
      // Mock Math.random 返回 0.7（大于 0.5，不应该抛出错误）
      const originalRandom = Math.random
      Math.random = vi.fn(() => 0.7)
      
      await expect(randomError('测试错误', 0.5)).resolves.toBeUndefined()
      
      Math.random = originalRandom
    })

    it('应该使用默认参数', async () => {
      const originalRandom = Math.random
      Math.random = vi.fn(() => 0.3)
      
      await expect(randomError()).rejects.toThrow('随机任务创建失败测试')
      
      Math.random = originalRandom
    })
  })
})
