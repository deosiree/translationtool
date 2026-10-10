/**
 * entryManage API 单元测试
 * 测试 entryImportExcle_v2 和 entryValidate_v2 两个 v2 版本的 API
 * 
 * 注意：当前使用 Mock 接口，响应格式符合新 API 文档
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'

// IMPORTANT:
// src/http/api/entryManage.js 当前接口走真实 request/requestMultipart（axios 实例）。
// 单测需要 mock request 层，避免 jsdom 下发真实网络请求。
vi.mock('@/http/request', () => ({
  default: vi.fn(),
  requestMultipart: vi.fn(),
}))

// NOTE:
// 下面这段是基于 mock entryManage 模块的单测配置，当前阶段改为贴真实 API，
// 保留但暂时不用，后续如果需要切回 mock，可以直接恢复。
// vi.mock('@/http/api/mock/entryManage', () => ({
//   entryImportExcle: vi.fn(),
//   entryImportExcle_v2: vi.fn(),
//   entryValidate_v2: vi.fn(),
// }))

import request, { requestMultipart } from '@/http/request'
// import {
//   entryImportExcle_v2 as mockEntryImportExcle_v2,
// } from '@/http/api/mock/entryManage'
import {
  entryImportExcle,
  entryImportExcle_v2,
  entryValidate_v2,
  createEntrysourceListByClassfyTask,
} from '@/http/api/entryManage'

describe('entryManage API - v1/v2 版本', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(console, 'log').mockImplementation(() => { })
    vi.spyOn(console, 'error').mockImplementation(() => { })
    vi.spyOn(console, 'warn').mockImplementation(() => { })
  })

  describe('entryImportExcle (v1 更新接口)', () => {
    it('应返回 200/OK 的成功结构', async () => {
      const params = {}
      const formData = new FormData()
      const successResp = {
        code: 200,
        type: 'OK',
        data: { globalMessage: null, failedEntryInfos: [], exceptionVOs: [] },
        message: null,
        operationObject: ''
      }
      requestMultipart.mockResolvedValue(successResp)

      const result = await entryImportExcle(params, formData)

      expect(requestMultipart).toHaveBeenCalledTimes(1)
      expect(result.code).toBe(200)
      expect(result.type).toBe('OK')
      expect(result.data.failedEntryInfos).toEqual([])
      expect(result.data.exceptionVOs).toEqual([])
    })

    it('应返回 201/ERROR 的部分失败结构', async () => {
      const params = {}
      const formData = new FormData()
      const failureResp = {
        code: 201,
        type: 'ERROR',
        data: {
          globalMessage: '更新词条翻译时部分词条更新后存在警告和异常信息, 总共有1个信息',
          failedEntryInfos: [{ id: '1', entry: '词条1', english: 'value1' }],
          exceptionVOs: [{ message: '异常1', resolvedMethodMessage: '处理方式1' }]
        },
        message: '词条翻译更新存在异常, 请查看相关日志信息',
        operationObject: ''
      }
      requestMultipart.mockResolvedValue(failureResp)

      const result = await entryImportExcle(params, formData)

      expect(result.code).toBe(201)
      expect(result.type).toBe('ERROR')
      expect(result.data.globalMessage).toContain('警告和异常信息')
      expect(result.data.failedEntryInfos).toHaveLength(1)
      expect(result.data.exceptionVOs).toHaveLength(1)
    })
  })

  describe('entryImportExcle_v2 (更新接口)', () => {
    it('应该正确调用 requestMultipart 并传递参数', async () => {
      const params = {}
      const formData = new FormData()
      formData.append('dedupExcel', new Blob(['test'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
      formData.append('payload', JSON.stringify({
        options: { emptyStringAsValue: true },
        rules: [{ taskType: 'backfillFields', params: { backfillFields: ['englishTranslate'] } }]
      }))

      const mockResponse = {
        code: 200,
        message: '导入成功',
        data: { success: true }
      }
      requestMultipart.mockResolvedValue(mockResponse)

      const result = await entryImportExcle_v2(params, formData)

      expect(requestMultipart).toHaveBeenCalledTimes(1)
      expect(requestMultipart).toHaveBeenCalledWith({
        url: "/entryInfo/entryImportExcle_v2",
        method: "POST",
        params,
        data: formData,
      })
      expect(result).toEqual(mockResponse)
    })

    it('应该处理成功响应 (code: 200)', async () => {
      const params = {}
      const formData = new FormData()

      const successResponse = {
        code: 200,
        message: '导入成功'
      }
      requestMultipart.mockResolvedValue(successResponse)

      const result = await entryImportExcle_v2(params, formData)

      expect(result.code).toBe(200)
      expect(result.message).toBe('导入成功')
    })

    it('应该处理部分失败响应 (code: 201)', async () => {
      const params = {}
      const formData = new FormData()

      const failureResponse = {
        code: 201,
        message: '导入存在失败',
        data: {
          failedEntryInfos: [
            { id: '1', entry: '测试词条1', englishTranslate: '测试值1' },
            { id: '2', entry: '测试词条2', englishTranslate: '测试值2' }
          ],
          exceptionVos: [
            { id: '1', message: '异常信息1' }
          ],
          globalMessage: '部分词条导入失败'
        }
      }
      requestMultipart.mockResolvedValue(failureResponse)

      const result = await entryImportExcle_v2(params, formData)

      expect(result.code).toBe(201)
      expect(result.message).toBe('导入存在失败')
      expect(result.data.failedEntryInfos).toHaveLength(2)
      expect(result.data.exceptionVos).toHaveLength(1)
      expect(result.data.globalMessage).toBe('部分词条导入失败')
    })

    it('应该处理多次调用', async () => {
      const params = {}
      const formData = new FormData()

      // 模拟多次调用，返回不同的响应
      const responses = [
        { code: 200, message: '导入成功' },
        { code: 201, message: '导入存在失败', data: { failedEntryInfos: [], exceptionVos: [], globalMessage: '失败' } },
        { code: 200, message: '导入成功' }
      ]

      for (const response of responses) {
        requestMultipart.mockResolvedValueOnce(response)
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
      expect(requestMultipart).toHaveBeenCalledTimes(3)
    })

    it('应该验证响应结构完整性', async () => {
      const params = {}
      const formData = new FormData()

      const response = {
        code: 200,
        message: '导入成功'
      }
      requestMultipart.mockResolvedValue(response)

      const result = await entryImportExcle_v2(params, formData)

      // 验证响应结构
      expect(result).toHaveProperty('code')
      expect(result).toHaveProperty('message')
    })
  })

  describe('entryValidate_v2 (校验接口)', () => {
    it('应该正确调用mock函数并传递参数', async () => {
      const params = {}
      const formData = new FormData()
      formData.append('dedupOriginExcel', new Blob(['test'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
      formData.append('dedupExcel', new Blob(['test'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
      formData.append('payload', JSON.stringify({
        options: { emptyStringAsValue: true, failFast: false },
        rules: [
          { taskType: 'checkFields', params: { checkFields: ['entry', 'comment'] } },
          { taskType: 'backfillFields', params: { backfillFields: ['englishTranslate'] } }
        ]
      }))

      const mockResponse = {
        success: true,
        canBackFill: true,
        summary: null,
        issues: [],
        previews: [],
        attachments: { issueLog: [] }
      }
      requestMultipart.mockResolvedValue(mockResponse)

      const result = await entryValidate_v2(params, formData)

      expect(requestMultipart).toHaveBeenCalledTimes(1)
      expect(requestMultipart).toHaveBeenCalledWith({
        url: "/entryInfo/checkBeforeUpdateTranslationByFile",
        method: "POST",
        params,
        data: formData,
      })
      expect(result).toEqual(mockResponse)
    })

    it('应该处理成功响应（完全通过）', async () => {
      const params = {}
      const formData = new FormData()

      const successResponse = {
        success: true,
        canBackFill: true,
        summary: {
          totalOriginRows: 1200,
          totalDedupRows: 300,
          affectedRows: 1180,
          willUpdateCells: 3500
        },
        issues: [],
        previews: [],
        attachments: { issueLog: [] }
      }
      requestMultipart.mockResolvedValue(successResponse)

      const result = await entryValidate_v2(params, formData)

      expect(result.success).toBe(true)
      expect(result.canBackFill).toBe(true)
      expect(result.summary).toBeDefined()
      expect(result.issues).toEqual([])
      expect(result.previews).toEqual([])
      expect(Array.isArray(result.attachments.issueLog)).toBe(true)
    })

    it('应该处理有警告但可回填的响应', async () => {
      const params = {}
      const formData = new FormData()

      const warnResponse = {
        success: true,
        canBackFill: true,
        summary: {
          totalOriginRows: 1200,
          totalDedupRows: 300,
          affectedRows: 1180,
          skippedRows: 20
        },
        issues: [
          {
            level: 'WARN',
            type: 'EMPTY_VALUE_SKIP',
            fieldKey: 'englishTranslate',
            id: 'c321',
            message: '字段在去重后文件中为空，已跳过更新'
          }
        ],
        previews: [],
        attachments: {
          invalidExcel: {
            fileName: 'backfill_invalid_rows.xlsx',
            downloadUrl: '/api/backfill/validate/files/invalid-excel'
          },
          issueLog: []
        }
      }
      requestMultipart.mockResolvedValue(warnResponse)

      const result = await entryValidate_v2(params, formData)

      expect(result.success).toBe(true)
      expect(result.canBackFill).toBe(true)
      expect(result.issues).toHaveLength(1)
      expect(result.issues[0].level).toBe('WARN')
      expect(result.attachments).toBeDefined()
      expect(result.attachments.invalidExcel).toBeDefined()
      expect(Array.isArray(result.attachments.issueLog)).toBe(true)
    })

    it('应该处理致命错误响应（禁止回填）', async () => {
      const params = {}
      const formData = new FormData()

      const fatalResponse = {
        success: false,
        canBackFill: false,
        issues: [
          {
            level: 'FATAL',
            type: 'MAPPING_MISSING_PARENT',
            message: '去重后 id 在映射文件中不存在'
          }
        ],
        previews: [],
        attachments: { issueLog: [] }
      }
      requestMultipart.mockResolvedValue(fatalResponse)

      const result = await entryValidate_v2(params, formData)

      expect(result.success).toBe(false)
      expect(result.canBackFill).toBe(false)
      expect(result.issues).toHaveLength(1)
      expect(result.issues[0].level).toBe('FATAL')
    })

    it('应该处理多次调用', async () => {
      const params = {}
      const formData = new FormData()

      // 模拟多次调用，返回不同的响应
      const responses = [
        {
          success: true,
          canBackFill: true,
          summary: { totalOriginRows: 100, totalDedupRows: 50, affectedRows: 100, willUpdateCells: 200 },
          issues: [],
          previews: [],
          attachments: { issueLog: [] }
        },
        {
          success: false,
          canBackFill: false,
          issues: [{ level: 'FATAL', type: 'MAPPING_MISSING_PARENT', message: '错误' }],
          previews: [],
          attachments: { issueLog: [] }
        },
        {
          success: true,
          canBackFill: true,
          summary: { totalOriginRows: 100, totalDedupRows: 50, affectedRows: 100, willUpdateCells: 200 },
          issues: [],
          previews: [],
          attachments: { issueLog: [] }
        }
      ]

      for (const response of responses) {
        requestMultipart.mockResolvedValueOnce(response)
      }

      const results = await Promise.all([
        entryValidate_v2(params, formData),
        entryValidate_v2(params, formData),
        entryValidate_v2(params, formData)
      ])

      expect(results).toHaveLength(3)
      expect(results[0].success).toBe(true)
      expect(results[1].success).toBe(false)
      expect(results[2].success).toBe(true)
      expect(requestMultipart).toHaveBeenCalledTimes(3)
    })

    it('应该验证响应结构完整性（成功场景）', async () => {
      const params = {}
      const formData = new FormData()

      const successResponse = {
        success: true,
        canBackFill: true,
        summary: {
          totalOriginRows: 1200,
          totalDedupRows: 300,
          affectedRows: 1180,
          willUpdateCells: 3500
        },
        issues: [],
        previews: [],
        attachments: { issueLog: [] }
      }
      requestMultipart.mockResolvedValue(successResponse)

      const result = await entryValidate_v2(params, formData)

      expect(result).toHaveProperty('success')
      expect(result).toHaveProperty('canBackFill')
      expect(result).toHaveProperty('summary')
      expect(result).toHaveProperty('issues')
      expect(result).toHaveProperty('previews')
      expect(result).toHaveProperty('attachments')
      expect(Array.isArray(result.issues)).toBe(true)
      expect(Array.isArray(result.previews)).toBe(true)
      expect(Array.isArray(result.attachments.issueLog)).toBe(true)
    })

    it('应该验证响应结构完整性（失败场景）', async () => {
      const params = {}
      const formData = new FormData()

      const failureResponse = {
        success: false,
        canBackFill: false,
        issues: [
          {
            level: 'FATAL',
            type: 'MAPPING_MISSING_PARENT',
            message: '去重后 id 在映射文件中不存在'
          }
        ],
        previews: [],
        attachments: { issueLog: [] }
      }
      requestMultipart.mockResolvedValue(failureResponse)

      const result = await entryValidate_v2(params, formData)

      expect(result).toHaveProperty('success')
      expect(result).toHaveProperty('canBackFill')
      expect(result).toHaveProperty('issues')
      expect(Array.isArray(result.issues)).toBe(true)
      expect(result.issues[0]).toHaveProperty('level')
      expect(result.issues[0]).toHaveProperty('type')
      expect(result.issues[0]).toHaveProperty('message')
      expect(result).toHaveProperty('previews')
      expect(result).toHaveProperty('attachments')
    })

    it('应该验证 Issue 结构完整性', async () => {
      const params = {}
      const formData = new FormData()

      const response = {
        success: true,
        canBackFill: true,
        summary: { totalOriginRows: 100, totalDedupRows: 50, affectedRows: 100, willUpdateCells: 200 },
        issues: [
          {
            level: 'WARN',
            type: 'SPECIAL_CHAR_MISMATCH',
            id: 'c321',
            fieldKey: 'englishTranslate',
            message: '占位符不一致'
          }
        ],
        previews: [],
        attachments: { issueLog: [] }
      }
      requestMultipart.mockResolvedValue(response)

      const result = await entryValidate_v2(params, formData)

      const issue = result.issues[0]
      expect(issue).toHaveProperty('level')
      expect(issue).toHaveProperty('type')
      expect(issue).toHaveProperty('id')
      expect(issue).toHaveProperty('fieldKey')
      expect(issue).toHaveProperty('message')
      expect(['WARN', 'ERROR', 'FATAL']).toContain(issue.level)
    })
  })

  describe('createEntrysourceListByClassfyTask (创建更新词条任务)', () => {
    it('应该正确调用request并传递参数', async () => {
      const params = {
        classifyID: 'c210e64a-5553-40df-8eac-a04325251918',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const successResp = {
        code: 200,
        type: 'OK',
        data: null,
        message: '创建词条更新任务成功',
        operationObject: '',
      }
      request.mockResolvedValue(successResp)

      const result = await createEntrysourceListByClassfyTask(params)

      expect(request).toHaveBeenCalledTimes(1)
      expect(request).toHaveBeenCalledWith({
        url: '/entryInfo/getEntrysourceListByClassfy',
        method: 'POST',
        params,
      })
      expect(result.code).toBe(200)
      expect(result.type).toBe('OK')
      expect(result.message).toBe('创建词条更新任务成功')
    })

    it('应该处理成功响应', async () => {
      const params = {
        classifyID: 'c210e64a-5553-40df-8eac-a04325251918',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const successResp = {
        code: 200,
        type: 'OK',
        data: null,
        message: '创建词条更新任务成功',
        operationObject: '',
      }
      request.mockResolvedValue(successResp)

      const result = await createEntrysourceListByClassfyTask(params)

      expect(result).toHaveProperty('code')
      expect(result).toHaveProperty('type')
      expect(result).toHaveProperty('message')
      expect(result.code).toBe(200)
    })

    it('应该处理错误响应', async () => {
      const params = {
        classifyID: 'c210e64a-5553-40df-8eac-a04325251918',
        i18nUrl: 'http://10.17.196.28:18099/',
      }
      const errorResp = {
        code: 500,
        type: 'ERROR',
        data: null,
        message: '创建任务失败',
        operationObject: '',
      }
      request.mockResolvedValue(errorResp)

      const result = await createEntrysourceListByClassfyTask(params)

      expect(result.code).toBe(500)
      expect(result.type).toBe('ERROR')
      expect(result.message).toBe('创建任务失败')
    })
  })
})
