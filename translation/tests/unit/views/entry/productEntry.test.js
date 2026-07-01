/**
 * ProductEntry 组件测试
 */

import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ProductEntry from '@/views/entry/productEntry.vue'
import ColumnFilter from '@/components/ColumnFilter/ColumnFilter.vue'
import { entryAllCols, entryPresets } from '@/constants/commonParam.js'
import { resolvePresetCols } from '@/components/ColumnFilter/colPreset.js'
import { setTableHeight as setTableHeightMock } from '@/utils/tableUtils'

const applyTableMock = vi.hoisted(() => vi.fn())
const changeColumnMock = vi.hoisted(() => vi.fn())
const getEntryByClassfyMock = vi.hoisted(() =>
  vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } }))
)

import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'
import { message } from 'ant-design-vue'

vi.mock('@/http/api/entryManage', () => ({
  searchEntryInfo: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  getEntryByClassfy: getEntryByClassfyMock,
  getEntrySourcesByClassify: vi.fn(() => Promise.resolve({ data: [] })),
  getWriteFileNamesByClassify: vi.fn(() => Promise.resolve({ data: [] })),
  deleteEntryInfo: vi.fn(),
  updatePublicEntry: vi.fn(),
  addSingleEntry: vi.fn(),
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
  Modal: {},
  notification: {},
}))

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
        EditReason: true,
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
