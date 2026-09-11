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
import { h, nextTick } from 'vue'

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
import { resetLoading } from '@/composables/useLoading'
import CreateVersionModal from '@/views/entry/createVersionModal/index.vue'
import PreTranslateForm from '@/views/entry/createVersionModal/PreTranslateForm.vue'
import CellOverflowTooltip from '@/components/table/CellOverflowTooltip.vue'
import EntryStateBadge from '@/components/stateBadge/entryStateBadge.vue'
import TransStateBadge from '@/components/stateBadge/transStateBadge.vue'

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
  props: {
    okLoading: {
      type: Boolean,
      default: false
    }
  },
  template: '<div><slot name="leftBottomBtn" /><slot /></div>'
}
const SpinStub = { template: '<div><slot /></div>' }
const TooltipStub = { template: '<div><slot /></div>' }
const RulesDropdownStub = {
  name: 'RulesDropdownStub',
  props: {
    options: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:options'],
  template: '<div class="rules-dropdown-stub"></div>'
}
const OperationTableStub = {
  name: 'OperationTableStub',
  props: ['columns', 'dataSource'],
  template: `
    <div class="table-stub">
      <div v-for="record in dataSource" :key="record.id" class="table-row-stub">
        <slot name="bodyCell" :column="{ dataIndex: 'operation' }" :record="record" :text="''" />
      </div>
    </div>
  `
}
const OverflowTableStub = {
  name: 'OverflowTableStub',
  props: {
    columns: {
      type: Array,
      default: () => []
    },
    dataSource: {
      type: Array,
      default: () => []
    }
  },
  setup(props, { slots }) {
    return () => h('div', { class: 'table-stub' }, [
      h(
        'div',
        { class: 'header-row' },
        props.columns.map((column) =>
          slots.headerCell?.({ title: column.title, column })
        )
      ),
      ...props.dataSource.map((record) =>
        h(
          'div',
          { class: 'body-row', key: record.id },
          props.columns.map((column) =>
            slots.bodyCell?.({
              column,
              record,
              text: record[column.dataIndex]
            })
          )
        )
      )
    ])
  }
}

function mountShell(
  dataSource,
  tableStub = true,
  { realCellOverflowTooltip = false, realBadges = false } = {}
) {
  const cellOverflowStub = realCellOverflowTooltip ? {} : { CellOverflowTooltip: true }
  const badgeStubs = realBadges ? {} : { EntryStateBadge: true, TransStateBadge: true }
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
        'a-button': {
          template: '<button @click="$emit(\'click\', $event)"><slot /></button>'
        },
        'a-form': true,
        'a-form-item': true,
        'a-select': true,
        'a-spin': SpinStub,
        'a-tooltip': TooltipStub,
        'a-table': tableStub,
        'a-config-provider': SpinStub,
        'TableCellTextArea': true,
        'RulesDropdown': RulesDropdownStub,
        'ExportButton': true,
        'CreateVersionForm': true,
        'ExamineTaskForm': true,
        'WriteBackForm': true,
        'PreTranslateForm': true,
        ...cellOverflowStub,
        ...badgeStubs
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
    resetLoading()
  })

  it('壳暴露全局 loading，供 okLoading / 表格遮罩绑定', () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }])
    expect(wrapper.vm.loading).toBeDefined()
    expect(wrapper.vm.loading).toBe(false)
    expect(wrapper.findComponent(ModalStub).props('okLoading')).toBe(false)
  })

  it('点击预翻译按钮应打开配置弹窗', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }])
    expect(wrapper.vm.preTranslateFormVisible).toBe(false)

    const preTranslateButton = wrapper.findAll('button').find((button) => button.text() === '预翻译')
    expect(preTranslateButton).toBeDefined()
    await preTranslateButton.trigger('click')

    // 配置弹窗由真实按钮入口打开，并保持 visible 受壳控制
    const form = wrapper.findComponent(PreTranslateForm)
    expect(form.exists()).toBe(true)
    expect(wrapper.vm.preTranslateFormVisible).toBe(true)
    expect(form.props('visible')).toBe(true)
  })

  it('配置提交后进入编辑模式：接口按语种并行、译文写入列、快照保存', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }])
    wrapper.vm.rulesOptions = wrapper.vm.rulesOptions.map((item) => ({
      ...item,
      checked: false
    }))

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

    // 保存接口（批量 per-row）
    request.mockResolvedValue({
      code: 200,
      data: {
        list: [{ id: 'e1', success: true, message: 'OK' }],
        totalNum: 1,
      },
      message: '更新完成',
    })
    await wrapper.vm.preTranslateSave()
    await nextTick()

    // 批量 updateEntryInfoList 被调用
    const updateCall = request.mock.calls.map(c => c[0]).find(c => c.url === '/entryInfo/updateEntryInfoList')
    expect(updateCall).toBeTruthy()
    expect(Array.isArray(updateCall.data)).toBe(true)
    expect(updateCall.data[0].english).toBe('final value')
    expect(updateCall.params).toEqual({ notes: '预翻译' })

    // 全部移除 → 关壳三件套 + refresh
    expect(wrapper.emitted('update:dataSource').at(-1)[0]).toEqual([])
    expect(wrapper.emitted('createClose')).toBeTruthy()
    expect(wrapper.emitted('cancelCreate')).toBeTruthy()
    expect(wrapper.emitted('refresh')).toBeTruthy()
    expect(wrapper.vm.preTranslateActive).toBe(false)
  })

  it('编辑态保存中连点：写接口只触发一次', async () => {
    const entries = [{ id: 'e1', entry: '断路器', english: '' }]
    wrapper = mountShell(entries)
    wrapper.vm.rulesOptions = wrapper.vm.rulesOptions.map((item) => ({
      ...item,
      checked: false
    }))

    request.mockResolvedValue({
      code: 200,
      data: { list: [{ id: 'e1', english: 'final value' }] }
    })
    await wrapper.vm.onPreTranslateSubmit({
      language: ['英文'],
      priority: 'shuyuku',
      verifyMethods: []
    })

    let resolveUpdate
    const updateGate = new Promise((resolve) => {
      resolveUpdate = resolve
    })
    request.mockImplementation((config) => {
      if (config.url === '/entryInfo/updateEntryInfoList') {
        return updateGate.then(() => ({
          code: 200,
          data: {
            list: [{ id: 'e1', success: true, message: 'OK' }],
            totalNum: 1,
          },
        }))
      }
      return Promise.resolve({ code: 200 })
    })

    const first = wrapper.vm.preTranslateSave()
    await nextTick()
    expect(wrapper.vm.loading).toBe(true)

    const second = wrapper.vm.preTranslateSave()
    const viaHandleOK = wrapper.vm.handleOK()

    resolveUpdate()
    await Promise.all([first, second, viaHandleOK])
    await nextTick()

    const updateCalls = request.mock.calls
      .map((c) => c[0])
      .filter((c) => c.url === '/entryInfo/updateEntryInfoList')
    expect(updateCalls).toHaveLength(1)
    expect(wrapper.vm.loading).toBe(false)
  })

  it('预翻译部分保存成功：仅移除成功行的 dataSource 与已选状态', async () => {
    const entries = [
      { id: 'e1', entry: '断路器', english: '' },
      { id: 'e2', entry: '隔离开关', english: '' }
    ]
    wrapper = mountShell(entries)
    await wrapper.setProps({
      selectedRows: entries,
      selectedRowKeys: ['e1', 'e2']
    })
    wrapper.vm.rulesOptions = wrapper.vm.rulesOptions.map((item) => ({
      ...item,
      checked: false
    }))

    request.mockImplementation((config) => {
      if (config.url === '/entryInfo/preTranslate') {
        return Promise.resolve({
          code: 200,
          data: {
            list: [
              { id: 'e1', english: 'breaker' },
              { id: 'e2', english: 'disconnector' }
            ]
          }
        })
      }
      if (config.url === '/entryInfo/updateEntryInfoList') {
        return Promise.resolve({
          code: 203,
          data: {
            list: [
              { id: 'e1', success: true, message: 'OK' },
              { id: 'e2', success: false, message: '落库失败' },
            ],
            totalNum: 2,
          },
        })
      }
      return Promise.resolve({ code: 200 })
    })

    await wrapper.vm.onPreTranslateSubmit({
      language: ['英文'],
      priority: 'shuyuku'
    })
    await wrapper.vm.preTranslateSave()
    await nextTick()

    expect(wrapper.emitted('update:dataSource').at(-1)[0].map((item) => item.id)).toEqual(['e2'])
    expect(wrapper.emitted('update:selectedRows').at(-1)[0].map((item) => item.id)).toEqual(['e2'])
    expect(wrapper.emitted('update:selectedRowKeys').at(-1)[0]).toEqual(['e2'])
    expect(wrapper.emitted('createClose')).toBeFalsy()
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

  it('startEditRow：浏览态行生成独立编辑副本并配置语种校验规则', async () => {
    const entries = [{ id: 'e1', entry: '断路器', english: 'origin' }]
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

    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.startEditRow(record)

    expect(wrapper.vm.editableData.e1).toBeDefined()
    expect(wrapper.vm.editableData.e1).not.toBe(record)
    expect(wrapper.vm.editableData.e1.english).toBe('translated')
    expect(wrapper.vm.rules.e1.english).toBeTruthy()
  })

  it('customRow 双击与编辑按钮共用同一进入编辑态方法', async () => {
    const entries = [{ id: 'e1', entry: '断路器', english: '' }]
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

    const startEditSpy = vi.spyOn(wrapper.vm, 'startEditRow')
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.customRow(record).onDblclick({ target: { closest: () => null } })

    expect(startEditSpy).toHaveBeenCalledWith(record)
    expect(wrapper.vm.editableData.e1).toBeDefined()
  })

  it('双击交互控件不触发行编辑，重复双击不覆盖未确认输入', async () => {
    const entries = [{ id: 'e1', entry: '断路器', english: '' }]
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

    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.customRow(record).onDblclick({
      target: { closest: () => ({ className: 'ant-input' }) }
    })
    expect(wrapper.vm.editableData.e1).toBeUndefined()

    await wrapper.vm.startEditRow(record)
    wrapper.vm.editableData.e1.english = '未确认输入'
    await wrapper.vm.customRow(record).onDblclick({ target: { closest: () => null } })

    expect(wrapper.vm.editableData.e1.english).toBe('未确认输入')
  })

  it('浏览态操作列显示编辑图标，进入编辑态后隐藏', async () => {
    const entries = [{ id: 'e1', entry: '断路器', english: '' }]
    wrapper = mountShell(entries, OperationTableStub)
    request.mockResolvedValue({
      code: 200,
      data: { list: [{ id: 'e1', english: 'translated' }] }
    })
    await wrapper.vm.onPreTranslateSubmit({
      language: ['英文'],
      priority: 'shuyuku',
      verifyMethods: []
    })

    const editIcon = wrapper.find('.row-edit-icon')
    expect(editIcon.exists()).toBe(true)
    await editIcon.trigger('click')
    await nextTick()

    expect(wrapper.vm.editableData.e1).toBeDefined()
    expect(wrapper.find('.row-edit-icon').exists()).toBe(false)
  })

  it('已选词条表格业务列按 200px 锁定，序号与操作列保持既有宽度', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: 'circuit breaker' }])
    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })
    await nextTick()

    const entryCol = wrapper.vm.columns.find((col) => col.dataIndex === 'entry')
    const indexCol = wrapper.vm.columns.find((col) => col.dataIndex === 'index')
    const operationCol = wrapper.vm.columns.find((col) => col.dataIndex === 'operation')

    expect(entryCol.width).toBe(200)
    expect(entryCol.ellipsis).toEqual({ showTitle: false })
    expect(entryCol.customCell()).toEqual({
      style: { width: '200px', minWidth: '200px', maxWidth: '200px' }
    })
    expect(indexCol.width).toBe(50)
    expect(operationCol.width).toBe(200)
  })

  it('已选词条表格复用 table-cell-overflow 并为表头和普通正文渲染 Tooltip', async () => {
    wrapper = mountShell(
      [{ id: 'e1', entry: '超长词条内容用于验证省略和悬浮提示', english: 'long translation' }],
      OverflowTableStub,
      { realCellOverflowTooltip: true }
    )
    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })
    await nextTick()

    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
    const tooltips = wrapper.findAllComponents(CellOverflowTooltip)
    expect(tooltips.some((tooltip) => tooltip.props('content') === '词条')).toBe(true)
    expect(
      tooltips.some((tooltip) => tooltip.props('content') === '超长词条内容用于验证省略和悬浮提示')
    ).toBe(true)
  })

  it('状态列仍渲染 Badge，并在外层保留溢出 Tooltip', async () => {
    wrapper = mountShell(
      [{ id: 'e1', entry: '断路器', entryState: 0, englishTranslateState: '1' }],
      OverflowTableStub,
      { realCellOverflowTooltip: true, realBadges: true }
    )
    await wrapper.setProps({ visible: false })
    await wrapper.setProps({ visible: true })
    await nextTick()

    wrapper.vm.columns = [
      { title: '词条状态', dataIndex: 'entryState', colValue: 'entryState', index: 1 },
      {
        title: '英文翻译状态',
        dataIndex: 'englishTranslateState',
        colValue: 'englishTranslateState',
        index: 2
      }
    ]
    await nextTick()

    expect(wrapper.findComponent(EntryStateBadge).exists()).toBe(true)
    expect(wrapper.findComponent(TransStateBadge).exists()).toBe(true)
    expect(wrapper.findAllComponents(CellOverflowTooltip).length).toBeGreaterThan(0)
  })

  it('批量选择顶部始终渲染工作台同款校验规则控件', () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器' }])

    const rulesDropdown = wrapper.findComponent(RulesDropdownStub)
    expect(rulesDropdown.exists()).toBe(true)
    expect(rulesDropdown.props('options').length).toBeGreaterThan(0)
  })

  it('未预翻译时点击编辑图标可进入编辑态，底部切换为取消/保存', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }], OperationTableStub)

    const editIcon = wrapper.find('.row-edit-icon')
    expect(editIcon.exists()).toBe(true)
    expect(wrapper.find('.row-delete-icon').exists()).toBe(true)

    await editIcon.trigger('click')
    await nextTick()

    expect(wrapper.vm.editableData.e1).toBeDefined()
    expect(wrapper.vm.manualEditActive).toBe(true)
    expect(wrapper.vm.isEditMode).toBe(true)
    expect(wrapper.find('.row-confirm-icon').exists()).toBe(true)
    expect(wrapper.find('.row-edit-icon').exists()).toBe(false)
  })

  it('未预翻译时双击行可进入编辑态', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }])
    const record = wrapper.vm.dataSource[0]

    await wrapper.vm.customRow(record).onDblclick({ target: { closest: () => null } })

    expect(wrapper.vm.editableData.e1).toBeDefined()
    expect(wrapper.vm.manualEditActive).toBe(true)
  })

  it('普通编辑保存立即落库但保留已选词条', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: 'origin' }])
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.startEditRow(record)
    wrapper.vm.editableData.e1.english = 'manual value'

    request.mockResolvedValue({
      code: 200,
      data: {
        list: [{ id: 'e1', success: true, message: 'OK' }],
        totalNum: 1,
      },
      message: '更新完成',
    })
    await wrapper.vm.handleOK()

    const updateCall = request.mock.calls.map((c) => c[0]).find((c) => c.url === '/entryInfo/updateEntryInfoList')
    expect(updateCall).toBeTruthy()
    expect(Array.isArray(updateCall.data)).toBe(true)
    expect(updateCall.data[0].english).toBe('manual value')
    expect(updateCall.params).toEqual({ notes: '编辑词条' })
    expect(wrapper.vm.dataSource.map((item) => item.id)).toEqual(['e1'])
    expect(wrapper.vm.manualEditActive).toBe(false)
    expect(wrapper.vm.createVersionFormVisible).toBe(false)
  })

  it('普通编辑取消恢复快照并退出编辑态', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: 'origin' }])
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.startEditRow(record)
    wrapper.vm.editableData.e1.english = 'changed'
    await wrapper.vm.rowConfirm(record)

    expect(record.english).toBe('changed')
    expect(wrapper.vm.manualEditActive).toBe(true)

    wrapper.vm.cancelManualEdits()
    await nextTick()

    const emitted = wrapper.emitted('update:dataSource')
    expect(emitted.at(-1)[0][0].english).toBe('origin')
    expect(wrapper.vm.manualEditActive).toBe(false)
    expect(wrapper.vm.editableData).toEqual({})
  })

  it('切换校验规则会清空当前编辑行红字', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }])
    wrapper.vm.cellErrors = { e1: { english: '旧错误' } }

    const nextRules = wrapper.vm.rulesOptions.map((item) => ({
      ...item,
      checked: false
    }))
    wrapper.findComponent(RulesDropdownStub).vm.$emit('update:options', nextRules)
    await nextTick()

    expect(wrapper.vm.cellErrors).toEqual({})
  })
})
