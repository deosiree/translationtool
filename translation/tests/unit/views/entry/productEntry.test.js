/**
 * ProductEntry 组件测试
 */

import { describe, it, expect, afterEach, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ProductEntry from '@/views/entry/productEntry.vue'
import ColumnFilter from '@/components/ColumnFilter/ColumnFilter.vue'
import { entryAllCols, entryPresets, entryParams } from '@/constants/commonParam.js'
import { resolvePresetCols } from '@/components/ColumnFilter/colPreset.js'
import { setTableHeight as setTableHeightMock } from '@/utils/tableUtils'

const applyTableMock = vi.hoisted(() => vi.fn())
const changeColumnMock = vi.hoisted(() => vi.fn())
const getEntryByClassfyMock = vi.hoisted(() =>
  vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } }))
)

import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'
import { createDeferred } from '../../testUtils/deferred'
import { message } from 'ant-design-vue'

vi.mock('@/http/api/entryManage', () => ({
  searchEntryInfo: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  getEntryByClassfy: getEntryByClassfyMock,
  getEntrySourcesByClassify: vi.fn(() => Promise.resolve({ data: [] })),
  getWriteFileNamesByClassify: vi.fn(() => Promise.resolve({ data: [] })),
  deleteEntryInfo: vi.fn(() => Promise.resolve({})),
  updatePublicEntry: vi.fn(),
  addSingleEntry: vi.fn(),
  updateEntryInfoList: vi.fn(),
  getClassfy: vi.fn(),
  getClassTree: vi.fn(),
  entryImportExcle: vi.fn(),
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))

vi.mock('@/http/api/product', () => ({
  getProductVersion: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))

vi.mock('@/http/api/productVersion', () => ({
  getVersionByName: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))

vi.mock('@/http/api/secondClassify', () => ({
  getSecondClassify: vi.fn(),
}))

vi.mock('@/http/api/userPartiality', () => ({
  queryUserPartiality: vi.fn(),
  updateUserPartiality: vi.fn(),
}))

vi.mock('@/http/api/workbench', () => ({
  getI18nAdress: vi.fn(),
}))

vi.mock('@/http/api/check', () => ({
  getCheckNotUseEntry: vi.fn(),
  checkNotUseEntry: vi.fn(),
}))

vi.mock('@/utils/tableUtils', () => ({
  setTableHeight: vi.fn(),
  handleSearch: vi.fn(),
  handleReset: vi.fn(),
  clearFilters: vi.fn(),
  handleTableChange: vi.fn(),
  handleResizeColumn: vi.fn(),
}))

vi.mock('@/components/ColumnFilter', () => ({
  applyTable: applyTableMock,
  changeColumn: changeColumnMock,
  mergeColumnSelection: (selected, list) => selected,
}))

vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn(),
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
  Modal: {
    confirm: vi.fn(({ onOk }) => {
      if (typeof onOk === 'function') return onOk()
    }),
  },
  notification: {},
}))

import { deleteEntryInfo, getEntryByClassfy, updateEntryInfoList } from '@/http/api/entryManage'
import { getVersionByName } from '@/http/api/productVersion'
import { checkNotUseEntry, getCheckNotUseEntry } from '@/http/api/check'
import { EDIT_NOTES } from '@/constants/editNotes.js'
import { Modal } from 'ant-design-vue'

const testProduct = {
  type: 'module',
  parentId: 'product-id-1',
  key: 'module-id-1',
  title: '测试产品',
}

function mountProductEntry(storeMock = createUserStoreMock(), options = {}) {
  return mount(ProductEntry, {
    props: {
      currentProduct: testProduct,
      boxHeight: 600,
      productEdit: false,
    },
    global: {
      mocks: storeMock,
      stubs: {
        SearchBox: {
          template: '<div><slot name="form" /></div>',
        },
        'a-form': {
          template: '<form><slot /></form>',
        },
        'a-row': {
          template: '<div class="a-row"><slot /></div>',
        },
        'a-form-item': {
          template: '<div class="a-form-item"><slot /></div>',
        },
        DataBox: true,
        OperationArea: true,
        'a-table': true,
        'a-button': true,
        AccurSearchButton: true,
        EntryStateSelect: true,
        TransStateSelect: true,
        CreateVersionModal: true,
        SecondClassify: true,
        Dictionary: true,
        CustomModal: true,
        BackFillModal: true,
        BackFillModal_v2: true,
        BackFillModal_v2_5: true,
        BackFillModal_v3: true,
        GitCommitButton: true,
        ColumnFilter: {
          name: 'ColumnFilter',
          props: [
            'title',
            'modelValue',
            'columns',
            'colPrefName',
            'overlayStyle',
            'buttonSize',
            'persistMode',
            'buttonText',
            'normalWidth',
            'needFilter',
          ],
          template: '<div class="column-filter-stub" />',
        },
        ...options.stubs,
      },
    },
  })
}

describe('ProductEntry - 翻译状态详情投影', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  it('仅为 1/2/3 状态写入公共翻译标签，并兼容数值状态码', () => {
    wrapper = mountProductEntry()
    const record = {
      id: 'entry-1',
      englishTranslateState: 2,
      russianTranslateState: '3',
      spanishTranslateState: 0,
      frenchTranslateState: 'unknown',
    }

    wrapper.vm.entryDetails(record)

    expect(wrapper.vm.currentEntry.englishChineseState).toBe('审核不通过')
    expect(wrapper.vm.currentEntry.russianChineseState).toBe('已审核')
    expect(wrapper.vm.currentEntry.spanishChineseState).toBeUndefined()
    expect(wrapper.vm.currentEntry.frenchChineseState).toBeUndefined()
  })
})

describe('ProductEntry - 表格空态', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  it('空数据时保留完整表格高度并启用共享空态插槽', async () => {
    wrapper = mountProductEntry(undefined, {
      stubs: {
        DataBox: {
          template: '<div><slot name="data" /></div>',
        },
        'a-config-provider': {
          template: '<div><slot /></div>',
        },
        TableViewport: {
          name: 'TableViewport',
          props: ['dataSource', 'scroll', 'emptyOverlay'],
          template: '<section class="table-viewport-stub" :data-scroll-y="scroll?.y" :data-empty-overlay="emptyOverlay ? \'enabled\' : \'disabled\'"><slot name="empty" /></section>',
        },
        'a-empty': {
          props: ['image'],
          template: '<div class="entry-empty-stub" />',
        },
      },
    })
    wrapper.vm.dataSource = []
    wrapper.vm.tableHeight = { x: 'max-content', y: 360 }
    await nextTick()

    const table = wrapper.find('.table-viewport-stub')
    expect(table.attributes('data-scroll-y')).toBe('360')
    expect(table.attributes('data-empty-overlay')).toBe('enabled')
    expect(table.find('.entry-empty-stub').exists()).toBe(true)
  })
})

describe('ProductEntry - user 属性重构测试', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('user 属性验证', () => {
    it('不应该在 data 中定义 user 属性', () => {
      wrapper = mountProductEntry()
      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户',
      }
      wrapper = mountProductEntry(createUserStoreMock(testUser))

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toEqual(testUser)
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      wrapper = mountProductEntry(createNullUserStoreMock())

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })

  it('created 与 mounted 时应调用 applyTable 初始化展示列', async () => {
    wrapper = mountProductEntry()
    await nextTick()
    await wrapper.vm.$nextTick()
    expect(applyTableMock).toHaveBeenCalledTimes(2)
    expect(applyTableMock).toHaveBeenCalledWith(
      expect.any(Object),
      expect.objectContaining({
        allCols: entryAllCols,
        preset: entryPresets.productEntry,
        colPrefName: 'colPref-productEntry',
        needFilter: false,
      })
    )
  })

  it('列偏好持久化由 ColumnFilter 承担，页面无 changeColumn 包装', () => {
    wrapper = mountProductEntry()
    expect(typeof wrapper.vm.changeColumn).toBe('undefined')
  })

  it('applyTable 后 columnSettingsList 应包含必选列定义', () => {
    const requiredValues = resolvePresetCols(entryPresets.productEntry, entryAllCols)
      .filter((item) => item.required)
      .map((item) => item.value)
    expect(requiredValues).toEqual(['index', 'entry', 'operation'])
  })
})

describe('ProductEntry - 展示条件变更重算表格高度', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  it('展示条件 ColumnFilter change 时应调用 setTableHeight', async () => {
    wrapper = mountProductEntry()
    await nextTick()
    await wrapper.vm.$nextTick()

    setTableHeightMock.mockClear()

    const columnFilters = wrapper.findAllComponents(ColumnFilter)
    expect(columnFilters.length).toBeGreaterThan(0)

    const searchConditionFilter = columnFilters.find(
      (component) => component.props('title') === '展示条件'
    )

    expect(searchConditionFilter).toBeTruthy()
    searchConditionFilter.vm.$emit('change', ['entry', 'language', 'translate'])
    await nextTick()
    await wrapper.vm.$nextTick()

    expect(setTableHeightMock).toHaveBeenCalled()
  })

  it('setTableHeight 方法应委托 tableUtils.setTableHeight', async () => {
    wrapper = mountProductEntry()
    await nextTick()
    await wrapper.vm.$nextTick()

    setTableHeightMock.mockClear()
    wrapper.vm.setTableHeight()
    await nextTick()
    await wrapper.vm.$nextTick()

    expect(setTableHeightMock).toHaveBeenCalledWith(
      wrapper.vm,
      -8,
      166,
      84,
      { ok: true, h: wrapper.vm.box }
    )
  })
})

describe('ProductEntry - getEntryByClassfy customCheckTransLength', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  it('有长度且有语种时，应在 body 中传递 customCheckTransLength', async () => {
    wrapper = mountProductEntry()
    await nextTick()

    wrapper.vm.product = testProduct
    wrapper.vm.search.language = '英文'
    wrapper.vm.search.customCheckTransLength = 50
    wrapper.vm.getEntryByClassfy()

    await nextTick()

    expect(getEntryByClassfyMock).toHaveBeenCalledTimes(1)
    const [params, data] = getEntryByClassfyMock.mock.calls[0]
    expect(params.translateType).toBe('英文')
    expect(data.customCheckTransLength).toBe(50)
  })

  it('有长度但无语种时，应提示且不调用 API', async () => {
    wrapper = mountProductEntry()
    await nextTick()

    wrapper.vm.product = testProduct
    wrapper.vm.search.language = null
    wrapper.vm.search.customCheckTransLength = 50
    wrapper.vm.getEntryByClassfy()

    await nextTick()

    expect(message.info).toHaveBeenCalledWith('请选择翻译语种！')
    expect(getEntryByClassfyMock).not.toHaveBeenCalled()
  })

  it('无长度时，body 中不应包含 customCheckTransLength', async () => {
    wrapper = mountProductEntry()
    await nextTick()

    wrapper.vm.product = testProduct
    wrapper.vm.search.language = '英文'
    wrapper.vm.search.customCheckTransLength = null
    wrapper.vm.getEntryByClassfy()

    await nextTick()

    expect(getEntryByClassfyMock).toHaveBeenCalledTimes(1)
    const [, data] = getEntryByClassfyMock.mock.calls[0]
    expect(data).not.toHaveProperty('customCheckTransLength')
  })
})

describe('ProductEntry - 浏览省略与编辑文本域', () => {
  let wrapper

  const tableBodyStub = {
    name: 'ATable',
    props: ['columns', 'dataSource'],
    template: `
      <div class="table-stub">
        <div class="header-cell">
          <slot name="headerCell" :title="'词条'" :column="{ colValue: 'entry', dataIndex: 'entry', title: '词条' }" />
          <slot name="headerCell" :title="'tag'" :column="{ dataIndex: 'tag' }" />
          <slot name="headerCell" :title="'comment'" :column="{ dataIndex: 'comment' }" />
        </div>
        <template v-for="record in (dataSource || [])" :key="record.id">
          <div
            v-for="col in (columns || [])"
            :key="col.dataIndex"
            class="cell"
            :data-col="col.dataIndex"
          >
            <slot name="bodyCell" :column="col" :record="record" :text="record[col.dataIndex]" />
          </div>
        </template>
      </div>
    `,
  }

  function mountWithTableStub(extra = {}) {
    return mount(ProductEntry, {
      props: {
        currentProduct: testProduct,
        boxHeight: 600,
        productEdit: false,
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          SearchBox: {
            template: '<div><slot name="form" /></div>',
          },
          'a-form': {
            template: '<form><slot /></form>',
          },
          'a-row': {
            template: '<div class="a-row"><slot /></div>',
          },
          'a-form-item': {
            template: '<div class="a-form-item"><slot /></div>',
          },
          DataBox: {
            template: '<div><slot name="data" /></div>',
          },
          OperationArea: true,
          'a-table': tableBodyStub,
          'a-button': true,
          'a-config-provider': {
            template: '<div><slot /></div>',
          },
          AccurSearchButton: true,
          EntryStateSelect: true,
          TransStateSelect: true,
          CreateVersionModal: true,
          SecondClassify: true,
          Dictionary: true,
          CustomModal: true,
          BackFillModal: true,
          BackFillModal_v2: true,
          BackFillModal_v2_5: true,
          BackFillModal_v3: true,
          GitCommitButton: true,
          ColumnFilter: true,
          EntryStateBadge: true,
          TransStateBadge: true,
          CellOverflowTooltip: {
            name: 'CellOverflowTooltip',
            props: ['content'],
            template: '<span class="cell-overflow-tooltip-stub"><slot /></span>',
          },
          TableHeaderHint: {
            props: ['title'],
            template: '<span class="table-header-hint-stub">{{ title }}</span>',
          },
          ...extra.stubs,
        },
      },
    })
  }

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  it('浏览态应渲染 CellOverflowTooltip', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.dataSource = [
      {
        id: 'entry-1',
        entry: '短词条',
        english: 'a'.repeat(200),
        comment: '备注',
      },
    ]
    wrapper.vm.columns = [
      { dataIndex: 'index', title: '序号', colValue: 'index' },
      { dataIndex: 'entry', title: '词条', colValue: 'entry' },
      { dataIndex: 'english', title: '英文', colValue: 'english' },
      { dataIndex: 'comment', title: 'comment', colValue: 'comment' },
    ]
    await nextTick()
    expect(wrapper.findAll('.cell-overflow-tooltip-stub').length).toBeGreaterThan(0)
    expect(wrapper.find('[data-col="index"]').find('.cell-overflow-tooltip-stub').exists()).toBe(false)
  })

  it('浏览态 tag 列使用共享 TableTagCell 渲染', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.dataSource = [
      {
        id: 'entry-1',
        tag: '前端;后端；测试',
      },
    ]
    wrapper.vm.columns = [
      { dataIndex: 'tag', title: 'tag', colValue: 'tag' },
    ]
    await nextTick()

    expect(wrapper.findComponent({ name: 'TableTagCell' }).exists()).toBe(true)
  })

  it('tag/comment 列头使用统一提示组件', async () => {
    wrapper = mountWithTableStub()
    await nextTick()

    expect(wrapper.findAll('.table-header-hint-stub').map((node) => node.text())).toEqual([
      'tag',
      'comment',
    ])
  })

  it('编辑态 entry/translate 使用 TextArea 且传 autoSize.maxRows，不包浏览 Tooltip', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.dataSource = [
      {
        id: 'entry-1',
        entry: '编辑词条',
        english: 'edit english',
        comment: '备注',
      },
    ]
    wrapper.vm.columns = [
      { dataIndex: 'entry', title: '词条', colValue: 'entry' },
      { dataIndex: 'english', title: '英文', colValue: 'english' },
      { dataIndex: 'comment', title: 'comment', colValue: 'comment' },
    ]
    wrapper.vm.editableData = {
      'entry-1': {
        entry: '编辑词条',
        english: 'edit english',
        comment: '备注',
      },
    }
    await nextTick()

    const entryCell = wrapper.find('[data-col="entry"]')
    expect(entryCell.find('.cell-overflow-tooltip-stub').exists()).toBe(false)

    const textAreas = wrapper.findAllComponents({ name: 'TextAreaIME' })
    expect(textAreas.length).toBeGreaterThanOrEqual(2)
    textAreas.forEach((area) => {
      expect(area.props('autoSize')).toEqual({ minRows: 1, maxRows: 5 })
    })

    const commentCell = wrapper.find('[data-col="comment"]')
    expect(commentCell.findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(commentCell.find('.cell-overflow-tooltip-stub').exists()).toBe(false)
  })

  it('编辑态 inputColumn 仍使用 Input（如 diFileName）', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.dataSource = [
      {
        id: 'entry-1',
        entry: '词条',
        diFileName: 'path/to.dic',
      },
    ]
    wrapper.vm.columns = [
      { dataIndex: 'entry', title: '词条', colValue: 'entry' },
      { dataIndex: 'diFileName', title: '辞典名称', colValue: 'diFileName' },
    ]
    wrapper.vm.editableData = {
      'entry-1': {
        entry: '词条',
        diFileName: 'path/to.dic',
      },
    }
    await nextTick()

    const diCell = wrapper.find('[data-col="diFileName"]')
    expect(diCell.findComponent({ name: 'InputIME' }).exists()).toBe(true)
    expect(diCell.find('.cell-overflow-tooltip-stub').exists()).toBe(false)
  })

  it('编辑态 remark/abbr 使用 TextArea', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.dataSource = [
      {
        id: 'entry-1',
        entry: '词条',
        remark: '备注',
        abbr: 'abbr',
      },
    ]
    wrapper.vm.columns = [
      { dataIndex: 'remark', title: '备注', colValue: 'remark' },
      { dataIndex: 'abbr', title: 'abbr', colValue: 'abbr' },
    ]
    wrapper.vm.editableData = {
      'entry-1': { remark: '备注', abbr: 'abbr' },
    }
    await nextTick()

    expect(wrapper.find('[data-col="remark"]').findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(wrapper.find('[data-col="abbr"]').findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
  })

  it('编辑态 maxLength、entryLength 与 writeType 使用专用控件', async () => {
    wrapper = mountWithTableStub({
      stubs: {
        'a-input-number': {
          name: 'AInputNumber',
          template: '<input class="ant-input-number-stub" />',
        },
        'a-select': {
          name: 'ASelect',
          template: '<select class="ant-select-stub" />',
        },
      },
    })
    await nextTick()
    wrapper.vm.dataSource = [
      { id: 'entry-1', entryLength: 10, maxLength: 100, writeType: 'TS' },
    ]
    wrapper.vm.columns = [
      { dataIndex: 'entryLength', title: '词条字符数', colValue: 'entryLength' },
      { dataIndex: 'maxLength', title: '翻译最大长度', colValue: 'maxLength' },
      { dataIndex: 'writeType', title: '回写类型', colValue: 'writeType' },
    ]
    wrapper.vm.editableData = {
      'entry-1': { entryLength: 10, maxLength: 100, writeType: 'TS' },
    }
    await nextTick()

    expect(wrapper.find('[data-col="entryLength"]').find('.ant-input-number-stub').exists()).toBe(true)
    expect(wrapper.find('[data-col="maxLength"]').find('.ant-input-number-stub').exists()).toBe(true)
    expect(wrapper.find('[data-col="writeType"]').find('.ant-select-stub').exists()).toBe(true)
  })

  it('浏览态 maxLength 为 0 时 Tooltip 内容留空', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.dataSource = [{ id: 'entry-1', maxLength: 0 }]
    wrapper.vm.columns = [
      { dataIndex: 'maxLength', title: '翻译最大长度', colValue: 'maxLength' },
    ]
    wrapper.vm.editableData = {}
    await nextTick()
    const tip = wrapper.find('[data-col="maxLength"]').findComponent({ name: 'CellOverflowTooltip' })
    expect(tip.exists()).toBe(true)
    expect(tip.props('content')).toBe('')
  })

  it('writeTypeOptions 选项 label 为 TS/DI', () => {
    wrapper = mountWithTableStub()
    expect(wrapper.vm.writeTypeOptions).toEqual([
      { label: 'TS', value: 'TS' },
      { label: 'DI', value: 'DI' },
    ])
  })

  it('prepareEntryForSave 将 entryLength/maxLength 数字转为字符串', () => {
    wrapper = mountWithTableStub()
    expect(wrapper.vm.prepareEntryForSave({ id: '1', entryLength: 10, maxLength: 120 })).toEqual({
      id: '1',
      entryLength: '10',
      maxLength: '120',
    })
    expect(wrapper.vm.prepareEntryForSave({ id: '1', entryLength: null, maxLength: null })).toEqual({
      id: '1',
      entryLength: '',
      maxLength: '',
    })
  })

  it('entryParams 列数组互斥且 inputColumn 不含 entryLength/maxLength/writeType', () => {
    const overlap = entryParams.inputColumn.filter((col) =>
      entryParams.textareaInputColumn.includes(col)
    )
    expect(overlap).toEqual([])
    expect(entryParams.inputColumn).not.toContain('entryLength')
    expect(entryParams.inputColumn).not.toContain('maxLength')
    expect(entryParams.inputColumn).not.toContain('writeType')
  })

  it('productEntry preset 不暴露内部 id 列', () => {
    const cols = resolvePresetCols(entryPresets.productEntry, entryAllCols)
    const values = cols.map((c) => c.value)
    expect(values).not.toContain('enTransId')
    expect(values).not.toContain('dbRID')
  })
})

describe('ProductEntry - 模块上限挂到词条行', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  it('paintModuleLimits 按 classfy1 刷 maxByte/foreignMaxByte，含子行', () => {
    wrapper = mountProductEntry()
    wrapper.vm.classifyLimit = {
      模组A: { maxByte: 50, foreignMaxByte: 120 },
    }
    wrapper.vm.dataSource = [
      {
        id: '1',
        classfy1: '模组A',
        children: [{ id: '1-1', classfy1: '模组A' }],
      },
      { id: '2', classfy1: '未知' },
    ]
    wrapper.vm.paintModuleLimits()
    expect(wrapper.vm.dataSource[0].maxByte).toBe(50)
    expect(wrapper.vm.dataSource[0].foreignMaxByte).toBe(120)
    expect(wrapper.vm.dataSource[0].children[0].maxByte).toBe(50)
    expect(wrapper.vm.dataSource[1].maxByte).toBeNull()
    expect(wrapper.vm.dataSource[1].foreignMaxByte).toBeNull()
  })

  it('模块下新增词条 maxLength 用 foreignMaxByte，展示列用模块上限', () => {
    wrapper = mountProductEntry()
    wrapper.vm.product = {
      type: 'module',
      title: '模组A',
      parentId: 'p1',
      key: 'm1',
      maxByte: 50,
      foreignMaxByte: 120,
    }
    wrapper.vm.pagination = { current: 1, pageSize: 10, total: 0 }
    wrapper.vm.dataSource = []
    wrapper.vm.getRowClassify2Option = vi.fn()
    wrapper.vm.$nextTick = () => {}
    wrapper.vm.addEntry()
    const row = wrapper.vm.dataSource[0]
    expect(row.maxLength).toBe(120)
    expect(row.maxByte).toBe(50)
    expect(row.foreignMaxByte).toBe(120)
  })
})

describe('ProductEntry - 校验规则显隐与操作区布局', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  function mountWithOperateSlot() {
    return mount(ProductEntry, {
      props: {
        currentProduct: testProduct,
        boxHeight: 600,
        productEdit: true,
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          SearchBox: { template: '<div />' },
          DataBox: {
            template: '<div class="data-box-stub"><slot name="operate" /></div>',
          },
          OperationArea: true,
          'a-table': true,
          'a-button': { template: '<button><slot /></button>' },
          AccurSearchButton: true,
          EntryStateSelect: true,
          TransStateSelect: true,
          CreateVersionModal: true,
          SecondClassify: true,
          Dictionary: true,
          CustomModal: true,
          BackFillModal_v3: true,
          GitCommitButton: true,
          ColumnFilter: true,
          RulesDropdown: {
            name: 'RulesDropdown',
            props: ['options'],
            template: '<div class="rules-dropdown-stub" />',
          },
          'a-badge': true,
          'a-select': true,
        },
      },
    })
  }

  it('无编辑行时不显示校验规则；有 editableData 时显示在左栏', async () => {
    wrapper = mountWithOperateSlot()
    expect(wrapper.vm.hasEditingRows).toBe(false)
    expect(wrapper.findComponent({ name: 'RulesDropdown' }).exists()).toBe(false)
    expect(wrapper.find('.entry-toolbar-left').exists()).toBe(true)
    expect(wrapper.find('.entry-toolbar-right').exists()).toBe(true)

    wrapper.vm.editableData = { e1: { id: 'e1', entry: '断路器' } }
    await nextTick()

    expect(wrapper.vm.hasEditingRows).toBe(true)
    const rules = wrapper.findComponent({ name: 'RulesDropdown' })
    expect(rules.exists()).toBe(true)
    expect(wrapper.find('.entry-toolbar-left .entry-toolbar-rules').exists()).toBe(true)

    wrapper.vm.editableData = {}
    await nextTick()
    expect(wrapper.findComponent({ name: 'RulesDropdown' }).exists()).toBe(false)
  })
})

describe('ProductEntry - 操作列编辑态加宽', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  it('进入编辑态操作列从 80 加宽到 130；退出恢复 80', async () => {
    wrapper = mount(ProductEntry, {
      props: {
        currentProduct: testProduct,
        boxHeight: 600,
        productEdit: true,
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          SearchBox: { template: '<div />' },
          DataBox: { template: '<div><slot name="operate" /><slot name="data" /></div>' },
          OperationArea: true,
          'a-table': true,
          AccurSearchButton: true,
          EntryStateSelect: true,
          TransStateSelect: true,
          CreateVersionModal: true,
          SecondClassify: true,
          Dictionary: true,
          CustomModal: true,
          BackFillModal_v3: true,
          GitCommitButton: true,
          ColumnFilter: true,
          RulesDropdown: true,
        },
      },
    })

    wrapper.vm.columns = [
      { title: '操作', dataIndex: 'operation', width: 80, align: 'center' },
    ]
    expect(wrapper.vm.hasEditingRows).toBe(false)

    wrapper.vm.editableData = { e1: { id: 'e1', entry: '断路器' } }
    await nextTick()
    expect(wrapper.vm.columns.find((c) => c.dataIndex === 'operation').width).toBe(130)
    expect(wrapper.vm.columns.find((c) => c.dataIndex === 'operation').resizable).toBe(false)

    wrapper.vm.editableData = {}
    await nextTick()
    expect(wrapper.vm.columns.find((c) => c.dataIndex === 'operation').width).toBe(80)
    expect(wrapper.vm.columns.find((c) => c.dataIndex === 'operation').resizable).toBe(false)
  })
})

describe('ProductEntry - 修改校验规则复检编辑态', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  it('rulesOptions 变更后对编辑行重跑校验写红字', async () => {
    wrapper = mount(ProductEntry, {
      props: {
        currentProduct: testProduct,
        boxHeight: 600,
        productEdit: true,
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          SearchBox: { template: '<div />' },
          DataBox: { template: '<div><slot name="operate" /><slot name="data" /></div>' },
          OperationArea: true,
          'a-table': true,
          AccurSearchButton: true,
          EntryStateSelect: true,
          TransStateSelect: true,
          CreateVersionModal: true,
          SecondClassify: true,
          Dictionary: true,
          CustomModal: true,
          BackFillModal_v3: true,
          GitCommitButton: true,
          ColumnFilter: true,
          RulesDropdown: true,
        },
      },
    })

    wrapper.vm.editableData = {
      e1: { id: 'e1', entry: '断路器', english: 'x' },
    }
    wrapper.vm.rules = {
      e1: {
        english: [
          {
            validator: async () => {
              throw new Error('规则变更后失败')
            },
          },
        ],
      },
    }
    wrapper.vm.cellErrors = {}

    wrapper.vm.rulesOptions = wrapper.vm.rulesOptions.map((item) => ({
      ...item,
      checked: item.key === 'special' ? !item.checked : item.checked,
    }))
    await nextTick()
    // deep watch 是 async handler，再等一轮
    await nextTick()
    await Promise.resolve()

    expect(wrapper.vm.cellErrors.e1?.english).toBe('规则变更后失败')
    expect(wrapper.vm.editableData.e1).toBeDefined()
  })
})

describe('ProductEntry - 单元格校验触发时机', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  it('单元格 change 与 blur 均触发 applyCell 写红字', async () => {
    wrapper = mount(ProductEntry, {
      props: {
        currentProduct: testProduct,
        boxHeight: 600,
        productEdit: true,
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          SearchBox: { template: '<div />' },
          DataBox: { template: '<div><slot name="operate" /><slot name="data" /></div>' },
          OperationArea: true,
          'a-table': true,
          AccurSearchButton: true,
          EntryStateSelect: true,
          TransStateSelect: true,
          CreateVersionModal: true,
          SecondClassify: true,
          Dictionary: true,
          CustomModal: true,
          BackFillModal_v3: true,
          GitCommitButton: true,
          ColumnFilter: true,
          RulesDropdown: true,
        },
      },
    })

    const record = { id: 'e1', entry: '断路器', english: '' }
    wrapper.vm.editableData = {
      e1: { id: 'e1', entry: '断路器', english: '' },
    }
    wrapper.vm.rules = {
      e1: {
        english: [
          {
            validator: async () => {
              throw new Error('翻错了')
            },
          },
        ],
      },
    }
    wrapper.vm.cellErrors = {}

    await wrapper.vm.onCellInput('bad', record, { dataIndex: 'english' })
    await nextTick()
    expect(wrapper.vm.cellErrors.e1?.english).toBe('翻错了')

    wrapper.vm.cellErrors = {}
    await wrapper.vm.onCellBlur(record, { dataIndex: 'english' })
    await nextTick()
    expect(wrapper.vm.cellErrors.e1?.english).toBe('翻错了')
  })
})

describe('ProductEntry - 保存确认与 notes 兜底', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('confirmSave：确认后带 EDIT_NOTES 调用 updateEntryInfoList', async () => {
    updateEntryInfoList.mockResolvedValue({
      data: {
        list: [{ id: 'e1', success: true, message: 'OK' }],
        totalNum: 1,
      },
    })

    wrapper = mountProductEntry()
    wrapper.vm.dataSource = [{ id: 'e1', entry: '断路器' }]
    wrapper.vm.editableData = { e1: { id: 'e1', entry: '断路器', english: 'breaker' } }
    wrapper.vm.selectEntry = [{ id: 'e1', entry: '断路器', english: 'old' }]
    wrapper.vm.selectedRows = wrapper.vm.selectEntry
    wrapper.vm.selectedRowKeys = ['e1']
    wrapper.vm.rules = {}
    wrapper.vm.rowClassify2Option = {}
    wrapper.vm.getEntryByClassfy = vi.fn()

    const row = { id: 'e1', entry: '断路器', english: 'breaker' }
    wrapper.vm.confirmSave([row])
    await nextTick()
    await Promise.resolve()

    expect(Modal.confirm).toHaveBeenCalled()
    expect(updateEntryInfoList).toHaveBeenCalledWith([row], {
      notes: EDIT_NOTES,
    })
    expect(wrapper.vm.editableData.e1).toBeUndefined()
    expect(wrapper.vm.selectEntry).toEqual([row])
    expect(wrapper.vm.selectedRows).toBe(wrapper.vm.selectEntry)
  })

  it('confirmSave：空列表不弹确认', () => {
    wrapper = mountProductEntry()
    wrapper.vm.confirmSave([])
    expect(Modal.confirm).not.toHaveBeenCalled()
    expect(updateEntryInfoList).not.toHaveBeenCalled()
  })

  it('203 部分成功时只用成功提交行更新已选清单', async () => {
    const oldSuccessEntry = {
      id: 'e1',
      entry: '断路器',
      english: 'old breaker',
      cachedOnly: true,
    }
    const oldFailedEntry = {
      id: 'e2',
      entry: '接触器',
      english: 'old contactor',
    }
    const savedEntry = {
      id: 'e1',
      entry: '断路器',
      english: 'circuit breaker',
    }
    const failedSubmission = {
      id: 'e2',
      entry: '接触器',
      english: 'contactor',
    }
    updateEntryInfoList.mockResolvedValueOnce({
      code: 203,
      data: {
        list: [
          { id: 'e1', success: true, message: 'OK' },
          { id: 'e2', success: false, message: '落库失败' },
        ],
        totalNum: 2,
      },
    })

    wrapper = mountProductEntry()
    wrapper.vm.dataSource = [oldSuccessEntry, oldFailedEntry]
    wrapper.vm.selectEntry = [oldSuccessEntry, oldFailedEntry]
    wrapper.vm.selectedRows = wrapper.vm.selectEntry
    wrapper.vm.selectedRowKeys = wrapper.vm.selectEntry.map((item) => item.id)
    wrapper.vm.getEntryByClassfy = vi.fn()

    await wrapper.vm.saveRows([savedEntry, failedSubmission])

    expect(wrapper.vm.selectEntry).toEqual([savedEntry, oldFailedEntry])
    expect(wrapper.vm.selectedRows).toBe(wrapper.vm.selectEntry)
    expect(wrapper.vm.selectedRowKeys).toEqual(['e1', 'e2'])
    expect(wrapper.vm.dataSource).toEqual([savedEntry, oldFailedEntry])
    expect(message.warning).toHaveBeenCalledWith(
      '有 1 条保存失败：e2: 落库失败'
    )
  })

  it('批量保存请求失败时不改动已选清单', async () => {
    const cachedEntry = { id: 'e1', entry: '断路器', english: 'old breaker' }
    updateEntryInfoList.mockRejectedValueOnce(new Error('请求失败'))

    wrapper = mountProductEntry()
    wrapper.vm.selectEntry = [cachedEntry]
    wrapper.vm.selectedRows = wrapper.vm.selectEntry
    wrapper.vm.selectedRowKeys = ['e1']

    await wrapper.vm.saveRows([
      { ...cachedEntry, english: 'circuit breaker' },
    ])

    expect(wrapper.vm.selectEntry).toEqual([cachedEntry])
    expect(wrapper.vm.selectedRows).toBe(wrapper.vm.selectEntry)
    expect(wrapper.vm.selectedRowKeys).toEqual(['e1'])
  })
})

describe('ProductEntry - 缺失产品 ID 的请求参数', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('分类下删除词条后的列表刷新不发送 productID', async () => {
    wrapper = mountProductEntry()
    await nextTick()
    wrapper.vm.product = { type: 'classify', key: 'classify-id-1' }
    wrapper.vm.productVersions = [{ id: null, tableName: 't_entry_info' }]
    wrapper.vm.currentVersion = null
    wrapper.vm.selectedRowKeys = ['entry-id-1']
    getEntryByClassfyMock.mockClear()
    deleteEntryInfo.mockClear()

    wrapper.vm.deleteEntry()
    await Promise.resolve()
    await nextTick()

    expect(deleteEntryInfo).toHaveBeenCalledWith(['entry-id-1'], {
      tableName: 't_entry_info',
    })
    const [, refreshData] = getEntryByClassfyMock.mock.calls.at(-1)
    expect(refreshData).not.toHaveProperty('productID')
  })

  it('分类下全量查询不发送 productID', async () => {
    wrapper = mountProductEntry()
    await nextTick()
    wrapper.vm.product = { type: 'classify', key: 'classify-id-1' }
    wrapper.vm.currentVersion = null
    getEntryByClassfyMock.mockClear()

    wrapper.vm.selectAllEntry()
    await nextTick()

    const [, queryData] = getEntryByClassfyMock.mock.calls.at(-1)
    expect(queryData).not.toHaveProperty('productID')
  })

  it('产品和模块查询继续发送有效产品 ID', async () => {
    wrapper = mountProductEntry()
    await nextTick()

    for (const [product, expectedProductID] of [
      [{ type: 'product', key: 'product-id-2' }, 'product-id-2'],
      [{ type: 'module', key: 'module-id-2', parentId: 'product-id-3' }, 'product-id-3'],
    ]) {
      wrapper.vm.product = product
      wrapper.vm.currentVersion = null
      getEntryByClassfyMock.mockClear()

      wrapper.vm.getEntryByClassfy()

      const [, queryData] = getEntryByClassfyMock.mock.calls.at(-1)
      expect(queryData.productID).toBe(expectedProductID)
    }
  })

  it('产品 ID 为 null、空串或字符串 null 时省略字段', async () => {
    wrapper = mountProductEntry()
    await nextTick()

    for (const productID of [undefined, null, '', 'null']) {
      wrapper.vm.product = { type: 'product', key: productID }
      wrapper.vm.currentVersion = null
      getEntryByClassfyMock.mockClear()

      wrapper.vm.getEntryByClassfy()

      const [, queryData] = getEntryByClassfyMock.mock.calls.at(-1)
      expect(queryData).not.toHaveProperty('productID')
    }
  })

  it('缺少产品 ID 时不查询版本，新增行省略 productID', async () => {
    wrapper = mountProductEntry()
    await nextTick()
    wrapper.vm.product = { type: 'classify', key: 'classify-id-1' }
    getVersionByName.mockClear()

    wrapper.vm.getProductVersion()
    await Promise.resolve()

    expect(getVersionByName).not.toHaveBeenCalled()

    wrapper.vm.createClose()
    expect(getVersionByName).not.toHaveBeenCalled()

    wrapper.vm.product = { type: 'product', key: 'null', title: '测试产品' }
    wrapper.vm.getRowClassify2Option = vi.fn()
    wrapper.vm.addEntry()

    expect(wrapper.vm.dataSource.at(-1)).not.toHaveProperty('productID')
  })

  it('版本查询与新增行继续保留有效产品 ID', async () => {
    wrapper = mountProductEntry()
    await nextTick()
    wrapper.vm.product = { type: 'product', key: 'product-id-4', title: '测试产品' }
    getVersionByName.mockClear()

    wrapper.vm.getProductVersion()
    await Promise.resolve()

    expect(getVersionByName).toHaveBeenCalledWith({
      versionName: '',
      productID: 'product-id-4',
    })

    wrapper.vm.getRowClassify2Option = vi.fn()
    wrapper.vm.addEntry()

    expect(wrapper.vm.dataSource.at(-1).productID).toBe('product-id-4')
  })
})

describe('ProductEntry - 查询响应竞态', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  /**
   * 挂载产品词条表并等待组件初始化完成。
   * @returns {Promise<any>} 初始化完成后的当前测试组件。
   */
  async function mountReadyEntry() {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    message.error.mockClear()
    return wrapper
  }

  it('较晚返回的最新筛选结果保留在表格中', async () => {
    await mountReadyEntry()
    const olderRequest = createDeferred()
    const latestRequest = createDeferred()
    getEntryByClassfyMock
      .mockReturnValueOnce(olderRequest.promise)
      .mockReturnValueOnce(latestRequest.promise)

    wrapper.vm.search.entry = '旧条件'
    wrapper.vm.getEntryByClassfy()
    wrapper.vm.search.entry = '新条件'
    wrapper.vm.getEntryByClassfy()

    latestRequest.resolve({ data: { list: [{ id: 'latest' }], totalNum: 1 } })
    await flushPromises()
    olderRequest.resolve({ data: { list: [{ id: 'stale' }], totalNum: 1 } })
    await flushPromises()

    expect(wrapper.vm.dataSource).toMatchObject([{ id: 'latest' }])
    expect(wrapper.vm.pagination.total).toBe(1)
    expect(wrapper.vm.loading).toBe(false)
  })

  it('版本切换、分页和刷新使用同一最新查询版本', async () => {
    await mountReadyEntry()
    const initialQuery = createDeferred()
    const versionQuery = createDeferred()
    const pageQuery = createDeferred()
    const refreshQuery = createDeferred()
    getEntryByClassfyMock
      .mockReturnValueOnce(initialQuery.promise)
      .mockReturnValueOnce(versionQuery.promise)
      .mockReturnValueOnce(pageQuery.promise)
      .mockReturnValueOnce(refreshQuery.promise)

    wrapper.vm.getEntryByClassfy()
    wrapper.vm.changeVersion('version-2')
    wrapper.vm.pageChange(2, 20)
    wrapper.vm.refreshTable()
    refreshQuery.resolve({ data: { list: [{ id: 'latest' }], totalNum: 1 } })
    await flushPromises()
    pageQuery.resolve({ data: { list: [{ id: 'stale-page' }], totalNum: 1 } })
    versionQuery.resolve({ data: { list: [{ id: 'stale-version' }], totalNum: 1 } })
    initialQuery.resolve({ data: { list: [{ id: 'stale-initial' }], totalNum: 1 } })
    await flushPromises()

    expect(wrapper.vm.dataSource).toMatchObject([{ id: 'latest' }])
    expect(wrapper.vm.pagination.current).toBe(2)
    expect(wrapper.vm.currentVersion).toBe('version-2')
    expect(wrapper.vm.loading).toBe(false)
  })

  it('过期查询错误不提示，也不清除最新请求的 loading', async () => {
    await mountReadyEntry()
    const olderRequest = createDeferred()
    const latestRequest = createDeferred()
    getEntryByClassfyMock
      .mockReturnValueOnce(olderRequest.promise)
      .mockReturnValueOnce(latestRequest.promise)

    wrapper.vm.getEntryByClassfy()
    wrapper.vm.search.entry = '新条件'
    wrapper.vm.getEntryByClassfy()
    olderRequest.reject(new Error('旧查询失败'))
    await flushPromises()

    expect(message.error).not.toHaveBeenCalled()
    expect(wrapper.vm.loading).toBe(true)

    latestRequest.resolve({ data: { list: [{ id: 'latest' }], totalNum: 1 } })
    await flushPromises()
    expect(wrapper.vm.dataSource).toMatchObject([{ id: 'latest' }])
    expect(wrapper.vm.loading).toBe(false)
  })

  it('初始化只发起一次词条表查询', async () => {
    await mountReadyEntry()
    wrapper.vm.product = { type: 'product', key: 'product-id-init', title: '测试产品' }
    wrapper.vm.selectFirstClassify = vi.fn().mockResolvedValue()
    wrapper.vm.getI18nAdress = vi.fn().mockResolvedValue()
    getEntryByClassfyMock.mockClear()

    await wrapper.vm.init()
    await flushPromises()

    expect(getEntryByClassfyMock).toHaveBeenCalledTimes(1)
    expect(getVersionByName).toHaveBeenCalledWith({
      versionName: '',
      productID: 'product-id-init',
    })
  })

  it.each([
    ['完成状态', 1],
    ['待启动状态', 0],
  ])('忽略过期冗余校验%s响应，不覆盖列表或启动任务', async (label, state) => {
    await mountReadyEntry()
    const statusRequest = createDeferred()
    const latestListRequest = createDeferred()
    getCheckNotUseEntry.mockReturnValueOnce(statusRequest.promise)
    getEntryByClassfyMock.mockReturnValueOnce(latestListRequest.promise)
    wrapper.vm.search.i18nURL = 'http://i18n.test'

    wrapper.vm.getCheckNotUseEntry()
    wrapper.vm.getEntryByClassfy()
    latestListRequest.resolve({
      data: { list: [{ id: 'latest' }], totalNum: 1 },
    })
    await flushPromises()
    statusRequest.resolve({
      data: { state, list: [{ id: 'stale' }], totalNum: 1 },
    })
    await flushPromises()

    expect(wrapper.vm.dataSource).toMatchObject([{ id: 'latest' }])
    expect(checkNotUseEntry).not.toHaveBeenCalled()
  })
})

describe('ProductEntry - 全选加载状态', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('并发全选请求全部结束后才显示已选数量并关闭 loading', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    const firstRequest = createDeferred()
    const secondRequest = createDeferred()
    getEntryByClassfyMock
      .mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise)
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品' }
    wrapper.vm.selectEntry = [{ id: 'existing' }]

    wrapper.vm.selectAllEntry()
    wrapper.vm.selectAllEntry()
    await nextTick()

    expect(wrapper.vm.selectAllLoading).toBe(true)

    firstRequest.resolve({ data: { list: [{ id: 'first' }], totalNum: 1 } })
    await flushPromises()
    expect(wrapper.vm.selectAllLoading).toBe(true)

    secondRequest.resolve({ data: { list: [{ id: 'second' }], totalNum: 1 } })
    await flushPromises()
    expect(wrapper.vm.selectAllLoading).toBe(false)
    expect(wrapper.vm.selectEntry).toHaveLength(3)
  })

  it('全选请求失败后恢复已选数量并关闭 loading', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    const failedRequest = createDeferred()
    getEntryByClassfyMock.mockReturnValueOnce(failedRequest.promise)
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品' }
    wrapper.vm.selectEntry = [{ id: 'existing' }]

    wrapper.vm.selectAllEntry()
    await nextTick()

    expect(wrapper.vm.selectAllLoading).toBe(true)

    failedRequest.reject(new Error('全选请求失败'))
    await flushPromises()

    expect(wrapper.vm.selectAllLoading).toBe(false)
    expect(wrapper.vm.selectEntry).toHaveLength(1)
  })
})

describe('ProductEntry - 选择范围', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('表格保留全局已选键，当前数据只命中自身已选行', () => {
    wrapper = mountProductEntry()
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品一' }
    wrapper.vm.dataSource = [
      { id: 'current-id', productID: 'product-1' },
      { id: 'unselected-id', productID: 'product-1' },
    ]
    wrapper.vm.selectEntry = [
      { id: 'other-product-id', productID: 'product-2' },
      { id: 'current-id', productID: 'product-1' },
    ]
    wrapper.vm.selectedRowKeys = wrapper.vm.selectEntry.map((item) => item.id)

    expect(wrapper.vm.selectedRowKeys).toEqual([
      'other-product-id',
      'current-id',
    ])
    expect(
      wrapper.vm.dataSource
        .filter((item) => wrapper.vm.selectedRowKeys.includes(item.id))
        .map((item) => item.id)
    ).toEqual(['current-id'])
  })

  it('查询变化和切换产品后保留全局键，当前数据按 ID 显示勾选', () => {
    wrapper = mountProductEntry()
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品一' }
    wrapper.vm.dataSource = [
      { id: 'product-1-current', productID: 'product-1' },
      { id: 'product-1-unselected', productID: 'product-1' },
    ]
    wrapper.vm.selectEntry = [
      { id: 'product-1-current', productID: 'product-1' },
      { id: 'product-1-previous-query', productID: 'product-1' },
      { id: 'product-2-current', productID: 'product-2' },
    ]
    wrapper.vm.selectedRowKeys = wrapper.vm.selectEntry.map((item) => item.id)

    expect(wrapper.vm.selectedRowKeys).toEqual([
      'product-1-current',
      'product-1-previous-query',
      'product-2-current',
    ])

    wrapper.vm.dataSource = [
      { id: 'product-1-new-query', productID: 'product-1' },
    ]
    expect(
      wrapper.vm.dataSource.filter((item) =>
        wrapper.vm.selectedRowKeys.includes(item.id)
      )
    ).toEqual([])

    wrapper.vm.product = { type: 'product', key: 'product-2', title: '产品二' }
    wrapper.vm.dataSource = [
      { id: 'product-2-current', productID: 'product-2' },
    ]

    expect(wrapper.vm.selectedRowKeys).toEqual([
      'product-1-current',
      'product-1-previous-query',
      'product-2-current',
    ])
    expect(
      wrapper.vm.dataSource
        .filter((item) => wrapper.vm.selectedRowKeys.includes(item.id))
        .map((item) => item.id)
    ).toEqual(['product-2-current'])
    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual([
      'product-1-current',
      'product-1-previous-query',
      'product-2-current',
    ])
  })

  it('上级分类全选按词条 ID 去重并以新查询对象覆盖旧值', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockResolvedValueOnce({
      data: {
        list: [
          { id: 'shared-entry', entry: '分类查询值', classfy1: '分类 A' },
          { id: 'classify-only', entry: '分类独有' },
        ],
        totalNum: 2,
      },
    })
    wrapper.vm.product = { type: 'classify', key: 'classify-a', title: '分类 A' }
    wrapper.vm.selectEntry = [
      { id: 'shared-entry', entry: '产品缓存值', staleField: true },
      { id: 'product-only', entry: '产品独有' },
    ]

    wrapper.vm.selectAllEntry()
    await flushPromises()

    expect(wrapper.vm.selectEntry).toHaveLength(3)
    expect(
      wrapper.vm.selectEntry.find((entry) => entry.id === 'shared-entry')
    ).toEqual({
      id: 'shared-entry',
      entry: '分类查询值',
      classfy1: '分类 A',
    })
    expect(wrapper.vm.selectedRowKeys).toEqual([
      'shared-entry',
      'product-only',
      'classify-only',
    ])
  })

  it('当前产品取勾后按全局已选清单重算计数并移除陈旧产品项', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const productA = { type: 'classify', key: 'category-a', title: '分类 A' }
    const productB = { type: 'product', key: 'product-b', title: '产品 B' }
    const selectedA = [
      { id: 'a-1', productID: 'product-a' },
      { id: 'a-2', productID: 'product-a' },
    ]
    const selectedB = [
      { id: 'b-1', productID: 'product-b' },
      { id: 'b-2', productID: 'product-b' },
      { id: 'b-3', productID: 'product-b' },
      { id: 'b-4', productID: 'product-b' },
    ]
    const newAEntries = [
      { id: 'a-3', productID: 'product-a' },
      { id: 'a-4', productID: 'product-a' },
      { id: 'a-5', productID: 'product-a' },
    ]

    wrapper.vm.product = productA
    await flushPromises()
    wrapper.vm.selectEntry = [...selectedA, ...selectedB]
    wrapper.vm.syncSelectedProducts()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock
      .mockResolvedValueOnce({ data: { list: newAEntries, totalNum: 3 } })
      .mockResolvedValueOnce({ data: { list: newAEntries, totalNum: 3 } })

    wrapper.vm.selectAllEntry()
    await flushPromises()
    expect(wrapper.vm.selectedProducts.products.get('product-a')).toBe(5)
    expect(wrapper.vm.selectedProducts.products.get('product-b')).toBe(4)
    expect(wrapper.vm.selectedProducts.totalNum).toBe(9)

    wrapper.vm.onSelectAll(false, [], [])
    await flushPromises()
    expect(wrapper.vm.selectedProducts.products.get('product-a')).toBe(2)
    expect(wrapper.vm.selectedProducts.products.get('product-b')).toBe(4)
    expect(wrapper.vm.selectedProducts.totalNum).toBe(6)

    wrapper.vm.product = productB
    await flushPromises()
    wrapper.vm.updateSelectedEntries(selectedB)
    expect(wrapper.vm.selectedProducts.products.has('product-a')).toBe(false)
    expect(wrapper.vm.selectedProducts.products.get('product-b')).toBe(4)
    expect(wrapper.vm.selectedProducts.totalNum).toBe(4)
    expect(wrapper.vm.selectedRowKeys).toEqual(selectedB.map((entry) => entry.id))
  })

  it('较早的全选响应不能覆盖较新查询结果中的同 ID 词条', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品一' }
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    const olderRequest = createDeferred()
    const newerRequest = createDeferred()
    getEntryByClassfyMock
      .mockReturnValueOnce(olderRequest.promise)
      .mockReturnValueOnce(newerRequest.promise)

    wrapper.vm.selectAllEntry()
    wrapper.vm.search.entry = '新查询'
    wrapper.vm.selectAllEntry()

    newerRequest.resolve({
      data: { list: [{ id: 'shared-entry', entry: '新查询值' }] },
    })
    await flushPromises()
    olderRequest.resolve({
      data: { list: [{ id: 'shared-entry', entry: '旧查询值' }] },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry).toEqual([
      { id: 'shared-entry', entry: '新查询值' },
    ])
  })

  it('全量选择请求晚于保存响应时保留成功保存的新词条对象', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品一' }
    await flushPromises()
    const oldEntry = {
      id: 'saved-entry',
      productID: 'product-1',
      entry: '断路器',
      english: 'old value',
    }
    const savedEntry = { ...oldEntry, english: 'saved value' }
    const pendingSelection = createDeferred()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingSelection.promise)
    updateEntryInfoList.mockResolvedValueOnce({
      code: 200,
      data: {
        list: [{ id: savedEntry.id, success: true, message: 'OK' }],
        totalNum: 1,
      },
    })
    wrapper.vm.dataSource = [oldEntry]
    wrapper.vm.selectEntry = [oldEntry]
    wrapper.vm.selectedRows = wrapper.vm.selectEntry
    wrapper.vm.selectedRowKeys = [oldEntry.id]
    wrapper.vm.getEntryByClassfy = vi.fn()

    wrapper.vm.selectAllEntry()
    await nextTick()
    await wrapper.vm.saveRows([savedEntry])
    pendingSelection.resolve({ data: { list: [oldEntry], totalNum: 1 } })
    await flushPromises()

    expect(wrapper.vm.selectEntry).toEqual([savedEntry])
    expect(wrapper.vm.selectedRowKeys).toEqual([savedEntry.id])
  })

  it('产品切换后忽略旧取勾响应并保留历史产品选择', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const productA = {
      type: 'product',
      key: 'product-a',
      title: '产品 A',
      children: [],
    }
    const productB = {
      type: 'product',
      key: 'product-b',
      title: '产品 B',
      children: [],
    }
    wrapper.vm.product = productA
    wrapper.vm.selectEntry = [
      { id: 'a-1', productID: 'product-a' },
      { id: 'a-2', productID: 'product-a' },
    ]
    wrapper.vm.syncSelectedProducts()

    const pendingClear = createDeferred()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingClear.promise)
    wrapper.vm.clearCurrentQuerySelection()
    await nextTick()

    wrapper.vm.product = productB
    expect(wrapper.vm.selectionOperations.pendingCount).toBe(0)
    expect(wrapper.vm.selectedProducts.products.get('product-a')).toBe(2)
    expect(wrapper.vm.selectedProducts.totalNum).toBe(2)

    pendingClear.resolve({
      data: {
        list: [
          { id: 'a-1', productID: 'product-a' },
          { id: 'a-2', productID: 'product-a' },
        ],
      },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual(['a-1', 'a-2'])
    expect(wrapper.vm.selectedProducts.products.get('product-a')).toBe(2)
    expect(wrapper.vm.selectedProducts.totalNum).toBe(2)
  })

  it('切回原产品后旧取勾不提交并保留最新全局清单', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const productA = { type: 'product', key: 'product-a', title: '产品 A' }
    const productB = { type: 'product', key: 'product-b', title: '产品 B' }
    const remainingEntry = { id: 'a-2', productID: 'product-a' }
    wrapper.vm.product = productA
    wrapper.vm.selectEntry = [
      { id: 'a-1', productID: 'product-a' },
      remainingEntry,
    ]
    wrapper.vm.syncSelectedProducts()

    const pendingClear = createDeferred()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingClear.promise)
    wrapper.vm.clearCurrentQuerySelection()
    wrapper.vm.onSelect(remainingEntry, true)
    wrapper.vm.product = productB
    wrapper.vm.product = productA

    pendingClear.resolve({
      data: {
        list: [
          { id: 'a-1', productID: 'product-a' },
          remainingEntry,
        ],
      },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual(['a-1', 'a-2'])
    expect(wrapper.vm.selectedProducts.products.get('product-a')).toBe(2)
    expect(wrapper.vm.selectedProducts.totalNum).toBe(2)
  })

  it('普通查询表头取勾只移除当前查询结果', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockResolvedValueOnce({
      data: { list: [{ id: 'matched', productID: 'product-id-1' }] },
    })
    wrapper.vm.selectEntry = [
      { id: 'matched', productID: 'product-id-1' },
      { id: 'other-query', productID: 'product-id-1' },
      { id: 'other-product', productID: 'product-id-2' },
    ]

    wrapper.vm.onSelectAll(false, [], [])
    await flushPromises()

    expect(getEntryByClassfyMock).toHaveBeenCalledWith(
      expect.objectContaining({ pageIndex: -1, pageSize: -1 }),
      expect.any(Object),
    )
    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual([
      'other-query',
      'other-product',
    ])
  })

  it('冗余校验表头取勾只移除当前完整结果集', async () => {
    wrapper = mountProductEntry()
    wrapper.vm.lastSearchType = 'checkNotUseEntry'
    wrapper.vm.dataSource = [
      { id: 'redundant-current', productID: 'product-id-1' },
    ]
    wrapper.vm.selectEntry = [
      { id: 'redundant-current', productID: 'product-id-1' },
      { id: 'other-query', productID: 'product-id-1' },
      { id: 'other-product', productID: 'product-id-2' },
    ]

    wrapper.vm.onSelectAll(false, [], wrapper.vm.dataSource)
    await nextTick()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual([
      'other-query',
      'other-product',
    ])
  })

  it('全局取消清空跨产品清单并使未完成全选请求失效', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    const pendingRequest = createDeferred()
    getEntryByClassfyMock.mockReturnValueOnce(pendingRequest.promise)
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品一' }
    wrapper.vm.selectEntry = [
      { id: 'existing', productID: 'product-2' },
    ]
    wrapper.vm.selectedProducts = {
      products: new Map([['product-2', 1]]),
      totalNum: 1,
    }
    wrapper.vm.dataSource = [{ id: 'pending-page-entry', productID: 'product-1' }]

    wrapper.vm.selectAllEntry()
    expect(wrapper.vm.tableSelectedRowKeys).toContain('pending-page-entry')
    wrapper.vm.cancelSelection()
    expect(wrapper.vm.tableSelectedRowKeys).not.toContain('pending-page-entry')
    pendingRequest.resolve({
      data: { list: [{ id: 'late', productID: 'product-1' }] },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry).toEqual([])
    expect(wrapper.vm.selectedRows).toEqual([])
    expect(wrapper.vm.selectedRowKeys).toEqual([])
    expect(wrapper.vm.selectedProducts.products.size).toBe(0)
    expect(wrapper.vm.selectAllLoading).toBe(false)
  })

  it('全选请求期间翻页后立即勾选新页，并在结果返回后同步全局清单', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const pageOneEntry = { id: 'page-one', productID: 'product-a' }
    const pageTwoEntry = { id: 'page-two', productID: 'product-a' }
    const pendingSelection = createDeferred()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingSelection.promise)
    wrapper.vm.product = { type: 'product', key: 'product-a', title: '产品 A' }
    wrapper.vm.dataSource = [pageOneEntry]

    wrapper.vm.onSelectAll(true, [], [pageOneEntry])
    await nextTick()

    expect(wrapper.vm.selectAllLoading).toBe(true)
    expect(wrapper.vm.loading).toBe(false)
    expect(wrapper.vm.tableSelectedRowKeys).toContain(pageOneEntry.id)

    wrapper.vm.pageChange(2, 20, null)
    wrapper.vm.dataSource = [pageTwoEntry]
    await nextTick()

    expect(wrapper.vm.pagination.current).toBe(2)
    expect(wrapper.vm.tableSelectedRowKeys).toEqual(
      expect.arrayContaining([pageOneEntry.id, pageTwoEntry.id]),
    )
    expect(wrapper.vm.selectedRowKeys).not.toContain(pageTwoEntry.id)

    pendingSelection.resolve({ data: { list: [pageOneEntry, pageTwoEntry] } })
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual(
      expect.arrayContaining([pageOneEntry.id, pageTwoEntry.id]),
    )
    expect(wrapper.vm.tableSelectedRowKeys).toContain(pageTwoEntry.id)
    expect(wrapper.vm.selectAllLoading).toBe(false)
  })

  it('待全选投影只作用于原查询范围', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const pendingSelection = createDeferred()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingSelection.promise)
    wrapper.vm.product = { type: 'product', key: 'product-a', title: '产品 A' }
    wrapper.vm.selectAllEntry()
    wrapper.vm.dataSource = [{ id: 'next-page-entry', productID: 'product-a' }]

    expect(wrapper.vm.tableSelectedRowKeys).toContain('next-page-entry')

    wrapper.vm.search.entry = '另一个查询条件'
    await nextTick()

    expect(wrapper.vm.tableSelectedRowKeys).not.toContain('next-page-entry')

    pendingSelection.resolve({ data: { list: [] } })
    await flushPromises()
  })

  it('待全选投影在请求失败后消失并保留原全局清单', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const originalEntry = { id: 'already-selected', productID: 'product-b' }
    const pendingSelection = createDeferred()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingSelection.promise)
    wrapper.vm.product = { type: 'product', key: 'product-a', title: '产品 A' }
    wrapper.vm.selectEntry = [originalEntry]
    wrapper.vm.dataSource = [{ id: 'temporary-entry', productID: 'product-a' }]

    wrapper.vm.selectAllEntry()

    expect(wrapper.vm.tableSelectedRowKeys).toContain('temporary-entry')
    expect(wrapper.vm.selectAllLoading).toBe(true)

    pendingSelection.reject(new Error('全选失败'))
    await flushPromises()

    expect(wrapper.vm.tableSelectedRowKeys).not.toContain('temporary-entry')
    expect(wrapper.vm.selectEntry).toEqual([originalEntry])
    expect(wrapper.vm.selectAllLoading).toBe(false)
    expect(message.error).toHaveBeenCalledWith('全选失败')
  })

  it('全选和取勾交叠时按请求及单行操作顺序投影当前页勾选', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const row = { id: 'overlap-entry', productID: 'product-a' }
    const pendingClear = createDeferred()
    const pendingSelection = createDeferred()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock
      .mockReturnValueOnce(pendingClear.promise)
      .mockReturnValueOnce(pendingSelection.promise)
    wrapper.vm.product = { type: 'product', key: 'product-a', title: '产品 A' }
    wrapper.vm.dataSource = [row]
    wrapper.vm.selectEntry = [row]

    wrapper.vm.clearCurrentQuerySelection()
    expect(wrapper.vm.tableSelectedRowKeys).not.toContain(row.id)

    wrapper.vm.selectAllEntry()
    expect(wrapper.vm.tableSelectedRowKeys).toContain(row.id)

    wrapper.vm.onSelect(row, false)
    expect(wrapper.vm.tableSelectedRowKeys).not.toContain(row.id)

    wrapper.vm.onSelect(row, true)
    expect(wrapper.vm.tableSelectedRowKeys).toContain(row.id)

    pendingSelection.resolve({ data: { list: [row] } })
    await flushPromises()
    expect(wrapper.vm.tableSelectedRowKeys).toContain(row.id)

    pendingClear.resolve({ data: { list: [row] } })
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toContain(row.id)
    expect(wrapper.vm.tableSelectedRowKeys).toContain(row.id)
  })

  it('产品切换并返回后，旧取勾响应失效且保留已提交清单', async () => {
    wrapper = mountProductEntry(undefined, {
      stubs: {
        DataBox: {
          template:
            '<div><slot name="operate" /><slot name="data" /></div>',
        },
      },
    })
    await flushPromises()
    const productA = { type: 'product', key: 'product-a', title: '产品 A' }
    const productB = { type: 'product', key: 'product-b', title: '产品 B' }
    const pendingClear = createDeferred()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingClear.promise)
    wrapper.vm.product = productA
    wrapper.vm.dataSource = [{ id: 'shared-id' }, { id: 'not-selected' }]
    wrapper.vm.selectEntry = [
      { id: 'shared-id', productID: 'product-a' },
      { id: 'next-page-id', productID: 'product-a' },
      { id: 'other-product-id', productID: 'product-b' },
    ]
    wrapper.vm.syncSelectedProducts()

    const pendingClearScope = wrapper.vm.getCurrentSelectionScopeKey()
    wrapper.vm.clearCurrentQuerySelection()
    await nextTick()

    expect(wrapper.vm.loading).toBe(false)
    expect(wrapper.vm.selectAllLoading).toBe(true)
    expect(wrapper.vm.selectionOperations.pendingCount).toBe(1)
    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual([
      'shared-id',
      'next-page-id',
      'other-product-id',
    ])
    expect(wrapper.vm.tableSelectedRowKeys).toEqual([
      'next-page-id',
      'other-product-id',
    ])

    wrapper.vm.dataSource = [{ id: 'next-page-id' }]
    await nextTick()
    expect(wrapper.vm.tableSelectedRowKeys).toEqual([
      'shared-id',
      'other-product-id',
    ])
    expect(
      wrapper.vm.tableSelectedRowKeys.filter((id) =>
        wrapper.vm.dataSource.some((entry) => entry.id === id),
      ),
    ).toEqual([])

    wrapper.vm.product = productB
    wrapper.vm.dataSource = [
      { id: 'shared-id' },
      { id: 'other-product-id' },
    ]
    await nextTick()
    expect(wrapper.vm.product.key).toBe('product-b')
    expect(wrapper.vm.getCurrentSelectionScopeKey()).not.toBe(pendingClearScope)
    expect(wrapper.vm.selectionOperations.pendingCount).toBe(0)
    expect(wrapper.vm.selectAllLoading).toBe(false)
    expect(wrapper.vm.tableSelectedRowKeys).toContain('shared-id')
    expect(wrapper.vm.tableSelectedRowKeys).toContain('other-product-id')

    wrapper.vm.product = productA
    wrapper.vm.dataSource = [{ id: 'shared-id' }]
    await nextTick()
    expect(
      {
        scopeMatches:
          wrapper.vm.getCurrentSelectionScopeKey() === pendingClearScope,
        visibleIds: wrapper.vm.dataSource.map((entry) => entry.id),
        projectedSelected: wrapper.vm.selectionOperations.project(
          'shared-id',
          pendingClearScope,
          true,
        ),
        selectedIds: wrapper.vm.selectEntry.map((entry) => entry.id),
      },
    ).toEqual({
      scopeMatches: true,
      visibleIds: ['shared-id'],
      projectedSelected: true,
      selectedIds: ['shared-id', 'next-page-id', 'other-product-id'],
    })
    expect(wrapper.vm.tableSelectedRowKeys).toContain('shared-id')

    pendingClear.resolve({
      data: {
        list: [
          { id: 'shared-id', productID: 'product-a' },
          { id: 'next-page-id', productID: 'product-a' },
        ],
      },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual([
      'shared-id',
      'next-page-id',
      'other-product-id',
    ])
    expect(wrapper.vm.selectedProducts.totalNum).toBe(3)
    expect(wrapper.vm.selectAllLoading).toBe(false)
    expect(wrapper.vm.loading).toBe(false)
  })

  it('查询条件变化后旧全选响应失效，即使随后返回原条件也不提交', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const pendingSelection = createDeferred()
    const committedEntry = { id: 'already-selected', productID: 'product-b' }
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingSelection.promise)
    wrapper.vm.product = { type: 'product', key: 'product-a', title: '产品 A' }
    wrapper.vm.selectEntry = [committedEntry]
    wrapper.vm.syncSelectedProducts()
    wrapper.vm.search.entry = '原条件'

    wrapper.vm.selectAllEntry()
    expect(wrapper.vm.selectAllLoading).toBe(true)

    wrapper.vm.search.entry = '新条件'
    expect(wrapper.vm.selectionOperations.pendingCount).toBe(0)
    expect(wrapper.vm.selectAllLoading).toBe(false)
    wrapper.vm.search.entry = '原条件'

    pendingSelection.resolve({
      data: { list: [{ id: 'stale-entry', productID: 'product-a' }] },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry).toEqual([committedEntry])
    expect(wrapper.vm.selectedProducts.totalNum).toBe(1)
    expect(message.error).not.toHaveBeenCalled()
  })

  it('工具栏显示取消选择按钮，空列表时禁用，有选择时清空全局清单', async () => {
    wrapper = mountProductEntry(undefined, {
      stubs: {
        DataBox: {
          template: '<div><slot name="operate" /><slot name="data" /></div>',
        },
        'a-button': {
          props: ['disabled'],
          template: '<button :disabled="disabled"><slot /></button>',
        },
        'a-badge': {
          template: '<div><slot /></div>',
        },
      },
    })
    await nextTick()
    const cancelButton = () =>
      wrapper.findAll('button').find((button) => button.text() === '取消选择')

    expect(cancelButton()).toBeDefined()
    expect(cancelButton().element.disabled).toBe(true)

    wrapper.vm.selectEntry = [{ id: 'selected', productID: 'product-id-1' }]
    await nextTick()
    expect(cancelButton().element.disabled).toBe(false)
    await cancelButton().trigger('click')

    expect(wrapper.vm.selectEntry).toEqual([])
  })

  /**
   * 验证工具栏按钮层级和全选 loading 的展示状态。
   * @param {void} none - 无参数。
   * @returns {Promise<void>} 页面状态断言完成后结束。
   */
  it('全选 loading 在已选词条按钮内显示 spinner 且保持文案占位', async () => {
    const storeMock = createUserStoreMock()
    storeMock.$currentDepartment = { ops: new Set(['needIP', 'fileUpdate']) }
    wrapper = mountProductEntry(storeMock, {
      stubs: {
        DataBox: {
          template: '<div><slot name="operate" /></div>',
        },
        'a-button': {
          props: ['type', 'disabled'],
          template: '<button type="button" :data-button-type="type" :disabled="disabled" v-bind="$attrs"><slot name="icon" /><slot /></button>',
        },
        BackFillModal_v3: {
          props: {
            buttonType: { type: String, default: 'primary' },
            buttonTitle: { type: String, default: '导入' },
          },
          template: '<button :data-button-type="buttonType">{{ buttonTitle }}</button>',
        },
        ColumnFilter: {
          props: {
            buttonType: { type: String, default: 'primary' },
            buttonText: { type: String, default: '展示列' },
          },
          template: '<button class="column-filter-button" :data-button-type="buttonType">{{ buttonText }}</button>',
        },
        GitCommitButton: {
          props: ['buttonClass', 'buttonTitle'],
          template: '<button :class="buttonClass">{{ buttonTitle }}</button>',
        },
        'a-badge': {
          props: ['count'],
          template: '<div><span v-if="count" class="badge-count">{{ count }}</span><slot /></div>',
        },
      },
    })
    await flushPromises()
    wrapper.vm.admin = true
    await nextTick()

    wrapper.vm.selectEntry = [{ id: 'selected-entry' }]
    await nextTick()

    /**
     * 查找已选词条按钮。
     * @param {void} none - 无参数。
     * @returns {any} 已选词条按钮包装器。
     */
    const selectedButton = () =>
      wrapper
        .findAll('.select-toolbar-button')
        .find((button) => button.text().includes('已选词条'))
    /**
     * 按文案查找工具栏按钮。
     * @param {string} text - 按钮文案。
     * @returns {any} 匹配的按钮包装器。
     */
    const toolbarButton = (text) =>
      wrapper
        .findAll('.entry-toolbar-right button')
        .find((button) => button.text().trim() === text)

    expect(selectedButton().element.disabled).toBe(false)
    expect(selectedButton().attributes('data-button-type')).toBe('primary')
    expect(selectedButton().classes()).toContain('resetBtn')
    expect(toolbarButton('取消选择').attributes('data-button-type')).toBe('default')
    expect(toolbarButton('新增').attributes('data-button-type')).toBe('primary')
    expect(toolbarButton('二级分类管理').attributes('data-button-type')).toBe('primary')
    expect(toolbarButton('git推送').classes()).toContain('yellowBtn')
    expect(toolbarButton('更新翻译').attributes('data-button-type')).toBe('primary')
    expect(
      wrapper
        .findAll('.column-filter-button')
        .find((button) => button.text() === '展示列')
        .attributes('data-button-type')
    ).toBe('text')
    expect(wrapper.find('.badge-count').text()).toBe('1')
    const selectedLabel = selectedButton().find('.select-toolbar-button-content').text()
    expect(selectedLabel).toBeTruthy()

    const loadingOperation = wrapper.vm.selectionOperations.begin(
      'toolbar-loading',
      true,
    )
    await nextTick()

    expect(selectedButton().element.disabled).toBe(true)
    expect(wrapper.find('.select-toolbar-button-spinner').exists()).toBe(true)
    expect(
      wrapper
        .find('.select-toolbar-button-content-hidden')
        .exists()
    ).toBe(true)
    expect(wrapper.find('.badge-count').exists()).toBe(false)

    wrapper.vm.selectionOperations.resolve(
      loadingOperation,
      [],
      wrapper.vm.selectEntry,
      'toolbar-loading',
    )
    await nextTick()

    expect(selectedButton().element.disabled).toBe(false)
    expect(wrapper.find('.select-toolbar-button-spinner').exists()).toBe(false)
    expect(selectedButton().find('.select-toolbar-button-content').text()).toBe(selectedLabel)
    expect(wrapper.find('.badge-count').text()).toBe('1')
  })

  it('当前查询取勾请求失败时保留原已选词条', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockRejectedValueOnce(new Error('取勾失败'))
    wrapper.vm.selectEntry = [
      { id: 'selected', productID: 'product-id-1' },
      { id: 'other-product', productID: 'product-id-2' },
    ]

    wrapper.vm.onSelectAll(false, [], [])
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual([
      'selected',
      'other-product',
    ])
    expect(message.error).toHaveBeenCalledWith('取勾失败')
  })

  it('待取勾期间单行重新勾选会覆盖待处理取勾', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    const pendingClear = createDeferred()
    const row = { id: 'reselected-entry', productID: 'product-id-1' }
    getEntryByClassfyMock.mockClear()
    getEntryByClassfyMock.mockReturnValueOnce(pendingClear.promise)
    wrapper.vm.dataSource = [row]
    wrapper.vm.selectEntry = [row, { id: 'removed-entry', productID: 'product-id-1' }]

    wrapper.vm.clearCurrentQuerySelection()
    expect(wrapper.vm.tableSelectedRowKeys).not.toContain(row.id)

    wrapper.vm.onSelect(row, true)
    expect(
      wrapper.vm.selectionOperations.project(
        row.id,
        wrapper.vm.getCurrentSelectionScopeKey(),
        false,
      ),
    ).toBe(true)
    expect(wrapper.vm.tableSelectedRowKeys).toContain(row.id)

    pendingClear.resolve({
      data: {
        list: [row, { id: 'removed-entry', productID: 'product-id-1' }],
      },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual([row.id])
    expect(wrapper.vm.selectedProducts.totalNum).toBe(1)
    expect(wrapper.vm.tableSelectedRowKeys).toContain(row.id)
  })

  it('取勾响应先返回时，较早的全选响应不得重新选择当前查询词条', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    const olderSelection = createDeferred()
    const queryClear = createDeferred()
    getEntryByClassfyMock
      .mockReturnValueOnce(olderSelection.promise)
      .mockReturnValueOnce(queryClear.promise)
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品一' }

    wrapper.vm.selectAllEntry()
    wrapper.vm.onSelectAll(false, [], [])
    expect(getEntryByClassfyMock).toHaveBeenCalledTimes(2)

    queryClear.resolve({
      data: { list: [{ id: 'query-entry', productID: 'product-1' }] },
    })
    await flushPromises()
    olderSelection.resolve({
      data: { list: [{ id: 'query-entry', productID: 'product-1' }] },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).not.toContain(
      'query-entry',
    )
  })

  it('取勾期间较新的全选优先于迟到的取勾响应', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    const queryClear = createDeferred()
    const newerSelection = createDeferred()
    getEntryByClassfyMock
      .mockReturnValueOnce(queryClear.promise)
      .mockReturnValueOnce(newerSelection.promise)
    wrapper.vm.product = { type: 'product', key: 'product-1', title: '产品一' }

    wrapper.vm.onSelectAll(false, [], [])
    wrapper.vm.selectAllEntry()
    newerSelection.resolve({
      data: { list: [{ id: 'query-entry', productID: 'product-1' }] },
    })
    await flushPromises()
    queryClear.resolve({
      data: { list: [{ id: 'query-entry', productID: 'product-1' }] },
    })
    await flushPromises()

    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toContain(
      'query-entry',
    )
  })

  it('分类取勾与子产品全选交错时保留较新的子产品选择，响应顺序不影响结果', async () => {
    for (const clearFirst of [true, false]) {
      wrapper = mountProductEntry()
      await flushPromises()
      getEntryByClassfyMock.mockClear()
      const categoryClear = createDeferred()
      const productSelect = createDeferred()
      getEntryByClassfyMock
        .mockReturnValueOnce(categoryClear.promise)
        .mockReturnValueOnce(productSelect.promise)
      wrapper.vm.product = {
        type: 'classify',
        key: 'category-a',
        title: '分类 A',
      }
      wrapper.vm.dataSource = [
        { id: 'shared-id', productID: 'product-b' },
        { id: 'category-only', productID: 'product-a' },
      ]
      wrapper.vm.selectEntry = [
        { id: 'shared-id', productID: 'product-b' },
        { id: 'category-only', productID: 'product-a' },
        { id: 'sibling-kept', productID: 'product-c' },
      ]
      wrapper.vm.syncSelectedProducts()

      wrapper.vm.clearCurrentQuerySelection()
      wrapper.vm.product = { type: 'product', key: 'product-b', title: '产品 B' }
      wrapper.vm.dataSource = [
        { id: 'shared-id', productID: 'product-b' },
        { id: 'product-only', productID: 'product-b' },
      ]
      wrapper.vm.onSelectAll(true, [], wrapper.vm.dataSource)

      const resolveClear = () =>
        categoryClear.resolve({
          data: {
            list: [
              { id: 'shared-id', productID: 'product-b' },
              { id: 'category-only', productID: 'product-a' },
            ],
          },
        })
      const resolveSelect = () =>
        productSelect.resolve({
          data: {
            list: [
              { id: 'shared-id', productID: 'product-b', entry: '产品 B 最新值' },
              { id: 'product-only', productID: 'product-b' },
            ],
          },
        })

      if (clearFirst) {
        resolveClear()
        await flushPromises()
        resolveSelect()
      } else {
        resolveSelect()
        await flushPromises()
        resolveClear()
      }
      await flushPromises()

      expect(wrapper.vm.selectEntry.map((entry) => entry.id).sort()).toEqual([
        'category-only',
        'product-only',
        'shared-id',
        'sibling-kept',
      ])
      expect(
        wrapper.vm.selectEntry.filter((entry) => entry.id === 'shared-id'),
      ).toHaveLength(1)
      expect(
        wrapper.vm.selectEntry.find((entry) => entry.id === 'shared-id').entry,
      ).toBe('产品 B 最新值')
      wrapper.unmount()
      wrapper = null
    }
  })

  it('子产品取勾与分类全选交错时以较新的子产品取勾为准，响应顺序不影响结果', async () => {
    for (const clearFirst of [true, false]) {
      wrapper = mountProductEntry()
      await flushPromises()
      getEntryByClassfyMock.mockClear()
      const productClear = createDeferred()
      const categorySelect = createDeferred()
      getEntryByClassfyMock
        .mockReturnValueOnce(categorySelect.promise)
        .mockReturnValueOnce(productClear.promise)
      wrapper.vm.product = { type: 'classify', key: 'category-a', title: '分类 A' }
      wrapper.vm.dataSource = [
        { id: 'shared-id', productID: 'product-b' },
        { id: 'category-only', productID: 'product-a' },
      ]
      wrapper.vm.onSelectAll(true, [], wrapper.vm.dataSource)

      wrapper.vm.product = { type: 'product', key: 'product-b', title: '产品 B' }
      wrapper.vm.dataSource = [
        { id: 'shared-id', productID: 'product-b' },
        { id: 'product-only', productID: 'product-b' },
      ]
      wrapper.vm.clearCurrentQuerySelection()

      const resolveClear = () =>
        productClear.resolve({
          data: {
            list: [
              { id: 'shared-id', productID: 'product-b' },
              { id: 'product-only', productID: 'product-b' },
            ],
          },
        })
      const resolveSelect = () =>
        categorySelect.resolve({
          data: {
            list: [
              { id: 'shared-id', productID: 'product-b' },
              { id: 'category-only', productID: 'product-a' },
            ],
          },
        })

      if (clearFirst) {
        resolveClear()
        await flushPromises()
        resolveSelect()
      } else {
        resolveSelect()
        await flushPromises()
        resolveClear()
      }
      await flushPromises()

      expect(wrapper.vm.selectEntry.map((entry) => entry.id).sort()).toEqual([
        'category-only',
      ])
      expect(wrapper.vm.selectedProducts.totalNum).toBe(1)
      wrapper.unmount()
      wrapper = null
    }
  })

  it('兄弟产品全选与取勾并发时保留各自范围内的最终操作和非交集选择', async () => {
    for (const clearFirst of [true, false]) {
      wrapper = mountProductEntry()
      await flushPromises()
      getEntryByClassfyMock.mockClear()
      const productASelect = createDeferred()
      const productBClear = createDeferred()
      getEntryByClassfyMock
        .mockReturnValueOnce(productASelect.promise)
        .mockReturnValueOnce(productBClear.promise)
      wrapper.vm.selectEntry = [
        { id: 'product-b-selected', productID: 'product-b' },
        { id: 'other-kept', productID: 'product-c' },
      ]
      wrapper.vm.product = { type: 'product', key: 'product-a', title: '产品 A' }
      wrapper.vm.dataSource = [{ id: 'product-a-new', productID: 'product-a' }]
      wrapper.vm.onSelectAll(true, [], wrapper.vm.dataSource)

      wrapper.vm.product = { type: 'product', key: 'product-b', title: '产品 B' }
      wrapper.vm.dataSource = [{ id: 'product-b-selected', productID: 'product-b' }]
      wrapper.vm.clearCurrentQuerySelection()

      const resolveClear = () =>
        productBClear.resolve({
          data: { list: [{ id: 'product-b-selected', productID: 'product-b' }] },
        })
      const resolveSelect = () =>
        productASelect.resolve({
          data: { list: [{ id: 'product-a-new', productID: 'product-a' }] },
        })

      if (clearFirst) {
        resolveClear()
        await flushPromises()
        resolveSelect()
      } else {
        resolveSelect()
        await flushPromises()
        resolveClear()
      }
      await flushPromises()

      expect(wrapper.vm.selectEntry.map((entry) => entry.id).sort()).toEqual([
        'other-kept',
        'product-a-new',
      ])
      expect(wrapper.vm.selectedProducts.products.get('product-a')).toBe(1)
      expect(wrapper.vm.selectedProducts.products.has('product-b')).toBe(false)
      wrapper.unmount()
      wrapper = null
    }
  })

  it('tracks full-selection requests through the shared selection operations hook', async () => {
    wrapper = mountProductEntry()
    await flushPromises()
    getEntryByClassfyMock.mockClear()
    const pending = createDeferred()
    getEntryByClassfyMock.mockReturnValueOnce(pending.promise)
    wrapper.vm.product = { type: 'product', key: 'product-a', title: 'Product A' }

    wrapper.vm.selectAllEntry()

    expect(wrapper.vm.selectionOperations.pendingCount).toBe(1)

    pending.resolve({ data: { list: [{ id: 'selected', productID: 'product-a' }] } })
    await flushPromises()

    expect(wrapper.vm.selectionOperations.pendingCount).toBe(0)
    expect(wrapper.vm.selectEntry.map((entry) => entry.id)).toEqual(['selected'])
  })
})
