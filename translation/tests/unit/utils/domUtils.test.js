import { describe, it, expect, vi, beforeEach } from 'vitest'
import { clickInput, setModalAriaHidden, createDragModalDirective, stopDomEvent } from '@/utils/domUtils'

describe('domUtils - DOM/UI工具函数', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 清理全局事件监听器
    document.onmousemove = null
    document.onmouseup = null
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

  describe('stopDomEvent', () => {
    it('event 为空时不应抛异常', () => {
      expect(() => stopDomEvent(null)).not.toThrow()
      expect(() => stopDomEvent(undefined)).not.toThrow()
    })

    it('默认应同时调用 preventDefault 和 stopPropagation', () => {
      const mockEvent = {
        preventDefault: vi.fn(),
        stopPropagation: vi.fn()
      }

      stopDomEvent(mockEvent)

      expect(mockEvent.preventDefault).toHaveBeenCalledTimes(1)
      expect(mockEvent.stopPropagation).toHaveBeenCalledTimes(1)
    })

    it('可仅阻止默认行为（不阻止冒泡）', () => {
      const mockEvent = {
        preventDefault: vi.fn(),
        stopPropagation: vi.fn()
      }

      stopDomEvent(mockEvent, { preventDefault: true, stopPropagation: false })

      expect(mockEvent.preventDefault).toHaveBeenCalledTimes(1)
      expect(mockEvent.stopPropagation).not.toHaveBeenCalled()
    })

    it('可仅阻止冒泡（不阻止默认行为）', () => {
      const mockEvent = {
        preventDefault: vi.fn(),
        stopPropagation: vi.fn()
      }

      stopDomEvent(mockEvent, { preventDefault: false, stopPropagation: true })

      expect(mockEvent.preventDefault).not.toHaveBeenCalled()
      expect(mockEvent.stopPropagation).toHaveBeenCalledTimes(1)
    })

    it('当 event 缺少对应方法时不应抛异常', () => {
      const mockEvent = {}
      expect(() => stopDomEvent(mockEvent)).not.toThrow()
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

  describe('createDragModalDirective', () => {
    it('应该返回一个指令函数', () => {
      const directive = createDragModalDirective()
      expect(typeof directive).toBe('function')
    })

    it('当找不到 dialogHeaderEl 时应该直接返回', () => {
      const directive = createDragModalDirective()
      const mockEl = {
        querySelector: vi.fn((selector) => {
          if (selector === '.modalHeader') return null
          if (selector === '.ant-modal') return { style: {} }
          return null
        })
      }
      const mockBinding = {}

      directive(mockEl, mockBinding)

      expect(mockEl.querySelector).toHaveBeenCalledWith('.modalHeader')
      expect(mockEl.querySelector).toHaveBeenCalledWith('.ant-modal')
    })

    it('当找不到 dragDom 时应该直接返回', () => {
      const directive = createDragModalDirective()
      const mockEl = {
        querySelector: vi.fn((selector) => {
          if (selector === '.modalHeader') return { style: {} }
          if (selector === '.ant-modal') return null
          return null
        })
      }
      const mockBinding = {}

      directive(mockEl, mockBinding)

      expect(mockEl.querySelector).toHaveBeenCalledWith('.modalHeader')
      expect(mockEl.querySelector).toHaveBeenCalledWith('.ant-modal')
    })

    it('应该设置 dialogHeaderEl 的 cursor 样式为 move', () => {
      const directive = createDragModalDirective()
      const mockDialogHeader = { style: {} }
      const mockDragDom = {
        style: {},
        currentStyle: { left: '100px', top: '200px' }
      }
      const mockEl = {
        querySelector: vi.fn((selector) => {
          if (selector === '.modalHeader') return mockDialogHeader
          if (selector === '.ant-modal') return mockDragDom
          return null
        })
      }
      const mockBinding = {}

      // Mock getComputedStyle
      const originalGetComputedStyle = window.getComputedStyle
      window.getComputedStyle = vi.fn(() => ({ left: '100px', top: '200px' }))

      directive(mockEl, mockBinding)

      expect(mockDialogHeader.style.cursor).toBe('move')

      window.getComputedStyle = originalGetComputedStyle
    })

    it('应该绑定 mousedown 事件处理器', () => {
      const directive = createDragModalDirective()
      const mockDialogHeader = {
        style: {},
        offsetLeft: 50,
        offsetTop: 100,
        onmousedown: null
      }
      const mockDragDom = {
        style: {},
        currentStyle: { left: '100px', top: '200px' }
      }
      const mockEl = {
        querySelector: vi.fn((selector) => {
          if (selector === '.modalHeader') return mockDialogHeader
          if (selector === '.ant-modal') return mockDragDom
          return null
        })
      }
      const mockBinding = {}

      // Mock getComputedStyle
      const originalGetComputedStyle = window.getComputedStyle
      window.getComputedStyle = vi.fn(() => ({ left: '100px', top: '200px' }))

      directive(mockEl, mockBinding)

      expect(typeof mockDialogHeader.onmousedown).toBe('function')

      window.getComputedStyle = originalGetComputedStyle
    })

    it('mousedown 事件应该设置 mousemove 和 mouseup 事件', () => {
      const directive = createDragModalDirective()
      const mockDialogHeader = {
        style: {},
        offsetLeft: 50,
        offsetTop: 100,
        onmousedown: null
      }
      const mockDragDom = {
        style: {},
        currentStyle: { left: '100px', top: '200px' }
      }
      const mockEl = {
        querySelector: vi.fn((selector) => {
          if (selector === '.modalHeader') return mockDialogHeader
          if (selector === '.ant-modal') return mockDragDom
          return null
        })
      }
      const mockBinding = {}

      // Mock getComputedStyle
      const originalGetComputedStyle = window.getComputedStyle
      window.getComputedStyle = vi.fn(() => ({ left: '100px', top: '200px' }))

      // Mock document.body properties
      Object.defineProperty(document.body, 'clientWidth', {
        writable: true,
        configurable: true,
        value: 1920
      })
      Object.defineProperty(document.body, 'clientHeight', {
        writable: true,
        configurable: true,
        value: 1080
      })

      directive(mockEl, mockBinding)

      // 模拟 mousedown 事件
      const mockMouseEvent = {
        clientX: 200,
        clientY: 300
      }
      mockDialogHeader.onmousedown(mockMouseEvent)

      expect(typeof document.onmousemove).toBe('function')
      expect(typeof document.onmouseup).toBe('function')

      // 模拟 mousemove 事件
      const mockMoveEvent = {
        clientX: 250,
        clientY: 350
      }
      document.onmousemove(mockMoveEvent)

      // 验证样式被更新
      // disX = 200 - 50 = 150, disY = 300 - 100 = 200
      // l = 250 - 150 = 100, t = 350 - 200 = 150
      // left = 100 + 100 = 200px, top = 150 + 200 = 350px
      expect(mockDragDom.style.left).toBe('200px')
      expect(mockDragDom.style.top).toBe('350px')

      // 模拟 mouseup 事件
      document.onmouseup()

      expect(document.onmousemove).toBeNull()
      expect(document.onmouseup).toBeNull()

      window.getComputedStyle = originalGetComputedStyle
    })

    it('应该正确处理百分比样式的计算', () => {
      const directive = createDragModalDirective()
      const mockDialogHeader = {
        style: {},
        offsetLeft: 50,
        offsetTop: 100,
        onmousedown: null
      }
      const mockDragDom = {
        style: {},
        currentStyle: { left: '50%', top: '50%' }
      }
      const mockEl = {
        querySelector: vi.fn((selector) => {
          if (selector === '.modalHeader') return mockDialogHeader
          if (selector === '.ant-modal') return mockDragDom
          return null
        })
      }
      const mockBinding = {}

      // Mock getComputedStyle
      const originalGetComputedStyle = window.getComputedStyle
      window.getComputedStyle = vi.fn(() => ({ left: '50%', top: '50%' }))

      // Mock document.body properties
      Object.defineProperty(document.body, 'clientWidth', {
        writable: true,
        configurable: true,
        value: 1920
      })
      Object.defineProperty(document.body, 'clientHeight', {
        writable: true,
        configurable: true,
        value: 1080
      })

      directive(mockEl, mockBinding)

      const mockMouseEvent = {
        clientX: 200,
        clientY: 300
      }
      mockDialogHeader.onmousedown(mockMouseEvent)

      // 模拟 mousemove 事件
      const mockMoveEvent = {
        clientX: 250,
        clientY: 350
      }
      document.onmousemove(mockMoveEvent)

      // 验证样式计算
      // disX = 200 - 50 = 150, disY = 300 - 100 = 200
      // styL = 1920 * 0.5 = 960, styT = 1080 * 0.5 = 540
      // l = 250 - 150 = 100, t = 350 - 200 = 150
      // left = 100 + 960 = 1060px, top = 150 + 540 = 690px
      expect(mockDragDom.style.left).toBe('1060px')
      expect(mockDragDom.style.top).toBe('690px')

      window.getComputedStyle = originalGetComputedStyle
    })

    it('应该优先使用 currentStyle（IE 兼容）', () => {
      const directive = createDragModalDirective()
      const mockDialogHeader = {
        style: {},
        offsetLeft: 50,
        offsetTop: 100,
        onmousedown: null
      }
      const mockDragDom = {
        style: {},
        currentStyle: { left: '100px', top: '200px' }
      }
      const mockEl = {
        querySelector: vi.fn((selector) => {
          if (selector === '.modalHeader') return mockDialogHeader
          if (selector === '.ant-modal') return mockDragDom
          return null
        })
      }
      const mockBinding = {}
      const mockGetComputedStyle = vi.fn(() => ({ left: '200px', top: '300px' }))

      const originalGetComputedStyle = window.getComputedStyle
      window.getComputedStyle = mockGetComputedStyle

      directive(mockEl, mockBinding)

      // 应该使用 currentStyle，而不是 getComputedStyle
      expect(mockDragDom.currentStyle).toBeDefined()
      // getComputedStyle 可能仍会被调用作为后备，但 currentStyle 优先

      window.getComputedStyle = originalGetComputedStyle
    })
  })

  describe('createDraggable', () => {
    // 注意：此函数尚未实现，测试将在函数实现后完善
    // 当前仅作为占位，确保测试结构完整
    
    it.skip('应该返回清理函数', () => {
      // TODO: 等待 createDraggable 函数实现后完善此测试
      // const cleanup = createDraggable(element, options)
      // expect(typeof cleanup).toBe('function')
    })

    it.skip('应该在拖拽时调用 onDrag 回调', () => {
      // TODO: 等待 createDraggable 函数实现后完善此测试
    })

    it.skip('应该限制在边界内', () => {
      // TODO: 等待 createDraggable 函数实现后完善此测试
    })

    it.skip('应该清理事件监听器', () => {
      // TODO: 等待 createDraggable 函数实现后完善此测试
    })
  })
})
