/**
 * 词条管理-已选词条-预翻译 v2 壳组件测试
 * 架构：PreTranslateForm（配置弹窗）+ usePreTranslateEdit（编排器）+ 壳 index.vue（编辑态渲染/底部模式切换）
 * 覆盖：
 * 1. API 契约：preTranslateEntry（POST /entryInfo/preTranslate，params: translateType+priority）
 * 2. 壳行为：预翻译按钮打开配置弹窗；配置提交进入编辑模式（底部切换 取消/保存）
 * 3. 编辑模式：取消恢复快照（弹窗不关）；保存走编排器
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

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
    warn: vi.fn(),
    info: vi.fn()
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
import CreateVersionModal from '@/views/entry/createVersionModal/index.vue'
import PreTranslateForm from '@/views/entry/createVersionModal/PreTranslateForm.vue'

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

// 壳测试：Modal stub 渲染插槽（默认 stub 不渲染插槽会导致表单 ref 挂不上）
const ModalStub = {
  name: 'ModalStub',
  template: '<div><slot name="leftBottomBtn" /><slot /></div>'
}
const SpinStub = { template: '<div><slot /></div>' }

function mountShell(dataSource) {
  return mount(CreateVersionModal, {
    props: {
      visible: true,
      currentProduct: { key: 'p1', type: 'module', parentId: 'root' },
      classifyLimit: {},
      dataSource,
      selectedRowKeys: [],
      selectedRows: [],
      selectedProducts: { products: new Map(), totalNum: 0 }
    },
    global: {
      mocks: createUserStoreMock(),
      stubs: {
        'CustomModal': ModalStub,
        'a-form': true,
        'a-form-item': true,
        'a-select': true,
        'a-spin': SpinStub,
        'a-table': true,
        'a-config-provider': SpinStub,
        'TableCellTextArea': true,
        'CellOverflowTooltip': true,
        'EntryStateBadge': true,
        'TransStateBadge': true,
        'ExportButton': true,
        'CreateVersionForm': true,
        'ExamineTaskForm': true,
        'WriteBackForm': true,
        'PreTranslateForm': true
      }
    }
  })
}

describe('CreateVersionModal - 预翻译 v2（编辑态模式）', () => {
  let wrapper

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  it('预翻译配置弹窗由壳挂载且 visible 受控', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }])
    expect(wrapper.vm.preTranslateFormVisible).toBe(false)

    // 壳模板挂载了 PreTranslateForm，visible 受壳控制
    const form = wrapper.findComponent(PreTranslateForm)
    expect(form.exists()).toBe(true)
    expect(form.props('visible')).toBe(false)

    wrapper.vm.preTranslateFormVisible = true
    await nextTick()
    expect(form.props('visible')).toBe(true)
  })

  it('配置提交后进入编辑模式：接口按语种并行、译文写入列、快照保存', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }])

    request.mockResolvedValue({
      code: 200,
      data: { list: [{ id: 'e1', english: 'circuit breaker' }] }
    })

    // 直接调用壳的提交流程（配置弹窗的 submit 事件）
    await wrapper.vm.onPreTranslateSubmit({
      language: ['英文'],
      priority: 'shuyuku',
      verifyMethods: []
    })
    await nextTick()

    // 接口契约
    expect(request).toHaveBeenCalledTimes(1)
    const call = request.mock.calls[0][0]
    expect(call.url).toBe('/entryInfo/preTranslate')
    expect(call.params).toEqual({ translateType: '英文', priority: 'shuyuku' })
    // 译文写入语种列（浏览态）
    expect(wrapper.vm.dataSource[0].english).toBe('circuit breaker')
    // 编辑模式激活：底部按钮语义切换 + 快照保存
    expect(wrapper.vm.preTranslateActive).toBe(true)
    expect(wrapper.vm.preTranslateSnapshot[0].english).toBe('')
    // 配置弹窗关闭
    expect(wrapper.vm.preTranslateFormVisible).toBe(false)
  })

  it('编辑模式取消：dataSource 恢复快照原值、退出编辑模式（模态框不关）', async () => {
    const entries = [{ id: 'e1', entry: '断路器', english: 'origin value' }]
    wrapper = mountShell(entries)

    request.mockResolvedValue({
      code: 200,
      data: { list: [{ id: 'e1', english: 'translated' }] }
    })
    await wrapper.vm.onPreTranslateSubmit({
      language: ['英文'],
      priority: 'shuyuku',
      verifyMethods: []
    })
    expect(wrapper.vm.dataSource[0].english).toBe('translated')

    // 取消
    wrapper.vm.preTranslateCancel()
    await nextTick()

    expect(wrapper.vm.preTranslateActive).toBe(false)
    expect(wrapper.vm.preTranslateSnapshot).toBeNull()
    // 壳 emit update:dataSource（父组件用快照替换）
    const emitted = wrapper.emitted('update:dataSource')
    expect(emitted).toBeTruthy()
    expect(emitted.at(-1)[0][0].english).toBe('origin value')
    // 模态框未关闭（无 createClose 事件）
    expect(wrapper.emitted('createClose')).toBeFalsy()
  })

  it('编辑模式保存：全部通过且全部移除 → 关壳+清空+refresh', async () => {
    const entries = [{ id: 'e1', entry: '断路器', english: '' }]
    wrapper = mountShell(entries)

    request.mockResolvedValue({
      code: 200,
      data: { list: [{ id: 'e1', english: 'final value' }] }
    })
    await wrapper.vm.onPreTranslateSubmit({
      language: ['英文'],
      priority: 'shuyuku',
      verifyMethods: []
    })

    // 保存接口
    request.mockResolvedValue({ code: 200 })
    await wrapper.vm.preTranslateSave()
    await nextTick()

    // 逐条 updateEntryInfo 被调用（url = /entryInfo/updateEntryInfo）
    const updateCall = request.mock.calls.map(c => c[0]).find(c => c.url === '/entryInfo/updateEntryInfo')
    expect(updateCall).toBeTruthy()
    expect(updateCall.data.english).toBe('final value')

    // 全部移除 → 关壳三件套 + refresh
    expect(wrapper.emitted('update:dataSource').at(-1)[0]).toEqual([])
    expect(wrapper.emitted('createClose')).toBeTruthy()
    expect(wrapper.emitted('cancelCreate')).toBeTruthy()
    expect(wrapper.emitted('refresh')).toBeTruthy()
    expect(wrapper.vm.preTranslateActive).toBe(false)
  })

  it('空已选词条提交：直接提示不调接口', async () => {
    wrapper = mountShell([])
    await wrapper.vm.onPreTranslateSubmit({
      language: ['英文'],
      priority: 'shuyuku',
      verifyMethods: []
    })
    expect(request).not.toHaveBeenCalled()
    expect(wrapper.vm.preTranslateActive).toBe(false)
  })
})
