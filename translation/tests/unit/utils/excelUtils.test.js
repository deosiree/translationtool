import { describe, it, expect, vi, beforeEach } from 'vitest'
import { entryBatchImportExcel } from '@/utils/excelUtils'

// Mock API
vi.mock('@/http/api/entryManage', () => ({
  entryImportExcle: vi.fn()
}))

import { entryImportExcle } from '@/http/api/entryManage'

describe('excelUtils - Excel 相关工具函数', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(console, 'log').mockImplementation(() => {})
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  describe('entryBatchImportExcel', () => {
    it('应该成功导入所有语言', async () => {
      const translateTypes = ['zh', 'en', 'fr']
      const formData = new FormData()
      formData.append('file', new Blob(['test'], { type: 'text/csv' }))

      entryImportExcle.mockResolvedValue({ code: 200 })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(entryImportExcle).toHaveBeenCalledTimes(3)
      expect(entryImportExcle).toHaveBeenCalledWith({ transType: 'zh' }, formData)
      expect(entryImportExcle).toHaveBeenCalledWith({ transType: 'en' }, formData)
      expect(entryImportExcle).toHaveBeenCalledWith({ transType: 'fr' }, formData)
      expect(result).toEqual({
        code: 200,
        success: ['zh', 'en', 'fr'],
        failed: new Map(),
        failedEntryInfos: [],
        exceptionVos: [],
        globalMessage: ""
      })
    })

    it('应该处理部分语言导入失败的情况', async () => {
      const translateTypes = ['zh', 'en', 'fr']
      const formData = new FormData()

      entryImportExcle
        .mockResolvedValueOnce({ code: 200 }) // zh 成功
        .mockRejectedValueOnce({ message: '文件格式错误' }) // en 失败
        .mockResolvedValueOnce({ code: 200 }) // fr 成功

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(entryImportExcle).toHaveBeenCalledTimes(3)
      // 失败信息应记录到 failed Map 中
      expect(result.failed.get('文件格式错误')).toEqual(['en'])
      expect(result.code).toBe(201)
      expect(result.success).toEqual(['zh', 'fr'])
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
      expect(result.globalMessage).toBe('导入存在失败')
    })

    it('应该处理多个语言使用相同错误消息的情况', async () => {
      const translateTypes = ['zh', 'en', 'fr', 'de']
      const formData = new FormData()

      entryImportExcle
        .mockRejectedValueOnce({ message: '文件格式错误' }) // zh 失败
        .mockRejectedValueOnce({ message: '文件格式错误' }) // en 失败
        .mockRejectedValueOnce({ message: '网络错误' }) // fr 失败
        .mockRejectedValueOnce({ message: '文件格式错误' }) // de 失败

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.failed.get('文件格式错误')).toEqual(['zh', 'en', 'de'])
      expect(result.failed.get('网络错误')).toEqual(['fr'])
      expect(result.code).toBe(201)
      expect(result.success).toEqual([])
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
      expect(result.globalMessage).toBe('导入存在失败')
    })

    it('应该处理所有语言导入失败的情况', async () => {
      const translateTypes = ['zh', 'en']
      const formData = new FormData()

      entryImportExcle
        .mockRejectedValueOnce({ message: '文件格式错误' })
        .mockRejectedValueOnce({ data: { message: '服务器错误' } })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.failed.get('文件格式错误')).toEqual(['zh'])
      expect(result.failed.get('服务器错误')).toEqual(['en'])
      expect(result.code).toBe(201)
      expect(result.success).toEqual([])
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
      expect(result.globalMessage).toBe('导入存在失败')
    })

    it('应该处理错误对象没有 message 的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      entryImportExcle.mockRejectedValueOnce({})

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.failed.get('未知错误')).toEqual(['zh'])
      expect(result.code).toBe(201)
      expect(result.success).toEqual([])
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
      expect(result.globalMessage).toBe('导入存在失败')
    })

    it('应该处理错误对象有 data.message 的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      entryImportExcle.mockRejectedValueOnce({
        data: { message: '后端返回的错误消息' }
      })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.failed.get('后端返回的错误消息')).toEqual(['zh'])
      expect(result.code).toBe(201)
      expect(result.success).toEqual([])
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
      expect(result.globalMessage).toBe('导入存在失败')
    })

    it('应该处理整个函数抛出异常的情况', async () => {
      // 模拟 translateTypes 不是可迭代对象，触发外层 try-catch
      const translateTypes = null
      const formData = new FormData()

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(console.error).toHaveBeenCalledWith(
        'entryBatchImportExcel 发生异常：',
        expect.any(TypeError)
      )
      expect(result.code).toBe(201)
      expect(result.success).toEqual([])
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
    })

    it('应该处理异常没有 message 的情况', async () => {
      // 模拟一个在迭代时抛出没有 message 的错误对象
      const translateTypes = {
        [Symbol.iterator]: function* () {
          throw {} // 抛出没有 message 的对象
        }
      }
      const formData = new FormData()

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(console.error).toHaveBeenCalled()
      expect(result.code).toBe(201)
      expect(result.success).toEqual([])
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
      expect(result.globalMessage).toBe('未知错误')
    })

    it('应该正确记录控制台日志', async () => {
      const translateTypes = ['zh', 'en']
      const formData = new FormData()
      formData.append('file', new Blob(['test'], { type: 'text/csv' }))

      entryImportExcle
        .mockResolvedValueOnce({ code: 200 })
        .mockRejectedValueOnce({ message: '错误消息' })

      await entryBatchImportExcel(translateTypes, formData)

      expect(console.log).toHaveBeenCalledWith('参数', translateTypes, formData)
      expect(console.log).toHaveBeenCalledWith('en导入响应：', expect.any(Object))
    })

    it('应该处理空的语言列表', async () => {
      const translateTypes = []
      const formData = new FormData()

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(entryImportExcle).not.toHaveBeenCalled()
      expect(result).toEqual({
        code: 200,
        success: [],
        failed: new Map(),
        failedEntryInfos: [],
        exceptionVos: [],
        globalMessage: ""
      })
    })

    it('应该处理 code=201 且包含 failedEntryInfos 的情况', async () => {
      const translateTypes = ['zh', 'en']
      const formData = new FormData()

      const mockFailedEntryInfos = [
        {
          id: '1',
          entry: '测试词条1',
          english: 'test entry 1'
        },
        {
          entryInfoVO: {
            entryInfoEntitie: [
              { id: '2', entry: '测试词条2', english: 'test entry 2' },
              { id: '3', entry: '测试词条3', english: 'test entry 3' }
            ]
          }
        }
      ]

      entryImportExcle
        .mockRejectedValueOnce({
          response: {
            data: {
              code: 201,
              data: {
                globalMessage: '更新词条翻译时部分词条更新后存在警告和异常信息',
                failedEntryInfos: mockFailedEntryInfos,
                exceptionVos: []
              }
            }
          }
        })
        .mockResolvedValueOnce({ code: 200 }) // en 成功

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(entryImportExcle).toHaveBeenCalledTimes(2)
      expect(result.code).toBe(201)
      expect(result.failedEntryInfos).toEqual(mockFailedEntryInfos)
      expect(result.exceptionVos).toEqual([])
      expect(result.globalMessage).toBe('更新词条翻译时部分词条更新后存在警告和异常信息')
      expect(result.success).toContain('en')
    })

    it('应该处理 code=201 且包含 exceptionVos 的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      const mockExceptionVos = [
        {
          message: '父节点id: "xxx"的词条送翻记录丢失',
          resolvedMethodMessage: '检查该id的词条是否送翻'
        },
        {
          message: '另一个异常信息',
          resolvedMethodMessage: '解决方案',
          entryInfoVO: {
            entryInfoEntitie: [],
            totalSize: 0
          }
        }
      ]

      entryImportExcle.mockRejectedValueOnce({
        data: {
          code: 201,
          data: {
            globalMessage: '总共有2个异常信息',
            failedEntryInfos: [],
            exceptionVos: mockExceptionVos
          }
        }
      })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.code).toBe(201)
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual(mockExceptionVos)
      expect(result.globalMessage).toBe('总共有2个异常信息')
    })

    it('应该处理 code=201 且同时包含 failedEntryInfos 和 exceptionVos 的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      const mockFailedEntryInfos = [
        { id: '1', entry: '测试词条1', english: 'test entry 1' }
      ]

      const mockExceptionVos = [
        {
          message: '异常信息1',
          resolvedMethodMessage: '解决方案1'
        }
      ]

      entryImportExcle.mockRejectedValueOnce({
        response: {
          data: {
            code: 201,
            data: {
              globalMessage: '更新词条翻译时部分词条更新后存在警告和异常信息, 总共有1个信息',
              failedEntryInfos: mockFailedEntryInfos,
              exceptionVos: mockExceptionVos
            }
          }
        }
      })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.code).toBe(201)
      expect(result.failedEntryInfos).toEqual(mockFailedEntryInfos)
      expect(result.exceptionVos).toEqual(mockExceptionVos)
      expect(result.globalMessage).toBe('更新词条翻译时部分词条更新后存在警告和异常信息, 总共有1个信息')
    })

    it('应该兼容 exceptionVos 和 exceptionVOs 两种字段名', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      const mockExceptionVOs = [
        {
          message: '使用 exceptionVOs 字段名',
          resolvedMethodMessage: '解决方案'
        }
      ]

      entryImportExcle.mockRejectedValueOnce({
        data: {
          code: 201,
          data: {
            globalMessage: '测试 exceptionVOs 字段名',
            failedEntryInfos: [],
            exceptionVOs: mockExceptionVOs // 注意这里是大写的 O
          }
        }
      })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.code).toBe(201)
      expect(result.exceptionVos).toEqual(mockExceptionVOs)
      expect(result.globalMessage).toBe('测试 exceptionVOs 字段名')
    })

    it('应该合并多个语种的失败信息', async () => {
      const translateTypes = ['zh', 'en', 'fr']
      const formData = new FormData()

      const mockFailedEntryInfos1 = [
        { id: '1', entry: '词条1', english: 'entry 1' }
      ]

      const mockFailedEntryInfos2 = [
        { id: '2', entry: '词条2', english: 'entry 2' }
      ]

      const mockExceptionVos1 = [
        { message: '异常1', resolvedMethodMessage: '解决方案1' }
      ]

      const mockExceptionVos2 = [
        { message: '异常2', resolvedMethodMessage: '解决方案2' }
      ]

      entryImportExcle
        .mockRejectedValueOnce({
          response: {
            data: {
              code: 201,
              data: {
                globalMessage: 'zh语种失败',
                failedEntryInfos: mockFailedEntryInfos1,
                exceptionVos: mockExceptionVos1
              }
            }
          }
        })
        .mockRejectedValueOnce({
          data: {
            code: 201,
            data: {
              globalMessage: 'en语种失败',
              failedEntryInfos: mockFailedEntryInfos2,
              exceptionVos: mockExceptionVos2
            }
          }
        })
        .mockResolvedValueOnce({ code: 200 }) // fr 成功

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.code).toBe(201)
      // 应该合并所有失败词条
      expect(result.failedEntryInfos).toHaveLength(2)
      expect(result.failedEntryInfos).toContainEqual(mockFailedEntryInfos1[0])
      expect(result.failedEntryInfos).toContainEqual(mockFailedEntryInfos2[0])
      // 应该合并所有异常信息
      expect(result.exceptionVos).toHaveLength(2)
      expect(result.exceptionVos).toContainEqual(mockExceptionVos1[0])
      expect(result.exceptionVos).toContainEqual(mockExceptionVos2[0])
      // globalMessage 应该取最后一个非空的
      expect(result.globalMessage).toBe('en语种失败')
      expect(result.success).toContain('fr')
    })

    it('应该处理 code=201 但 data 为空的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      entryImportExcle.mockRejectedValueOnce({
        response: {
          data: {
            code: 201,
            data: null
          }
        }
      })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.code).toBe(201)
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
      // 当 data 为 null 时，globalMessage 为空字符串
      expect(result.globalMessage).toBe('')
    })

    it('应该处理 code=201 但缺少某些字段的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      entryImportExcle.mockRejectedValueOnce({
        data: {
          code: 201,
          data: {
            globalMessage: '只有globalMessage',
            // 没有 failedEntryInfos 和 exceptionVos
          }
        }
      })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.code).toBe(201)
      expect(result.failedEntryInfos).toEqual([])
      expect(result.exceptionVos).toEqual([])
      expect(result.globalMessage).toBe('只有globalMessage')
    })

    it('应该处理 failedEntryInfos 包含 entryInfoVO 结构的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      const mockFailedEntryInfos = [
        {
          entryInfoVO: {
            entryInfoEntitie: [
              { id: '1', entry: '词条1', english: 'entry 1' },
              { id: '2', entry: '词条2', english: 'entry 2' }
            ],
            totalSize: 2
          }
        },
        {
          entryInfoVO: {
            entryInfoEntities: [ // 兼容不同的字段名
              { id: '3', entry: '词条3', english: 'entry 3' }
            ],
            totalSize: 1
          }
        }
      ]

      entryImportExcle.mockRejectedValueOnce({
        response: {
          data: {
            code: 201,
            data: {
              globalMessage: '测试 entryInfoVO 结构',
              failedEntryInfos: mockFailedEntryInfos,
              exceptionVos: []
            }
          }
        }
      })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(result.code).toBe(201)
      expect(result.failedEntryInfos).toEqual(mockFailedEntryInfos)
      expect(result.globalMessage).toBe('测试 entryInfoVO 结构')
    })
  })
})
