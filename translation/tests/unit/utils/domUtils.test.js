import { describe, it, expect, vi, beforeEach } from 'vitest'
import { clickInput, setModalAriaHidden } from '@/utils/domUtils'

describe('domUtils - DOM/UI工具函数', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('clickInput', () => {
    it('应该阻止事件冒泡', () => {
      const mockEvent = {
        stopPropagation: vi.fn()
      }
      const mockVm = {}

      clickInput(mockVm, mockEvent)

      expect(mockEvent.stopPropagation).toHaveBeenCalled()
    })
  })

  describe('setModalAriaHidden', () => {
    it('应该设置模态框的 aria-hidden 属性', async () => {
      // 创建模拟的 DOM 结构
      const mockChild1 = {
        setAttribute: vi.fn()
      }
      const mockChild2 = {
        setAttribute: vi.fn()
      }
      const mockModal = {
        childNodes: [mockChild1, mockChild2]
      }

      const mockDocument = {
        getElementsByClassName: vi.fn(() => [mockModal])
      }

      const mockVm = {
        $nextTick: vi.fn((callback) => {
          // 同步执行回调以简化测试
          callback()
        })
      }

      setModalAriaHidden(mockVm, mockDocument)

      expect(mockVm.$nextTick).toHaveBeenCalled()
      expect(mockDocument.getElementsByClassName).toHaveBeenCalledWith('ant-modal')
      expect(mockChild1.setAttribute).toHaveBeenCalledWith('aria-hidden', 'false')
      expect(mockChild2.setAttribute).toHaveBeenCalledWith('aria-hidden', 'false')
    })

    it('应该处理没有模态框的情况', async () => {
      const mockDocument = {
        getElementsByClassName: vi.fn(() => [])
      }

      const mockVm = {
        $nextTick: vi.fn((callback) => {
          callback()
        })
      }

      setModalAriaHidden(mockVm, mockDocument)

      expect(mockDocument.getElementsByClassName).toHaveBeenCalledWith('ant-modal')
    })

    it('应该处理没有子节点的情况', async () => {
      const mockModal = {
        childNodes: []
      }

      const mockDocument = {
        getElementsByClassName: vi.fn(() => [mockModal])
      }

      const mockVm = {
        $nextTick: vi.fn((callback) => {
          callback()
        })
      }

      setModalAriaHidden(mockVm, mockDocument)

      expect(mockDocument.getElementsByClassName).toHaveBeenCalledWith('ant-modal')
    })

    it('应该处理子节点没有 setAttribute 方法的情况', async () => {
      const mockChild1 = {
        setAttribute: vi.fn()
      }
      const mockChild2 = {} // 没有 setAttribute 方法
      const mockModal = {
        childNodes: [mockChild1, mockChild2]
      }

      const mockDocument = {
        getElementsByClassName: vi.fn(() => [mockModal])
      }

      const mockVm = {
        $nextTick: vi.fn((callback) => {
          callback()
        })
      }

      setModalAriaHidden(mockVm, mockDocument)

      expect(mockChild1.setAttribute).toHaveBeenCalledWith('aria-hidden', 'false')
      // mockChild2 没有 setAttribute，不应该调用
    })

    it('应该处理多个模态框', async () => {
      const mockChild1 = { setAttribute: vi.fn() }
      const mockChild2 = { setAttribute: vi.fn() }
      const mockModal1 = { childNodes: [mockChild1] }
      const mockModal2 = { childNodes: [mockChild2] }

      const mockDocument = {
        getElementsByClassName: vi.fn(() => [mockModal1, mockModal2])
      }

      const mockVm = {
        $nextTick: vi.fn((callback) => {
          callback()
        })
      }

      setModalAriaHidden(mockVm, mockDocument)

      expect(mockChild1.setAttribute).toHaveBeenCalledWith('aria-hidden', 'false')
      expect(mockChild2.setAttribute).toHaveBeenCalledWith('aria-hidden', 'false')
    })
  })
})
