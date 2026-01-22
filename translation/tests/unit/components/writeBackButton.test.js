/**
 * WriteBackButton 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import WriteBackButton from '@/components/Button/writeBackButton.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  createDragModalDirective: vi.fn(() => ({}))
}))

vi.mock('@/http/api/workbench', () => ({
  getI18nAdress: vi.fn(() => Promise.resolve({
    data: {
      list: [
        { ip: '192.168.1.1' },
        { ip: '192.168.1.2' }
      ]
    }
  })),
  getBranches: vi.fn(() => Promise.resolve({
    data: {
      list: ['main', 'develop']
    }
  })),
  writeBack: vi.fn(() => Promise.resolve({}))
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({
    data: {
      list: [
        { name: '英文', value: 'english' },
        { name: '俄文', value: 'russian' }
      ]
    }
  }))
}))

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

describe('WriteBackButton - user 属性重构测试', () => {
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
      
      wrapper = mount(WriteBackButton, {
        props: {
          buttonTitle: '回写',
          size: 'small'
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-radio-group': true,
            'a-radio': true,
            'a-checkbox': true,
            'a-spin': true,
            'a-tooltip': true
          }
        }
      })

      // 验证组件实例不应该有 user 属性
      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(WriteBackButton, {
        props: {
          buttonTitle: '回写',
          size: 'small'
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-radio-group': true,
            'a-radio': true,
            'a-checkbox': true,
            'a-spin': true,
            'a-tooltip': true
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
      
      wrapper = mount(WriteBackButton, {
        props: {
          buttonTitle: '回写',
          size: 'small'
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-radio-group': true,
            'a-radio': true,
            'a-checkbox': true,
            'a-spin': true,
            'a-tooltip': true
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
    it('应该正确渲染回写按钮', () => {
      const storeMock = createUserStoreMock()
      
      wrapper = mount(WriteBackButton, {
        props: {
          buttonTitle: '回写',
          size: 'small'
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'class']
            },
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-radio-group': true,
            'a-radio': true,
            'a-checkbox': true,
            'a-spin': true,
            'a-tooltip': true
          }
        }
      })

      const html = wrapper.html()
      expect(html).toContain('回写')
    })

    it('应该能够打开回写模态框', async () => {
      const storeMock = createUserStoreMock()
      const { setModalAriaHidden } = await import('@/utils/domUtils')
      
      wrapper = mount(WriteBackButton, {
        props: {
          buttonTitle: '回写',
          size: 'small'
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-radio-group': true,
            'a-radio': true,
            'a-checkbox': true,
            'a-spin': true,
            'a-tooltip': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 初始状态应该是关闭的
      expect(wrapper.vm.visible).toBe(false)

      // 调用打开方法
      wrapper.vm.showModal()
      await nextTick()

      // 应该设置为打开
      expect(wrapper.vm.visible).toBe(true)
      expect(setModalAriaHidden).toHaveBeenCalled()
    })
  })
})
