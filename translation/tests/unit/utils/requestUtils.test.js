import { describe, it, expect, vi, beforeEach } from 'vitest'
import { encodeParams, handleAsyncRequest } from '@/utils/requestUtils'
import { message } from 'ant-design-vue'

// Mock ant-design-vue
vi.mock('ant-design-vue', () => ({
  message: {
    error: vi.fn()
  }
}))

// Mock request
vi.mock('@/http/request', () => ({
  cancelRequest: vi.fn()
}))

describe('requestUtils - HTTP/请求处理工具函数', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('encodeParams', () => {
    it('应该编码字符串参数', () => {
      const input = '测试 字符串'
      const result = encodeParams(input)
      
      expect(result).toBe(encodeURIComponent(input))
    })

    it('应该编码数组参数', () => {
      const input = ['测试1', '测试2', 'hello world']
      const result = encodeParams(input)
      
      expect(result).toHaveLength(3)
      expect(result[0]).toBe(encodeURIComponent('测试1'))
      expect(result[1]).toBe(encodeURIComponent('测试2'))
      expect(result[2]).toBe(encodeURIComponent('hello world'))
    })

    it('应该编码对象参数', () => {
      const input = {
        name: '测试名称',
        value: 'test value',
        nested: {
          key: 'nested value'
        }
      }
      const result = encodeParams(input)
      
      expect(result.name).toBe(encodeURIComponent('测试名称'))
      expect(result.value).toBe(encodeURIComponent('test value'))
      expect(result.nested.key).toBe(encodeURIComponent('nested value'))
    })

    it('应该处理嵌套数组和对象', () => {
      const input = {
        items: ['item1', 'item2'],
        config: {
          setting: 'value'
        }
      }
      const result = encodeParams(input)
      
      expect(result.items).toHaveLength(2)
      expect(result.items[0]).toBe(encodeURIComponent('item1'))
      expect(result.config.setting).toBe(encodeURIComponent('value'))
    })

    it('应该处理 null 和 undefined', () => {
      expect(encodeParams(null)).toBe(null)
      expect(encodeParams(undefined)).toBe(undefined)
    })

    it('应该处理数字', () => {
      expect(encodeParams(123)).toBe(123)
    })
  })

  describe('handleAsyncRequest', () => {
    it('应该在表单验证失败时返回空数组', async () => {
      const closeLoading = vi.fn()
      const validateRef = {
        validate: vi.fn().mockRejectedValue(new Error('验证失败'))
      }
      const getDataFn = vi.fn()

      const result = await handleAsyncRequest(closeLoading, validateRef, getDataFn)

      expect(result).toEqual([])
      expect(closeLoading).toHaveBeenCalled()
      expect(getDataFn).not.toHaveBeenCalled()
    })

    it('应该在表单验证成功时调用 getDataFn', async () => {
      const closeLoading = vi.fn()
      const validateRef = {
        validate: vi.fn().mockResolvedValue(undefined)
      }
      const getDataFn = vi.fn().mockResolvedValue({
        data: {
          list: [1, 2, 3]
        }
      })

      const result = await handleAsyncRequest(closeLoading, validateRef, getDataFn)

      expect(result).toEqual([1, 2, 3])
      expect(validateRef.validate).toHaveBeenCalled()
      expect(getDataFn).toHaveBeenCalled()
    })

    it('应该使用自定义 returnParams', async () => {
      const closeLoading = vi.fn()
      const validateRef = {
        validate: vi.fn().mockResolvedValue(undefined)
      }
      const getDataFn = vi.fn().mockResolvedValue({
        data: {
          items: ['a', 'b', 'c']
        }
      })

      const result = await handleAsyncRequest(closeLoading, validateRef, getDataFn, null, null, 'data.items')

      expect(result).toEqual(['a', 'b', 'c'])
    })

    it('应该在 returnParams 为空时返回空数组', async () => {
      const closeLoading = vi.fn()
      const validateRef = {
        validate: vi.fn().mockResolvedValue(undefined)
      }
      const getDataFn = vi.fn().mockResolvedValue({ data: { list: [1, 2] } })

      const result = await handleAsyncRequest(closeLoading, validateRef, getDataFn, null, null, '')

      expect(result).toEqual([])
    })

    it('应该在 getDataFn 抛出 ValidationError 时返回空数组', async () => {
      const closeLoading = vi.fn()
      const validateRef = {
        validate: vi.fn().mockResolvedValue(undefined)
      }
      const validationError = new Error('验证错误')
      validationError.name = 'ValidationError'
      const getDataFn = vi.fn().mockRejectedValue(validationError)

      const result = await handleAsyncRequest(closeLoading, validateRef, getDataFn)

      expect(result).toEqual([])
      expect(message.error).not.toHaveBeenCalled()
    })

    it('应该在 getDataFn 抛出其他错误时显示错误消息', async () => {
      const closeLoading = vi.fn()
      const validateRef = {
        validate: vi.fn().mockResolvedValue(undefined)
      }
      const error = new Error('网络错误')
      const getDataFn = vi.fn().mockRejectedValue(error)

      const result = await handleAsyncRequest(closeLoading, validateRef, getDataFn)

      expect(result).toEqual([])
      expect(message.error).toHaveBeenCalledWith('数据获取失败！', '网络错误')
    })
  })
})
