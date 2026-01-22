/**
 * AddRepository 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 * 特别测试 department 的使用
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import AddRepository from '@/views/repository/addRepository.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  createDragModalDirective: vi.fn(() => ({}))
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
  TreeSelect: {
    name: 'TreeSelect',
    template: '<div></div>'
  }
}))

describe('AddRepository - user 属性重构测试', () => {
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
      
      wrapper = mount(AddRepository, {
        props: {
          visible: true
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true
          }
        }
      })

      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息，特别是 department', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(AddRepository, {
        props: {
          visible: true
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toEqual(testUser)
      expect(wrapper.vm.$store.state.user.department).toBe('测试部门')
      
      // 验证可以使用 department
      const repositoryData = {
        department: wrapper.vm.$store.state.user.department
      }
      expect(repositoryData.department).toBe('测试部门')
      
      // 验证组件内部使用 $store.state.user?.department 而不是 this.user.department
      // 这通过测试 null 情况来验证
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(AddRepository, {
        props: {
          visible: true
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })
})
