import { describe, it, expect } from 'vitest'
import { getCurrentFormattedTime, getCurrentStringTime } from '@/utils/dateUtils'

describe('dateUtils - 时间工具函数', () => {
  describe('getCurrentFormattedTime', () => {
    it('应该返回正确格式的时间字符串 (YYYY-MM-DD HH:mm:ss)', () => {
      const time = getCurrentFormattedTime()
      // 验证格式：YYYY-MM-DD HH:mm:ss
      expect(time).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    })

    it('应该返回当前时间', () => {
      const before = Date.now() - 1000
      const time = getCurrentFormattedTime()
      const after = Date.now() + 1000

      const timeDate = new Date(time).getTime()
      // 允许1秒的误差范围（考虑到函数执行时间）
      expect(timeDate).toBeGreaterThanOrEqual(before)
      expect(timeDate).toBeLessThanOrEqual(after)
    })
  })

  describe('getCurrentStringTime', () => {
    it('应该返回正确格式的时间字符串 (YYYYMMDDHHmmss)', () => {
      const time = getCurrentStringTime()
      // 验证格式：YYYYMMDDHHmmss（14位数字）
      expect(time).toMatch(/^\d{14}$/)
    })
  })
})
