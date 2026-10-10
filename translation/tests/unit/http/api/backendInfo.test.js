/**
 * backendInfo API 单元测试
 * 测试 getEntrysourceListByClassfyTaskState 和 getEntrysourceListByClassfyResult 两个新接口
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock request 层，避免 jsdom 下发真实网络请求
vi.mock('@/http/request', () => ({
  default: vi.fn(),
}))

import request from '@/http/request'
import {
  getEntrysourceListByClassfyTaskState,
  getEntrysourceListByClassfyResult,
} from '@/http/api/backendInfo'

describe('backendInfo API - 更新词条任务相关', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(console, 'log').mockImplementation(() => {})
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.spyOn(console, 'warn').mockImplementation(() => {})
  })

  describe('getEntrysourceListByClassfyTaskState (查询任务状态)', () => {
    it('应该正确调用request并传递参数', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const successResp = {
        code: 200,
        type: 'OK',
        data: {
          state: '0',
        },
        message: '没有查到正在执行的任务信息',
        operationObject: '',
      }
      request.mockResolvedValue(successResp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(request).toHaveBeenCalledTimes(1)
      expect(request).toHaveBeenCalledWith({
        url: '/backendInfo/getEntrysourceListByClassfyTaskState',
        method: 'POST',
        params,
      })
      expect(result.code).toBe(200)
      expect(result.data.state).toBe('0')
    })

    it('应该处理状态0（无任务）', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const resp = {
        code: 200,
        type: 'OK',
        data: { state: '0' },
        message: '没有查到正在执行的任务信息',
        operationObject: '',
      }
      request.mockResolvedValue(resp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(result.data.state).toBe('0')
      expect(result.message).toBe('没有查到正在执行的任务信息')
    })

    it('应该处理状态1（已完成）', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const resp = {
        code: 200,
        type: 'OK',
        data: { state: '1' },
        message: '任务执行成功',
        operationObject: '',
      }
      request.mockResolvedValue(resp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(result.data.state).toBe('1')
      expect(result.message).toBe('任务执行成功')
    })

    it('应该处理状态2（执行中）', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const resp = {
        code: 200,
        type: 'OK',
        data: { state: '2' },
        message: '任务正在执行中',
        operationObject: '',
      }
      request.mockResolvedValue(resp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(result.data.state).toBe('2')
      expect(result.message).toBe('任务正在执行中')
    })

    it('应该处理状态3（执行失败）', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const resp = {
        code: 200,
        type: 'OK',
        data: { state: '3' },
        message: '任务执行失败',
        operationObject: '',
      }
      request.mockResolvedValue(resp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(result.data.state).toBe('3')
      expect(result.message).toBe('任务执行失败')
    })

    it('应该处理状态4（终止执行）', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const resp = {
        code: 200,
        type: 'OK',
        data: { state: '4' },
        message: '任务终止执行',
        operationObject: '',
      }
      request.mockResolvedValue(resp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(result.data.state).toBe('4')
      expect(result.message).toBe('任务终止执行')
    })

    it('应该处理状态5（终止执行失败）', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const resp = {
        code: 200,
        type: 'OK',
        data: { state: '5' },
        message: '任务终止执行失败',
        operationObject: '',
      }
      request.mockResolvedValue(resp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(result.data.state).toBe('5')
      expect(result.message).toBe('任务终止执行失败')
    })

    it('应该处理状态6（未知状态）', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const resp = {
        code: 200,
        type: 'OK',
        data: { state: '6' },
        message: '系统服务存在异常, 联系研发, 存在未知的任务状态',
        operationObject: '',
      }
      request.mockResolvedValue(resp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(result.data.state).toBe('6')
      expect(result.message).toBe('系统服务存在异常, 联系研发, 存在未知的任务状态')
    })

    it('应该处理错误响应', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const errorResp = {
        code: 500,
        type: 'ERROR',
        data: null,
        message: '查询任务状态失败',
        operationObject: '',
      }
      request.mockResolvedValue(errorResp)

      const result = await getEntrysourceListByClassfyTaskState(params)

      expect(result.code).toBe(500)
      expect(result.type).toBe('ERROR')
      expect(result.message).toBe('查询任务状态失败')
    })
  })

  describe('getEntrysourceListByClassfyResult (获取任务结果)', () => {
    it('应该正确调用request并传递参数', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const successResp = {
        code: 200,
        type: 'OK',
        data: {
          list: {
            'type1': {
              type: 'type1',
              sourceFileAndEntryVO: [
                {
                  sourceFile: 'file1',
                  entry: 'entry1',
                },
              ],
            },
          },
        },
        message: '获取任务结果成功',
        operationObject: '',
      }
      request.mockResolvedValue(successResp)

      const result = await getEntrysourceListByClassfyResult(params)

      expect(request).toHaveBeenCalledTimes(1)
      expect(request).toHaveBeenCalledWith({
        url: '/backendInfo/getEntrysourceListByClassfyResult',
        method: 'POST',
        params,
      })
      expect(result.code).toBe(200)
      expect(result.data).toHaveProperty('list')
    })

    it('应该处理成功响应结构', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const successResp = {
        code: 200,
        type: 'OK',
        data: {
          list: {
            'type1': {
              type: 'type1',
              sourceFileAndEntryVO: [],
            },
          },
        },
        message: '获取任务结果成功',
        operationObject: '',
      }
      request.mockResolvedValue(successResp)

      const result = await getEntrysourceListByClassfyResult(params)

      expect(result).toHaveProperty('code')
      expect(result).toHaveProperty('type')
      expect(result).toHaveProperty('data')
      expect(result.data).toHaveProperty('list')
      expect(result.code).toBe(200)
    })

    it('应该处理空结果', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const successResp = {
        code: 200,
        type: 'OK',
        data: {
          list: {},
        },
        message: '获取任务结果成功',
        operationObject: '',
      }
      request.mockResolvedValue(successResp)

      const result = await getEntrysourceListByClassfyResult(params)

      expect(result.data.list).toEqual({})
    })

    it('应该处理错误响应', async () => {
      const params = {
        classfyID: 'b602e222-cabf-4879-9a1e-1ea35254f68b',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const errorResp = {
        code: 500,
        type: 'ERROR',
        data: null,
        message: '获取任务结果失败',
        operationObject: '',
      }
      request.mockResolvedValue(errorResp)

      const result = await getEntrysourceListByClassfyResult(params)

      expect(result.code).toBe(500)
      expect(result.type).toBe('ERROR')
      expect(result.message).toBe('获取任务结果失败')
    })
  })
})
