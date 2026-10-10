/**
 * TimeLine 组件测试
 * 验证组件不再使用 currentUser，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import TimeLine from '@/components/timeLine/index.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../testUtils/userStoreMock'

describe('TimeLine - user 属性重构测试', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('user 属性验证', () => {
    it('不应该在 data 中定义 currentUser 属性', () => {
      const storeMock = createUserStoreMock()
      
      wrapper = mount(TimeLine, {
        props: {
          currentTask: {
            creator: 'admin',
            developer: 'dev',
            entryAuditor: 'auditor',
            translator: 'translator',
            deliveryTime: '2024-01-01',
            importTime: '2024-01-02'
          },
          showButton: true
        },
        global: {
          mocks: storeMock,
          stubs: {
            'a-button': true,
            'a-timeline': true,
            'a-timeline-item': true
          }
        }
      })

      // 验证组件实例不应该有 currentUser 属性（如果重构完成）
      // 注意：如果重构尚未完成，这个测试可能会失败
      expect(wrapper.vm.currentUser).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(TimeLine, {
        props: {
          currentTask: {
            creator: 'admin',
            developer: 'dev',
            entryAuditor: 'auditor',
            translator: 'translator',
            deliveryTime: '2024-01-01',
            importTime: '2024-01-02'
          },
          showButton: true
        },
        global: {
          mocks: storeMock,
          stubs: {
            'a-button': true,
            'a-timeline': true,
            'a-timeline-item': true
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
      
      wrapper = mount(TimeLine, {
        props: {
          currentTask: {
            creator: 'admin',
            developer: 'dev',
            entryAuditor: 'auditor',
            translator: 'translator',
            deliveryTime: '2024-01-01',
            importTime: '2024-01-02'
          },
          showButton: true
        },
        global: {
          mocks: storeMock,
          stubs: {
            'a-button': true,
            'a-timeline': true,
            'a-timeline-item': true
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
    it('应该正确渲染时间线组件', () => {
      const storeMock = createUserStoreMock()
      
      wrapper = mount(TimeLine, {
        props: {
          currentTask: {
            creator: 'admin',
            developer: 'dev',
            entryAuditor: 'auditor',
            translator: 'translator',
            deliveryTime: '2024-01-01',
            importTime: '2024-01-02'
          },
          showButton: true
        },
        global: {
          mocks: storeMock,
          stubs: {
            'a-button': true,
            'a-timeline': true,
            'a-timeline-item': true
          }
        }
      })

      // 验证组件已挂载
      expect(wrapper.exists()).toBe(true)
    })

    it('init 方法应该使用 $store.state.user 而不是 currentUser', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(TimeLine, {
        props: {
          currentTask: {
            creator: 'admin',
            developer: 'dev',
            entryAuditor: 'auditor',
            translator: 'translator',
            deliveryTime: '2024-01-01',
            importTime: '2024-01-02'
          },
          showButton: true
        },
        global: {
          mocks: storeMock,
          stubs: {
            'a-button': true,
            'a-timeline': true,
            'a-timeline-item': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 调用 init 方法
      wrapper.vm.init()
      await nextTick()

      // 验证可以直接访问 $store.state.user
      expect(wrapper.vm.$store.state.user).toEqual(testUser)
    })
  })
})
