/**
 * ProductEntry 组件测试
 */

import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ProductEntry from '@/views/entry/productEntry.vue'
import { entryAllCols, entryPresets } from '@/constants/commonParam.js'
import { resolvePresetCols } from '@/components/ColumnFilter/colPreset.js'

const applyTableMock = vi.hoisted(() => vi.fn())
const changeColumnMock = vi.hoisted(() => vi.fn())

import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

vi.mock('@/http/api/entryManage', () => ({
  searchEntryInfo: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  getEntryByClassfy: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
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

function mountProductEntry(storeMock = createUserStoreMock()) {
  return mount(ProductEntry, {
    props: {
      currentProduct: testProduct,
      boxHeight: 600,
      productEdit: false,
    },
    global: {
      mocks: storeMock,
      stubs: {
        SearchBox: true,
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

  it('changeColumn 应传入 columnSettingsList', () => {
    wrapper = mountProductEntry()
    applyTableMock.mockClear()
    wrapper.vm.columnSettingsList = resolvePresetCols(
      entryPresets.productEntry,
      entryAllCols
    )
    wrapper.vm.changeColumn(['index', 'entry', 'operation'])
    expect(changeColumnMock).toHaveBeenCalledWith(
      'colPref-productEntry',
      200,
      ['index', 'entry', 'operation'],
      wrapper.vm,
      false,
      wrapper.vm.columnSettingsList
    )
  })

  it('applyTable 后 columnSettingsList 应包含必选列定义', () => {
    const requiredValues = resolvePresetCols(entryPresets.productEntry, entryAllCols)
      .filter((item) => item.required)
      .map((item) => item.value)
    expect(requiredValues).toEqual(['index', 'entry', 'operation'])
  })
})
