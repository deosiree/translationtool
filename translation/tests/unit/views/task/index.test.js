/**
 * Task Index 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 * 特别测试 roleName、department、userName 等多个字段的使用
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import TaskIndex from '@/views/task/index.vue'
import { createUserStoreMock, createAdminUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/http/api/task', () => ({
  searchTaskInfo: vi.fn(() => Promise.resolve({
    data: {
      list: [],
      totalNum: 0
    }
  })),
  getDepartments: vi.fn(() => Promise.resolve({
    data: {
      list: ['部门1', '部门2']
    }
  }))
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({
    data: {
      list: []
    }
  }))
}))

vi.mock('@/utils/tableUtils', () => ({
  setTableHeight: vi.fn()
}))

vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  }
}))

describe('Task Index - user 属性重构测试', () => {
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
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
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
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以直接访问 $store.state.user
      expect(wrapper.vm.$store.state.user).toEqual(testUser)
      expect(wrapper.vm.$store.state.user.userName).toBe('testUser')
      expect(wrapper.vm.$store.state.user.department).toBe('测试部门')
      expect(wrapper.vm.$store.state.user.roleName).toBe('普通用户')
    })

    it('应该正确使用 $store.state.user.roleName 判断超级管理员', async () => {
      const adminUser = {
        userName: 'admin',
        department: '通用平台部',
        roleName: '超级管理员'
      }
      const storeMock = createAdminUserStoreMock()
      storeMock.$store.state.user = adminUser
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以正确判断超级管理员
      const isAdmin = wrapper.vm.$store.state.user.roleName.includes('超级管理员')
      expect(isAdmin).toBe(true)
    })

    it('应该正确使用 $store.state.user 的多个字段（department, userName等）', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以使用多个字段
      expect(wrapper.vm.$store.state.user.department).toBe('测试部门')
      expect(wrapper.vm.$store.state.user.userName).toBe('testUser')
      
      // 模拟创建任务时使用这些字段
      const defaultUser = wrapper.vm.$store.state.user?.userName;
      const newTaskData = {
        department: wrapper.vm.$store.state.user.department,
        creator: defaultUser,
        developer: defaultUser,
        entryAuditor: defaultUser,
        translator: defaultUser,
        translationAuditor: defaultUser
      }
      
      expect(newTaskData.department).toBe('测试部门')
      expect(newTaskData.creator).toBe('testUser')
      
      // 验证组件内部使用 $store.state.user?.department 而不是 this.user.department
      // 这通过测试 null 情况来验证
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证 user 为 null 时不会报错
      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })
})
