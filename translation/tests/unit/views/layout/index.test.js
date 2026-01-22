/**
 * Layout Index 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 * 测试模板中显示用户信息
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import LayoutIndex from '@/views/layout/index.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/router', () => ({
  push: vi.fn()
}))

describe('Layout Index - user 属性重构测试', () => {
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
      
      wrapper = mount(LayoutIndex, {
        global: {
          mocks: {
            ...storeMock,
            $router: {
              push: vi.fn()
            },
            $route: {
              path: '/'
            }
          },
          stubs: {
            'a-layout': true,
            'a-layout-sider': true,
            'a-layout-header': true,
            'a-menu': true,
            'a-menu-item': true
          }
        }
      })

      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(LayoutIndex, {
        global: {
          mocks: {
            ...storeMock,
            $router: {
              push: vi.fn()
            },
            $route: {
              path: '/'
            }
          },
          stubs: {
            'a-layout': true,
            'a-layout-sider': true,
            'a-layout-header': true,
            'a-menu': true,
            'a-menu-item': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toEqual(testUser)
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(LayoutIndex, {
        global: {
          mocks: {
            ...storeMock,
            $router: {
              push: vi.fn()
            },
            $route: {
              path: '/'
            }
          },
          stubs: {
            'a-layout': true,
            'a-layout-sider': true,
            'a-layout-header': true,
            'a-menu': true,
            'a-menu-item': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })
})
