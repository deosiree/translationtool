/**
 * ConfigImportButton 组件测试
 * 验证组件不再接收 department prop，而是从 $store.state.user?.department 获取
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ConfigImportButton from '@/components/Button/codeBranch/configImportButton.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn()
}))

vi.mock('@/constants/commonParam.js', () => ({
  createBranchParams: {
    otherConfig: {
      '测试部门': {
        'develop': { key: 'value' }
      }
    }
  }
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

describe('ConfigImportButton - department prop 移除测试', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('department prop 验证', () => {
    it('不应该接收 department prop', () => {
      const storeMock = createUserStoreMock()
      
      wrapper = mount(ConfigImportButton, {
        props: {
          buttonTitle: '配置新增',
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
            'PlusOutlined': true
          }
        }
      })

      // 验证 props 中不包含 department
      expect(wrapper.props('department')).toBeUndefined()
      // 验证组件实例不应该有 department prop
      expect('department' in wrapper.props()).toBe(false)
    })

    it('应该从 $store.state.user?.department 获取部门信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(ConfigImportButton, {
        props: {
          buttonTitle: '配置新增',
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
            'PlusOutlined': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 触发 importConfig 方法，这会调用 getDepartmentOptions
      await wrapper.vm.importConfig()
      await nextTick()

      // 验证组件从 $store.state.user?.department 获取部门信息
      expect(wrapper.vm.$store.state.user.department).toBe('测试部门')
      expect(wrapper.vm.importModal.department).toBe('测试部门')
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(ConfigImportButton, {
        props: {
          buttonTitle: '配置新增',
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
            'PlusOutlined': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证 user 为 null 时不会报错
      expect(wrapper.vm.$store.state.user).toBeNull()
      
      // 触发 importConfig 方法
      await wrapper.vm.importConfig()
      await nextTick()

      // 验证可选链正确处理 null，department 应该为 undefined
      expect(wrapper.vm.importModal.department).toBeUndefined()
    })
  })
})
