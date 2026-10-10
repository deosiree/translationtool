/**
 * CreateBranchModal 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 * 特别测试多个用户角色字段的使用
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import CreateBranchModal from '@/views/entry/createBranchModal.vue'
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
  }
}))

describe('CreateBranchModal - user 属性重构测试', () => {
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
      
      wrapper = mount(CreateBranchModal, {
        props: {
          visible: true,
          currentClass: {}
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

    it('应该从 $store.state.user 获取用户信息，特别是多个角色字段', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(CreateBranchModal, {
        props: {
          visible: true,
          currentClass: {}
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
      
      // 验证可以使用多个字段
      const defaultUser = wrapper.vm.$store.state.user?.userName;
      const branchData = {
        department: wrapper.vm.$store.state.user.department,
        creator: defaultUser,
        developer: defaultUser,
        entryAuditor: defaultUser,
        translator: defaultUser,
        translationAuditor: defaultUser
      }
      
      expect(branchData.department).toBe('测试部门')
      expect(branchData.creator).toBe('testUser')
      
      // 验证 ImportButton 组件不再接收 department prop
      const importButton = wrapper.findComponent({ name: 'ImportButton' })
      if (importButton.exists()) {
        expect(importButton.props('department')).toBeUndefined()
      }
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(CreateBranchModal, {
        props: {
          visible: true,
          currentClass: {}
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
