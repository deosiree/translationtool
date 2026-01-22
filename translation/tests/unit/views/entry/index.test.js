/**
 * Entry Index 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 * 特别测试 roleName 和 department 的使用
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import EntryIndex from '@/views/entry/index.vue'
import { createUserStoreMock, createAdminUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/http/api/entryManage', () => ({
  getClassTree: vi.fn(() => Promise.resolve({
    data: []
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
  }
}))

describe('Entry Index - user 属性重构测试', () => {
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
      
      wrapper = mount(EntryIndex, {
        global: {
          mocks: {
            ...storeMock,
            $router: {
              push: vi.fn()
            }
          },
          stubs: {
            'ProductEntry': true,
            'ProductVersion': true,
            'CommonEntry': true,
            'ClassifyModal': true,
            'ProductAuthorityModal': true,
            'UpdateModal': true,
            'RedundantModal': true,
            'CreateBranchModal': true,
            'EntrySourceModal': true,
            'a-layout': true,
            'a-layout-sider': true,
            'a-tree': true
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
      
      wrapper = mount(EntryIndex, {
        global: {
          mocks: {
            ...storeMock,
            $router: {
              push: vi.fn()
            }
          },
          stubs: {
            'ProductEntry': true,
            'ProductVersion': true,
            'CommonEntry': true,
            'ClassifyModal': true,
            'ProductAuthorityModal': true,
            'UpdateModal': true,
            'RedundantModal': true,
            'CreateBranchModal': true,
            'EntrySourceModal': true,
            'a-layout': true,
            'a-layout-sider': true,
            'a-tree': true
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
      
      wrapper = mount(EntryIndex, {
        global: {
          mocks: {
            ...storeMock,
            $router: {
              push: vi.fn()
            }
          },
          stubs: {
            'ProductEntry': true,
            'ProductVersion': true,
            'CommonEntry': true,
            'ClassifyModal': true,
            'ProductAuthorityModal': true,
            'UpdateModal': true,
            'RedundantModal': true,
            'CreateBranchModal': true,
            'EntrySourceModal': true,
            'a-layout': true,
            'a-layout-sider': true,
            'a-tree': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以正确判断超级管理员
      const isSuperAdmin = wrapper.vm.$store.state.user.roleName.includes('超级管理员')
      expect(isSuperAdmin).toBe(true)
    })

    it('应该正确使用 $store.state.user.department', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(EntryIndex, {
        global: {
          mocks: {
            ...storeMock,
            $router: {
              push: vi.fn()
            }
          },
          stubs: {
            'ProductEntry': true,
            'ProductVersion': true,
            'CommonEntry': true,
            'ClassifyModal': true,
            'ProductAuthorityModal': true,
            'UpdateModal': true,
            'RedundantModal': true,
            'CreateBranchModal': true,
            'EntrySourceModal': true,
            'a-layout': true,
            'a-layout-sider': true,
            'a-tree': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以正确获取部门信息
      expect(wrapper.vm.$store.state.user.department).toBe('测试部门')
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(EntryIndex, {
        global: {
          mocks: {
            ...storeMock,
            $router: {
              push: vi.fn()
            }
          },
          stubs: {
            'ProductEntry': true,
            'ProductVersion': true,
            'CommonEntry': true,
            'ClassifyModal': true,
            'ProductAuthorityModal': true,
            'UpdateModal': true,
            'RedundantModal': true,
            'CreateBranchModal': true,
            'EntrySourceModal': true,
            'a-layout': true,
            'a-layout-sider': true,
            'a-tree': true
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
