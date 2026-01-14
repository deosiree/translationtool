import { describe, it, expect, vi, beforeEach } from 'vitest'
import { entryBatchImportExcel } from '@/utils/excelUtils'

// Mock ant-design-vue notification
vi.mock('ant-design-vue', () => ({
  notification: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

// Mock API
vi.mock('@/http/api/entryManage', () => ({
  entryImportExcle: vi.fn()
}))

import { notification } from 'ant-design-vue'
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

      entryImportExcle.mockResolvedValue({ type: 'SUCCESS' })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(entryImportExcle).toHaveBeenCalledTimes(3)
      expect(entryImportExcle).toHaveBeenCalledWith({ transType: 'zh' }, formData)
      expect(entryImportExcle).toHaveBeenCalledWith({ transType: 'en' }, formData)
      expect(entryImportExcle).toHaveBeenCalledWith({ transType: 'fr' }, formData)
      expect(notification.success).toHaveBeenCalledWith({
        message: '导入成功！',
        description: 'zh, en, fr导入成功！',
        duration: 0
      })
      expect(notification.error).not.toHaveBeenCalled()
      expect(result).toEqual({ success: '数据为空' })
    })

    it('应该处理部分语言导入失败的情况', async () => {
      const translateTypes = ['zh', 'en', 'fr']
      const formData = new FormData()

      entryImportExcle
        .mockResolvedValueOnce({ type: 'SUCCESS' }) // zh 成功
        .mockRejectedValueOnce({ message: '文件格式错误' }) // en 失败
        .mockResolvedValueOnce({ type: 'SUCCESS' }) // fr 成功

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(entryImportExcle).toHaveBeenCalledTimes(3)
      expect(notification.success).toHaveBeenCalledWith({
        message: '导入成功！',
        description: 'zh, fr导入成功！',
        duration: 0
      })
      expect(notification.error).toHaveBeenCalledWith({
        message: '导入失败！',
        description: '文件格式错误：en',
        duration: 0
      })
      expect(result).toEqual({ error: '有失败的导入，提供失败词条的相关文件下载' })
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

      expect(notification.success).not.toHaveBeenCalled()
      expect(notification.error).toHaveBeenCalledWith({
        message: '导入失败！',
        description: '文件格式错误：zh, en, de；网络错误：fr',
        duration: 0
      })
      expect(result).toEqual({ error: '有失败的导入，提供失败词条的相关文件下载' })
    })

    it('应该处理所有语言导入失败的情况', async () => {
      const translateTypes = ['zh', 'en']
      const formData = new FormData()

      entryImportExcle
        .mockRejectedValueOnce({ message: '文件格式错误' })
        .mockRejectedValueOnce({ data: { message: '服务器错误' } })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(notification.success).not.toHaveBeenCalled()
      expect(notification.error).toHaveBeenCalledWith({
        message: '导入失败！',
        description: '文件格式错误：zh；服务器错误：en',
        duration: 0
      })
      expect(result).toEqual({ error: '有失败的导入，提供失败词条的相关文件下载' })
    })

    it('应该处理错误对象没有 message 的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      entryImportExcle.mockRejectedValueOnce({})

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(notification.error).toHaveBeenCalledWith({
        message: '导入失败！',
        description: '未知错误：zh',
        duration: 0
      })
      expect(result).toEqual({ error: '有失败的导入，提供失败词条的相关文件下载' })
    })

    it('应该处理错误对象有 data.message 的情况', async () => {
      const translateTypes = ['zh']
      const formData = new FormData()

      entryImportExcle.mockRejectedValueOnce({
        data: { message: '后端返回的错误消息' }
      })

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(notification.error).toHaveBeenCalledWith({
        message: '导入失败！',
        description: '后端返回的错误消息：zh',
        duration: 0
      })
      expect(result).toEqual({ error: '有失败的导入，提供失败词条的相关文件下载' })
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
      expect(notification.error).toHaveBeenCalledWith({
        message: '导入过程发生异常！',
        description: expect.stringContaining(''),
        duration: 0
      })
      expect(result).toHaveProperty('error')
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
      expect(notification.error).toHaveBeenCalledWith({
        message: '导入过程发生异常！',
        description: '未知错误',
        duration: 0
      })
      expect(result).toEqual({ error: '未知错误' })
    })

    it('应该正确记录控制台日志', async () => {
      const translateTypes = ['zh', 'en']
      const formData = new FormData()
      formData.append('file', new Blob(['test'], { type: 'text/csv' }))

      entryImportExcle
        .mockResolvedValueOnce({ type: 'SUCCESS' })
        .mockRejectedValueOnce({ message: '错误消息' })

      await entryBatchImportExcel(translateTypes, formData)

      expect(console.log).toHaveBeenCalledWith('参数', translateTypes, formData)
      expect(console.log).toHaveBeenCalledWith('en导入失败原因', expect.any(Object))
    })

    it('应该处理空的语言列表', async () => {
      const translateTypes = []
      const formData = new FormData()

      const result = await entryBatchImportExcel(translateTypes, formData)

      expect(entryImportExcle).not.toHaveBeenCalled()
      expect(notification.success).not.toHaveBeenCalled()
      expect(notification.error).not.toHaveBeenCalled()
      expect(result).toEqual({ success: '数据为空' })
    })
  })
})
