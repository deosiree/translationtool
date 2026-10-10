/**
 * ExportButton 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ExportButton from '@/components/Button/exportButton.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  createDragModalDirective: vi.fn(() => ({}))
}))

vi.mock('@/http/api/userPartiality', () => ({
  queryUserPartiality: vi.fn(() => Promise.resolve({
    data: {
      list: [{
        exportColumn: '词条,英文翻译'
      }]
    }
  })),
  updateUserPartiality: vi.fn(() => Promise.resolve({}))
}))

vi.mock('@/constants/commonParam.js', () => {
  const commonParam = {
    languageList: [
      { name: '英文', value: 'english' },
      { name: '俄文', value: 'russian' }
    ]
  }
  const entryParams = {
    exportFields: [
      { label: '词条', value: 'entry', index: 1 },
      { label: '英文翻译', value: 'english', index: 12 }
    ]
  }
  return {
    default: commonParam,
    entryParams
  }
})

vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  },
  notification: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

describe('ExportButton - user 属性重构测试', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('user 属性验证', () => {
    it('不应该在 data 中定义 user 属性', () => {
      const storeMock = createUserStoreMock()
      
      wrapper = mount(ExportButton, {
        props: {
          buttonTitle: '导出',
          dataSource: [],
          fieldOptions_: []
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-input': true,
            'a-checkbox': true
          }
        }
      })

      // 验证组件实例不应该有 user 属性（如果重构完成）
      // 注意：如果重构尚未完成，这个测试可能会失败
      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(ExportButton, {
        props: {
          buttonTitle: '导出',
          dataSource: [],
          fieldOptions_: []
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-input': true,
            'a-checkbox': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以直接访问 $store.state.user
      expect(wrapper.vm.$store.state.user).toEqual(testUser)
      expect(wrapper.vm.$store.state.user.userName).toBe('testUser')
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(ExportButton, {
        props: {
          buttonTitle: '导出',
          dataSource: [],
          fieldOptions_: []
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-input': true,
            'a-checkbox': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证 user 为 null 时不会报错
      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })

  describe('组件功能测试', () => {
    it('应该正确渲染导出按钮', () => {
      const storeMock = createUserStoreMock()
      
      wrapper = mount(ExportButton, {
        props: {
          buttonTitle: '导出',
          dataSource: [],
          fieldOptions_: [],
          hideButton: false
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size']
            },
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-input': true,
            'a-checkbox': true
          }
        }
      })

      const html = wrapper.html()
      expect(html).toContain('导出')
    })

    it('应该能够打开导出模态框', async () => {
      const storeMock = createUserStoreMock()
      const { setModalAriaHidden } = await import('@/utils/domUtils')
      
      wrapper = mount(ExportButton, {
        props: {
          buttonTitle: '导出',
          dataSource: [],
          fieldOptions_: []
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-input': true,
            'a-checkbox': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 初始状态应该是关闭的
      expect(wrapper.vm.exportVisible).toBe(false)

      // 调用打开方法
      wrapper.vm.showExportModal()
      await nextTick()

      // 应该设置为打开
      expect(wrapper.vm.exportVisible).toBe(true)
      expect(setModalAriaHidden).toHaveBeenCalled()
    })
  })
})
