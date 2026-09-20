/**
 * 词条管理-已选词条 / 批量编辑拆分测试
 * 架构：已选（CreateVersionModal）只做选择；BatchEditModal 负责编辑/预翻译/保存
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
  createDragModalDirective: vi.fn(() => ({})),
}))

vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn(),
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
  },
  notification: {
    success: vi.fn(),
    error: vi.fn(),
  },
  Modal: {
    confirm: vi.fn(),
  },
}))

import request from '@/http/request'
import { preTranslateEntry } from '@/http/api/entryManage'
import { createUserStoreMock } from '../../testUtils/userStoreMock'
import { resetLoading } from '@/composables/useLoading'
import CreateVersionModal from '@/views/entry/createVersionModal/index.vue'
import BatchEditModal from '@/views/entry/createVersionModal/BatchEditModal.vue'
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
      data: [{ id: '1', entry: '断路器' }],
    })
  })
})

const ModalStub = {
  name: 'ModalStub',
  props: {
    okLoading: { type: Boolean, default: false },
    modalTitle: { type: String, default: '' },
    modalVisible: { type: Boolean, default: false },
  },
  template:
    '<div class="modal-stub" :data-title="modalTitle"><slot name="leftBottomBtn" /><slot /></div>',
}

const SpinStub = { template: '<div><slot /></div>' }
const TooltipStub = { template: '<div><slot /></div>' }
const RulesDropdownStub = {
  name: 'RulesDropdownStub',
  props: {
    options: { type: Array, default: () => [] },
  },
  emits: ['update:options'],
  template: '<div class="rules-dropdown-stub"></div>',
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
  `,
}

const OverflowTableStub = {
  name: 'OverflowTableStub',
  props: {
    columns: { type: Array, default: () => [] },
    dataSource: { type: Array, default: () => [] },
  },
  setup(props, { slots }) {
    return () =>
      h('div', { class: 'table-stub table-cell-overflow' }, [
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
                text: record[column.dataIndex],
              })
            )
          )
        ),
      ])
  },
}

function mountShell(
  dataSource,
  tableStub = true,
  { realCellOverflowTooltip = false, realBadges = false, stubBatchEdit = true } = {}
) {
  const cellOverflowStub = realCellOverflowTooltip ? {} : { CellOverflowTooltip: true }
  const badgeStubs = realBadges ? {} : { EntryStateBadge: true, TransStateBadge: true }
  return mount(CreateVersionModal, {
    props: {
      visible: true,
      currentProduct: { key: 'p1', type: 'module', parentId: 'root' },
      classifyLimit: {},
      dataSource,
      selectedRowKeys: dataSource.map((r) => r.id),
      selectedRows: [...dataSource],
      selectedProducts: { products: new Map(), totalNum: 0 },
    },
    global: {
      mocks: createUserStoreMock(),
      stubs: {
        CustomModal: ModalStub,
        'a-button': {
          template: '<button @click="$emit(\'click\', $event)"><slot /></button>',
        },
        'a-form': true,
        'a-form-item': true,
        'a-select': true,
        'a-spin': SpinStub,
        'a-tooltip': TooltipStub,
        'a-table': tableStub,
        'a-config-provider': SpinStub,
        TableCellTextArea: true,
        RulesDropdown: RulesDropdownStub,
        ExportButton: true,
        CreateVersionForm: true,
        ExamineTaskForm: true,
        WriteBackForm: true,
        PreTranslateForm: true,
        ...(stubBatchEdit ? { BatchEditModal: true } : {}),
        ...cellOverflowStub,
        ...badgeStubs,
      },
    },
  })
}

const ColumnFilterStub = {
  name: 'ColumnFilter',
  props: {
    modelValue: { type: Array, default: () => [] },
    columns: { type: Array, default: () => [] },
    colPrefName: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'change'],
  template: '<div class="column-filter-stub" :data-pref="colPrefName"></div>',
}

function mountBatchEdit(entries) {
  return mount(BatchEditModal, {
    props: {
      visible: true,
      entries,
      classifyLimit: {},
    },
    global: {
      mocks: createUserStoreMock(),
      stubs: {
        CustomModal: ModalStub,
        'a-button': {
          template: '<button @click="$emit(\'click\', $event)"><slot /></button>',
        },
        'a-input-search': {
          props: ['value', 'placeholder'],
          emits: ['update:value'],
          template:
            '<input class="keyword-search-stub" :value="value" :placeholder="placeholder" @input="$emit(\'update:value\', $event.target.value)" />',
        },
        'a-form': true,
        'a-form-item': true,
        'a-select': true,
        'a-spin': SpinStub,
        'a-tooltip': TooltipStub,
        'a-table': OperationTableStub,
        'a-config-provider': SpinStub,
        TableCellTextArea: true,
        RulesDropdown: RulesDropdownStub,
        ColumnFilter: ColumnFilterStub,
        PreTranslateForm: {
          name: 'PreTranslateForm',
          props: {
            visible: { type: Boolean, default: false },
            loading: { type: Boolean, default: false },
          },
          template: '<div class="pre-translate-form-stub" />',
        },
      },
    },
  })
}

describe('CreateVersionModal - 已选词条（无行编辑）', () => {
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

  it('底部有「编辑」、无「预翻译」；点击编辑打开批量编辑', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }])

    const buttons = wrapper.findAll('button').map((b) => b.text())
    expect(buttons).toContain('编辑')
    expect(buttons).not.toContain('预翻译')

    expect(wrapper.vm.batchEditOpen).toBe(false)
    const editBtn = wrapper.findAll('button').find((b) => b.text() === '编辑')
    await editBtn.trigger('click')
    expect(wrapper.vm.batchEditOpen).toBe(true)
  })

  it('空已选点击编辑：提示且不打开批量编辑', async () => {
    wrapper = mountShell([])
    wrapper.vm.openBatchEdit()
    expect(wrapper.vm.batchEditOpen).toBe(false)
  })

  it('操作列仅取消选择，无编辑图标；无双击编辑', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: '' }], OperationTableStub)

    expect(wrapper.find('.row-delete-icon').exists()).toBe(true)
    expect(wrapper.find('.row-edit-icon').exists()).toBe(false)
    expect(wrapper.vm.customRow).toBeUndefined()
    expect(wrapper.vm.startEditRow).toBeUndefined()
  })

  it('批量编辑保存：合并成功行到已选，不剔除 keys', async () => {
    const entries = [
      { id: 'e1', entry: '断路器', english: 'a' },
      { id: 'e2', entry: '隔离开关', english: 'b' },
    ]
    wrapper = mountShell(entries)

    wrapper.vm.onBatchEditSaved({
      savedRecords: [{ id: 'e1', entry: '断路器', english: 'breaker' }],
      remainingIds: ['e2'],
      allDone: false,
    })
    await nextTick()

    const ds = wrapper.emitted('update:dataSource').at(-1)[0]
    expect(ds.map((r) => r.id)).toEqual(['e1', 'e2'])
    expect(ds.find((r) => r.id === 'e1').english).toBe('breaker')
    expect(wrapper.emitted('update:selectedRowKeys')).toBeUndefined()
    expect(wrapper.vm.batchEditOpen).toBe(false)
  })

  it('批量编辑全部保存成功：关闭批量编辑', async () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器', english: 'a' }])
    wrapper.vm.batchEditOpen = true
    wrapper.vm.onBatchEditSaved({
      savedRecords: [{ id: 'e1', entry: '断路器', english: 'breaker' }],
      remainingIds: [],
      allDone: true,
    })
    expect(wrapper.vm.batchEditOpen).toBe(false)
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
      style: { width: '200px', minWidth: '200px', maxWidth: '200px' },
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
      tooltips.some(
        (tooltip) => tooltip.props('content') === '超长词条内容用于验证省略和悬浮提示'
      )
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
        index: 2,
      },
    ]
    await nextTick()

    expect(wrapper.findComponent(EntryStateBadge).exists()).toBe(true)
    expect(wrapper.findComponent(TransStateBadge).exists()).toBe(true)
    expect(wrapper.findAllComponents(CellOverflowTooltip).length).toBeGreaterThan(0)
  })

  it('批量选择顶部仅展示「已选词条」文案，无校验规则控件', () => {
    wrapper = mountShell([{ id: 'e1', entry: '断路器' }])
    expect(wrapper.text()).toContain('已选词条')
    expect(wrapper.findComponent(RulesDropdownStub).exists()).toBe(false)
  })
})

describe('BatchEditModal - 批量编辑', () => {
  let wrapper

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    resetLoading()
  })

  it('标题为「批量编辑」，无预翻译结果子标题', () => {
    wrapper = mountBatchEdit([{ id: 'e1', entry: '断路器', english: '' }])
    expect(wrapper.findComponent(ModalStub).props('modalTitle')).toBe('批量编辑')
    expect(wrapper.text()).not.toContain('预翻译结果')
  })

  it('底部有预翻译；点击打开配置弹窗', async () => {
    wrapper = mountBatchEdit([{ id: 'e1', entry: '断路器', english: '' }])
    const preBtn = wrapper.findAll('button').find((b) => b.text() === '预翻译')
    expect(preBtn).toBeDefined()
    await preBtn.trigger('click')
    expect(wrapper.vm.preTranslateFormVisible).toBe(true)
    const form = wrapper.findComponent({ name: 'PreTranslateForm' })
    expect(form.exists()).toBe(true)
    expect(form.props('visible')).toBe(true)
  })

  it('操作列浏览态有编辑与删除；删除只移出工作表', async () => {
    const entries = [
      { id: 'e1', entry: '断路器', english: 'a' },
      { id: 'e2', entry: '隔离开关', english: 'b' },
    ]
    wrapper = mountBatchEdit(entries)
    expect(wrapper.find('.row-edit-icon').exists()).toBe(true)
    expect(wrapper.find('.row-delete-icon').exists()).toBe(true)

    await wrapper.vm.removeFromEdit(entries[0])
    expect(wrapper.vm.dataSource.map((r) => r.id)).toEqual(['e2'])
    expect(wrapper.emitted('update:dataSource')).toBeUndefined()
  })

  it('startEditRow：生成独立编辑副本', async () => {
    const entries = [{ id: 'e1', entry: '断路器', english: 'translated' }]
    wrapper = mountBatchEdit(entries)
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.startEditRow(record)

    expect(wrapper.vm.editableData.e1).toBeDefined()
    expect(wrapper.vm.editableData.e1).not.toBe(record)
    expect(wrapper.vm.editableData.e1.english).toBe('translated')
  })

  it('预翻译配置提交：写回同表（调用 preTranslate API）', async () => {
    request.mockResolvedValue({
      code: 200,
      data: { list: [{ id: 'e1', english: 'circuit breaker' }] },
    })
    wrapper = mountBatchEdit([{ id: 'e1', entry: '断路器', english: '' }])
    await wrapper.vm.onPreTranslateSubmit({
      language: ['英文'],
      priority: 'shuyuku',
    })
    await nextTick()

    expect(wrapper.vm.preTranslateFormVisible).toBe(false)
    expect(request).toHaveBeenCalled()
    const call = request.mock.calls.map((c) => c[0]).find((c) => c.url === '/entryInfo/preTranslate')
    expect(call).toBeTruthy()
  })

  it('保存成功：notes 为批量编辑；成功行移出工作表并 emit saved（已选由父合并）', async () => {
    wrapper = mountBatchEdit([
      { id: 'e1', entry: '断路器', english: 'origin' },
      { id: 'e2', entry: '隔离开关', english: 'origin2' },
    ])
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
    await wrapper.vm.onSave()
    await nextTick()

    const updateCall = request.mock.calls
      .map((c) => c[0])
      .find((c) => c.url === '/entryInfo/updateEntryInfoList')
    expect(updateCall).toBeTruthy()
    expect(updateCall.params).toEqual({ notes: '批量编辑' })
    expect(wrapper.vm.dataSource.map((r) => r.id)).toEqual(['e2'])
    expect(wrapper.emitted('saved')).toBeTruthy()
    expect(wrapper.emitted('saved')[0][0].savedRecords[0].id).toBe('e1')
    expect(wrapper.emitted('refresh')).toBeTruthy()
  })

  it('customRow 双击进入编辑态；交互控件不触发', async () => {
    wrapper = mountBatchEdit([{ id: 'e1', entry: '断路器', english: 'translated' }])
    const startEditSpy = vi.spyOn(wrapper.vm, 'startEditRow')
    const record = wrapper.vm.dataSource[0]

    await wrapper.vm.customRow(record).onDblclick({
      target: { closest: () => ({ className: 'ant-input' }) },
    })
    expect(startEditSpy).not.toHaveBeenCalled()

    await wrapper.vm.customRow(record).onDblclick({
      target: { closest: () => null },
    })
    expect(startEditSpy).toHaveBeenCalledWith(record)
    expect(wrapper.vm.editableData.e1).toBeDefined()
  })

  it('工具栏有 ColumnFilter（独立 colPref）、校验规则与 handleResizeColumn', () => {
    wrapper = mountBatchEdit([{ id: 'e1', entry: '断路器', english: '' }])
    const colFilter = wrapper.findComponent({ name: 'ColumnFilter' })
    expect(colFilter.exists()).toBe(true)
    expect(colFilter.props('colPrefName')).toBe('colPref-batchEditModal')
    expect(wrapper.findComponent(RulesDropdownStub).exists()).toBe(true)
    expect(typeof wrapper.vm.handleResizeColumn).toBe('function')
    expect(typeof wrapper.vm.syncColumnsFromPref).toBe('function')
    expect(Array.isArray(wrapper.vm.columnSettingsList)).toBe(true)
  })

  it('单元格 change 即校验（不必等失焦或 √）', async () => {
    wrapper = mountBatchEdit([{ id: 'e1', entry: '断路器', english: '' }])
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.startEditRow(record)
    wrapper.vm.rules.e1.english = [
      {
        validator: async () => {
          throw new Error('翻错了')
        },
      },
    ]

    await wrapper.vm.onCellInput('bad', record, { dataIndex: 'english' })
    await nextTick()

    expect(wrapper.vm.cellErrors.e1?.english).toBe('翻错了')
  })

  it('关键字全字段过滤展示行；清空恢复；删除仍改完整 dataSource', async () => {
    wrapper = mountBatchEdit([
      { id: 'e1', entry: '断路器', english: 'breaker', remark: '高压' },
      { id: 'e2', entry: '隔离开关', english: 'disconnector', remark: '中压' },
    ])

    expect(wrapper.vm.filteredDataSource.map((r) => r.id)).toEqual(['e1', 'e2'])

    wrapper.vm.keyword = 'breaker'
    await nextTick()
    expect(wrapper.vm.filteredDataSource.map((r) => r.id)).toEqual(['e1'])

    wrapper.vm.keyword = '中压'
    await nextTick()
    expect(wrapper.vm.filteredDataSource.map((r) => r.id)).toEqual(['e2'])

    await wrapper.vm.startEditRow(wrapper.vm.dataSource[0])
    wrapper.vm.editableData.e1.english = 'unique-draft-xyz'
    wrapper.vm.keyword = 'unique-draft'
    await nextTick()
    expect(wrapper.vm.filteredDataSource.map((r) => r.id)).toEqual(['e1'])

    wrapper.vm.keyword = ''
    await nextTick()
    expect(wrapper.vm.filteredDataSource.map((r) => r.id)).toEqual(['e1', 'e2'])

    await wrapper.vm.removeFromEdit(wrapper.vm.dataSource[0])
    expect(wrapper.vm.dataSource.map((r) => r.id)).toEqual(['e2'])
    expect(wrapper.vm.filteredDataSource.map((r) => r.id)).toEqual(['e2'])
  })
})
