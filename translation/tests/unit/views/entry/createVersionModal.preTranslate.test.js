/**
 * 词条管理-已选词条-预翻译 功能测试
 * 覆盖两个层面：
 * 1. API 契约：preTranslateEntry 应 POST /entryInfo/preTranslate，params 携带 translateType+priority，data 为词条数组
 * 2. 组件行为：operateOk 预翻译分支应按选中语种并行调用接口，allSettled 后分语种汇总成败
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

vi.mock('@/http/request', () => ({
  default: vi.fn(),
  requestMultipart: vi.fn(),
}))

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
    warn: vi.fn()
  },
  notification: {
    success: vi.fn()
  },
  Modal: {
    confirm: vi.fn()
  }
}))

import request from '@/http/request'
import { preTranslateEntry } from '@/http/api/entryManage'
import { createUserStoreMock } from '../../testUtils/userStoreMock'

describe('entryManage API - preTranslateEntry（/entryInfo/preTranslate）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应 POST /entryInfo/preTranslate 且 params 携带 translateType 与 priority', async () => {
    request.mockResolvedValue({ code: 200, data: { list: [] } })
    const params = { translateType: '英文', priority: 'shuyuku' }
    const data = [{ id: '1', entry: '断路器' }]

    await preTranslateEntry(params, data)

    expect(request).toHaveBeenCalledTimes(1)
    expect(request).toHaveBeenCalledWith({
      url: '/entryInfo/preTranslate',
      method: 'POST',
      params: { translateType: '英文', priority: 'shuyuku' },
      data: [{ id: '1', entry: '断路器' }]
    })
  })
})

describe('CreateVersionModal - 预翻译分支', () => {
  let wrapper

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  it('多语种并行：遍历选中语种逐个调用接口并汇总成功语种', async () => {
    const { mount } = await import('@vue/test-utils')
    const { default: CreateVersionModal } = await import('@/views/entry/createVersionModal/index.vue')

    // 模拟法文语种后端失败；rejected Promise 预挂 catch 避免 unhandled rejection 告警
    const frenchFailure = Promise.reject(new Error('预翻译失败'))
    frenchFailure.catch(() => { })
    request.mockImplementation(({ params }) =>
      params.translateType === '法文'
        ? frenchFailure
        : Promise.resolve({ code: 200, data: { list: [] } })
    )

    // 表单 stub：暴露 validate，供 operateOk 调用
    const FormStub = {
      name: 'FormStub',
      template: '<form><slot /></form>',
      methods: {
        validate: () => Promise.resolve()
      },
      exposed: {
        validate: () => Promise.resolve()
      }
    }
    // CustomModal stub：渲染插槽（默认 stub 不渲染插槽，会导致表单 ref 挂不上）
    const ModalStub = {
      name: 'ModalStub',
      template: '<div><slot name="leftBottomBtn" /><slot /></div>'
    }

    wrapper = mount(CreateVersionModal, {
      props: {
        visible: true,
        currentProduct: { key: 'p1', type: 'module', parentId: 'root' },
        dataSource: [
          { id: '1', entry: '断路器', english: '' },
          { id: '2', entry: '隔离开关', english: '' }
        ]
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          'CustomModal': ModalStub,
          'a-form': FormStub,
          'a-form-item': true,
          'a-select': true,
          'a-spin': { template: '<div><slot /></div>' },
          'a-table': true
        }
      }
    })

    // 打开预翻译二级弹窗
    wrapper.vm.preTranslateFun()
    expect(wrapper.vm.title).toBe('预翻译')
    expect(wrapper.vm.operateVisible).toBe(true)
    // 等待二级弹窗渲染，让 $refs.preTranslateForm 挂载
    await wrapper.vm.$nextTick()

    // 模拟用户选择两个语种（英文成功、法文失败）
    wrapper.vm.preTran.language = ['英文', '法文']
    wrapper.vm.preTran.priority = 'shuyuku'

    await wrapper.vm.operateOk()
    await Promise.resolve()

    // 接口按语种并行各调用一次，translateType 逐语种传入
    const calls = request.mock.calls.map((call) => call[0])
    expect(calls).toHaveLength(2)
    expect(calls.map((c) => c.params.translateType).sort()).toEqual(['法文', '英文'])
    expect(calls.every((c) => c.params.priority === 'shuyuku')).toBe(true)
    expect(calls.every((c) => c.url === '/entryInfo/preTranslate')).toBe(true)

    // data 均为已选词条数组
    expect(calls.every((c) => Array.isArray(c.data) && c.data.length === 2)).toBe(true)
  })

  it('语种为空时不发起请求', async () => {
    const { mount } = await import('@vue/test-utils')
    const { default: CreateVersionModal } = await import('@/views/entry/createVersionModal/index.vue')

    // 表单 stub：validate 拒绝（语种必填校验失败）
    // 注意：rejected Promise 必须预先挂 catch，避免暴露给 Vue 运行时造成 unhandled rejection
    const rejectedValidate = Promise.reject(new Error('校验失败'))
    rejectedValidate.catch(() => { })
    const FormStub = {
      name: 'FormStub',
      template: '<form><slot /></form>',
      methods: {
        validate: () => rejectedValidate
      },
      exposed: {
        validate: () => rejectedValidate
      }
    }
    const ModalStub = {
      name: 'ModalStub',
      template: '<div><slot name="leftBottomBtn" /><slot /></div>'
    }

    wrapper = mount(CreateVersionModal, {
      props: {
        visible: true,
        currentProduct: { key: 'p1', type: 'module', parentId: 'root' },
        dataSource: [{ id: '1', entry: '断路器' }]
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          'CustomModal': ModalStub,
          'a-form': FormStub,
          'a-form-item': true,
          'a-select': true,
          'a-spin': { template: '<div><slot /></div>' },
          'a-table': true
        }
      }
    })

    wrapper.vm.preTranslateFun()
    wrapper.vm.preTran.language = []
    // 等待二级弹窗渲染，让 $refs.preTranslateForm 挂载
    await wrapper.vm.$nextTick()

    await wrapper.vm.operateOk()

    expect(request).not.toHaveBeenCalled()
    expect(wrapper.vm.operateVisible).toBe(true)
  })
})
