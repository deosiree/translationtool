/**
 * CommonEntry 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import CommonEntry from '@/views/entry/commonEntry.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/http/api/entryManage', () => ({
  searchEntryInfo: vi.fn(() => Promise.resolve({
    data: {
      list: [],
      totalNum: 0
    }
  })),
  getPublicEntry: vi.fn(() => Promise.resolve({
    data: {
      list: [],
      totalNum: 0
    }
  })),
  updatePublicEntry: vi.fn(() => Promise.resolve({})),
  deletePublicEntry: vi.fn(() => Promise.resolve({}))
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
    warning: vi.fn()
  }
}))

describe('CommonEntry - user 属性重构测试', () => {
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
      
      wrapper = mount(CommonEntry, {
        props: {
          currentCommon: {},
          boxHeight: 600
        },
        global: {
          mocks: storeMock,
          stubs: {
            'SearchBox': true,
            'DataBox': true,
            'a-table': true,
            'a-button': true
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
      
      wrapper = mount(CommonEntry, {
        props: {
          currentCommon: {},
          boxHeight: 600
        },
        global: {
          mocks: storeMock,
          stubs: {
            'SearchBox': true,
            'DataBox': true,
            'a-table': true,
            'a-button': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toEqual(testUser)
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(CommonEntry, {
        props: {
          currentCommon: {},
          boxHeight: 600
        },
        global: {
          mocks: storeMock,
          stubs: {
            'SearchBox': true,
            'DataBox': true,
            'a-table': true,
            'a-button': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })
})
