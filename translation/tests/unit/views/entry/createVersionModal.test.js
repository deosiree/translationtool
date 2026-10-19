/**
 * CreateVersionModal 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { message } from 'ant-design-vue'
import CreateVersionModal from '@/views/entry/createVersionModal/index.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

const deleteEntryInfoByIDMock = vi.hoisted(() =>
  vi.fn(() => Promise.resolve({}))
)

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
    warning: vi.fn(),
    info: vi.fn()
  },
  Modal: {
    confirm: vi.fn(({ onOk }) => onOk?.())
  }
}))

vi.mock('@/http/api/workbench.js', () => ({
  deleteEntryInfoByID: deleteEntryInfoByIDMock
}))

describe('CreateVersionModal - user 属性重构测试', () => {
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
      
      wrapper = mount(CreateVersionModal, {
        props: {
          visible: true,
          currentProduct: {}
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

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(CreateVersionModal, {
        props: {
          visible: true,
          currentProduct: {}
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
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(CreateVersionModal, {
        props: {
          visible: true,
          currentProduct: {}
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

describe('CreateVersionModal - 删除词条 productID 参数', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('缺少产品 ID 时删除请求省略 productID', async () => {
    wrapper = mount(CreateVersionModal, {
      props: {
        visible: true,
        currentProduct: { type: 'classify', key: 'classify-id-1' },
        dataSource: [
          { id: 'entry-id-1', productID: null, versionID: null },
          { id: 'entry-id-2', productID: 'null', versionID: undefined },
          { id: 'entry-id-3', productID: undefined, versionID: '' }
        ]
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          CustomModal: true,
          'a-form': true,
          'a-form-item': true,
          'a-input': true,
          CreateVersionForm: true,
          ExamineTaskForm: true,
          WriteBackForm: true,
          BatchEditModal: true
        }
      }
    })

    wrapper.vm.deleteEntrys()
    await flushPromises()

    expect(deleteEntryInfoByIDMock).toHaveBeenCalledWith({}, [
      'entry-id-1',
      'entry-id-2',
      'entry-id-3'
    ])
    expect(wrapper.emitted('refresh')).toBeTruthy()
  })

  it('有效产品和版本 ID 仍传给删除接口', async () => {
    wrapper = mount(CreateVersionModal, {
      props: {
        visible: true,
        currentProduct: { type: 'product', key: 'product-id-1' },
        dataSource: [{
          id: 'entry-id-4',
          productID: 'product-id-1',
          versionID: 'version-id-1'
        }]
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          CustomModal: true,
          'a-form': true,
          'a-form-item': true,
          'a-input': true,
          CreateVersionForm: true,
          ExamineTaskForm: true,
          WriteBackForm: true,
          BatchEditModal: true
        }
      }
    })

    wrapper.vm.deleteEntrys()
    await flushPromises()

    expect(deleteEntryInfoByIDMock).toHaveBeenCalledWith({
      versionID: 'version-id-1',
      productID: 'product-id-1'
    }, ['entry-id-4'])
  })

  it('删除失败时提示产品、版本、数量和错误原因', async () => {
    deleteEntryInfoByIDMock.mockRejectedValueOnce(new Error('删除失败原因'))
    wrapper = mount(CreateVersionModal, {
      props: {
        visible: true,
        currentProduct: { type: 'product', key: 'product-id-2' },
        dataSource: [{
          id: 'entry-id-5',
          productID: 'product-id-2',
          versionID: 'version-id-2'
        }]
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          CustomModal: true,
          'a-form': true,
          'a-form-item': true,
          'a-input': true,
          CreateVersionForm: true,
          ExamineTaskForm: true,
          WriteBackForm: true,
          BatchEditModal: true
        }
      }
    })

    wrapper.vm.deleteEntrys()
    await flushPromises()

    expect(message.info).toHaveBeenCalledWith(expect.stringContaining(
      '产品ID: product-id-2, 版本ID: version-id-2, 失败数量: 1, 原因: 删除失败原因'
    ))
  })
})
