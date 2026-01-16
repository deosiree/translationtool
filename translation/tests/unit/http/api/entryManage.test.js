/**
 * entryManage API 单元测试
 * 测试 entryImportExcle_v2 和 entryValidate_v2 两个 v2 版本的 API
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { entryImportExcle_v2, entryValidate_v2 } from '@/http/api/entryManage'

// Mock mock文件中的函数
vi.mock('@/http/api/mock/entryManage', () => ({
  entryImportExcle_v2: vi.fn(),
  entryValidate_v2: vi.fn(),
}))

import {
  entryImportExcle_v2 as mockEntryImportExcle_v2,
  entryValidate_v2 as mockEntryValidate_v2,
} from '@/http/api/mock/entryManage'

describe('entryManage API - v2版本', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(console, 'log').mockImplementation(() => {})
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  describe('entryImportExcle_v2', () => {
    it('应该正确调用mock函数并传递参数', async () => {
      const params = { field: '词条' }
      const formData = new FormData()
      formData.append('file', new Blob(['test'], { type: 'text/csv' }))

      const mockResponse = {
        code: 200,
        message: '导入成功',
        data: { success: true, field: '词条' }
      }
      mockEntryImportExcle_v2.mockResolvedValue(mockResponse)

      const result = await entryImportExcle_v2(params, formData)

      expect(mockEntryImportExcle_v2).toHaveBeenCalledTimes(1)
      expect(mockEntryImportExcle_v2).toHaveBeenCalledWith(params, formData)
      expect(result).toEqual(mockResponse)
    })

    it('应该处理成功响应 (code: 200)', async () => {
      const params = { field: '英文翻译' }
      const formData = new FormData()

      const successResponse = {
        code: 200,
        message: '导入成功',
        data: {
          success: true,
          field: '英文翻译'
        }
      }
      mockEntryImportExcle_v2.mockResolvedValue(successResponse)

      const result = await entryImportExcle_v2(params, formData)

      expect(result.code).toBe(200)
      expect(result.message).toBe('导入成功')
      expect(result.data.success).toBe(true)
      expect(result.data.field).toBe('英文翻译')
    })

    it('应该处理部分失败响应 (code: 201)', async () => {
      const params = { field: '俄文翻译' }
      const formData = new FormData()

      const failureResponse = {
        code: 201,
        message: '导入存在失败',
        data: {
          failedEntryInfos: [
            { id: '1', entry: '测试词条1', '俄文翻译': '测试值1' },
            { id: '2', entry: '测试词条2', '俄文翻译': '测试值2' }
          ],
          exceptionVos: [
            { id: '1', message: '异常信息1' }
          ],
          globalMessage: '部分词条导入失败'
        }
      }
      mockEntryImportExcle_v2.mockResolvedValue(failureResponse)

      const result = await entryImportExcle_v2(params, formData)

      expect(result.code).toBe(201)
      expect(result.message).toBe('导入存在失败')
      expect(result.data.failedEntryInfos).toHaveLength(2)
      expect(result.data.exceptionVos).toHaveLength(1)
      expect(result.data.globalMessage).toBe('部分词条导入失败')
    })

    it('应该处理随机响应多次调用', async () => {
      const params = { field: '词条' }
      const formData = new FormData()

      // 模拟多次调用，返回不同的随机响应
      const responses = [
        { code: 200, message: '导入成功', data: { success: true, field: '词条' } },
        { code: 201, message: '导入存在失败', data: { failedEntryInfos: [], exceptionVos: [], globalMessage: '失败' } },
        { code: 200, message: '导入成功', data: { success: true, field: '词条' } }
      ]

      for (const response of responses) {
        mockEntryImportExcle_v2.mockResolvedValueOnce(response)
      }

      const results = await Promise.all([
        entryImportExcle_v2(params, formData),
        entryImportExcle_v2(params, formData),
        entryImportExcle_v2(params, formData)
      ])

      expect(results).toHaveLength(3)
      expect(results[0].code).toBe(200)
      expect(results[1].code).toBe(201)
      expect(results[2].code).toBe(200)
      expect(mockEntryImportExcle_v2).toHaveBeenCalledTimes(3)
    })

    it('应该处理空字段参数', async () => {
      const params = {}
      const formData = new FormData()

      const response = {
        code: 200,
        message: '导入成功',
        data: { success: true, field: '未知字段' }
      }
      mockEntryImportExcle_v2.mockResolvedValue(response)

      const result = await entryImportExcle_v2(params, formData)

      expect(result.data.field).toBe('未知字段')
    })
  })

  describe('entryValidate_v2', () => {
    it('应该正确调用mock函数并传递参数', async () => {
      const params = { field: '词条' }
      const formData = new FormData()
      formData.append('file', new Blob(['test'], { type: 'text/csv' }))

      const mockResponse = {
        code: 200,
        message: '校验成功',
        data: { success: true, field: '词条' }
      }
      mockEntryValidate_v2.mockResolvedValue(mockResponse)

      const result = await entryValidate_v2(params, formData)

      expect(mockEntryValidate_v2).toHaveBeenCalledTimes(1)
      expect(mockEntryValidate_v2).toHaveBeenCalledWith(params, formData)
      expect(result).toEqual(mockResponse)
    })

    it('应该处理成功响应 (code: 200)', async () => {
      const params = { field: '英文翻译' }
      const formData = new FormData()

      const successResponse = {
        code: 200,
        message: '校验成功',
        data: {
          success: true,
          field: '英文翻译'
        }
      }
      mockEntryValidate_v2.mockResolvedValue(successResponse)

      const result = await entryValidate_v2(params, formData)

      expect(result.code).toBe(200)
      expect(result.message).toBe('校验成功')
      expect(result.data.success).toBe(true)
      expect(result.data.field).toBe('英文翻译')
    })

    it('应该处理部分失败响应 (code: 201)', async () => {
      const params = { field: '俄文翻译' }
      const formData = new FormData()

      const failureResponse = {
        code: 201,
        message: '校验存在失败',
        data: {
          failedEntryInfos: [
            { id: '1', entry: '测试词条1', '俄文翻译': '测试值1', error: '长度超限' },
            { id: '2', entry: '测试词条2', '俄文翻译': '测试值2', error: '特殊字符不一致' }
          ],
          exceptionVos: [
            { id: '1', message: '校验异常信息1' }
          ],
          globalMessage: '部分词条校验失败'
        }
      }
      mockEntryValidate_v2.mockResolvedValue(failureResponse)

      const result = await entryValidate_v2(params, formData)

      expect(result.code).toBe(201)
      expect(result.message).toBe('校验存在失败')
      expect(result.data.failedEntryInfos).toHaveLength(2)
      expect(result.data.failedEntryInfos[0].error).toBe('长度超限')
      expect(result.data.failedEntryInfos[1].error).toBe('特殊字符不一致')
      expect(result.data.exceptionVos).toHaveLength(1)
      expect(result.data.globalMessage).toBe('部分词条校验失败')
    })

    it('应该处理随机响应多次调用', async () => {
      const params = { field: '词条' }
      const formData = new FormData()

      // 模拟多次调用，返回不同的随机响应
      const responses = [
        { code: 200, message: '校验成功', data: { success: true, field: '词条' } },
        { code: 201, message: '校验存在失败', data: { failedEntryInfos: [], exceptionVos: [], globalMessage: '失败' } },
        { code: 200, message: '校验成功', data: { success: true, field: '词条' } }
      ]

      for (const response of responses) {
        mockEntryValidate_v2.mockResolvedValueOnce(response)
      }

      const results = await Promise.all([
        entryValidate_v2(params, formData),
        entryValidate_v2(params, formData),
        entryValidate_v2(params, formData)
      ])

      expect(results).toHaveLength(3)
      expect(results[0].code).toBe(200)
      expect(results[1].code).toBe(201)
      expect(results[2].code).toBe(200)
      expect(mockEntryValidate_v2).toHaveBeenCalledTimes(3)
    })

    it('应该处理空字段参数', async () => {
      const params = {}
      const formData = new FormData()

      const response = {
        code: 200,
        message: '校验成功',
        data: { success: true, field: '未知字段' }
      }
      mockEntryValidate_v2.mockResolvedValue(response)

      const result = await entryValidate_v2(params, formData)

      expect(result.data.field).toBe('未知字段')
    })

    it('应该验证响应结构完整性', async () => {
      const params = { field: '词条' }
      const formData = new FormData()

      const successResponse = {
        code: 200,
        message: '校验成功',
        data: { success: true, field: '词条' }
      }
      mockEntryValidate_v2.mockResolvedValue(successResponse)

      const result = await entryValidate_v2(params, formData)

      // 验证响应结构
      expect(result).toHaveProperty('code')
      expect(result).toHaveProperty('message')
      expect(result).toHaveProperty('data')
      expect(result.data).toHaveProperty('success')
      expect(result.data).toHaveProperty('field')
    })

    it('应该验证失败响应结构完整性', async () => {
      const params = { field: '词条' }
      const formData = new FormData()

      const failureResponse = {
        code: 201,
        message: '校验存在失败',
        data: {
          failedEntryInfos: [{ id: '1', entry: '测试', '词条': '值', error: '错误' }],
          exceptionVos: [{ id: '1', message: '异常' }],
          globalMessage: '失败'
        }
      }
      mockEntryValidate_v2.mockResolvedValue(failureResponse)

      const result = await entryValidate_v2(params, formData)

      // 验证失败响应结构
      expect(result).toHaveProperty('code')
      expect(result).toHaveProperty('message')
      expect(result).toHaveProperty('data')
      expect(result.data).toHaveProperty('failedEntryInfos')
      expect(result.data).toHaveProperty('exceptionVos')
      expect(result.data).toHaveProperty('globalMessage')
      expect(Array.isArray(result.data.failedEntryInfos)).toBe(true)
      expect(Array.isArray(result.data.exceptionVos)).toBe(true)
      expect(typeof result.data.globalMessage).toBe('string')
    })
  })
})
