import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ColumnFilter from '@/components/ColumnFilter/ColumnFilter.vue'

const columnSettingsList = [
  { label: '序号', value: 'index', index: 0, required: true },
  { label: '词条', value: 'entry', index: 2, required: true },
  { label: 'tag', value: 'tag', index: 3, visible: true },
  { label: '操作', value: 'operation', index: 100, required: true },
]

const searchConditionList = [
  { label: '词条', value: 'entry', index: 1, visible: true },
  { label: 'tag', value: 'tag', index: 3, visible: false },
  { label: '翻译语种', value: 'language', index: 7, visible: true },
  { label: '翻译结果', value: 'translate', index: 9, visible: true },
]

const mountStubs = {
  global: {
    stubs: {
      'a-popover': { template: '<div><slot /></div>' },
      'a-checkbox-group': true,
      'a-row': true,
      'a-col': true,
      'a-checkbox': true,
      'a-button': true,
      SettingOutlined: true,
    },
  },
}

describe('ColumnFilter 持久化', () => {
  const colPrefName = 'test-column-filter-persist'

  beforeEach(() => {
    localStorage.removeItem(colPrefName)
  })

  afterEach(() => {
    localStorage.removeItem(colPrefName)
  })

  it('colPrefName 有值时应写入 localStorage 并 emit update:modelValue', async () => {
    const host = {
      columnSettingsList,
      columns: [
        { dataIndex: 'index', colValue: 'index', index: 0 },
        { dataIndex: 'entry', colValue: 'entry', index: 2 },
        { dataIndex: 'tag', colValue: 'tag', index: 3 },
        { dataIndex: 'operation', colValue: 'operation', index: 100 },
      ],
      checkedColumn: ['index', 'entry', 'tag', 'operation'],
      colBuildCtx: { pagination: { pageSize: 20, current: 1 } },
    }

    const wrapper = mount(ColumnFilter, {
      props: {
        modelValue: ['index', 'entry', 'tag', 'operation'],
        columns: columnSettingsList,
        colPrefName,
        normalWidth: 120,
        needFilter: false,
        tableHost: host,
      },
      ...mountStubs,
    })

    await wrapper.vm.onChange(['tag'])

    expect(wrapper.emitted('update:modelValue')?.[0]?.[0]).toEqual([
      'index',
      'entry',
      'tag',
      'operation',
    ])
    expect(localStorage.getItem(colPrefName)).toBe(
      JSON.stringify({ displayColumn: 'tag' })
    )
    expect(host.checkedColumn).toEqual(['index', 'entry', 'tag', 'operation'])
  })

  it('selectionOnly 模式无 tableHost 时应写入 localStorage 并 emit', async () => {
    const wrapper = mount(ColumnFilter, {
      props: {
        modelValue: ['entry', 'language', 'translate'],
        columns: searchConditionList,
        colPrefName,
        persistMode: 'selectionOnly',
        title: '展示条件',
        buttonText: '展示条件',
        ghost: true,
      },
      ...mountStubs,
    })

    await wrapper.vm.onChange(['entry', 'language', 'translate', 'tag'])

    expect(wrapper.emitted('update:modelValue')?.[0]?.[0]).toEqual([
      'entry',
      'tag',
      'language',
      'translate',
    ])
    expect(localStorage.getItem(colPrefName)).toBe(
      JSON.stringify({ displayColumn: 'entry,tag,language,translate' })
    )
  })

  it('selectionOnly 重置应回到 visible 默认勾选', async () => {
    const wrapper = mount(ColumnFilter, {
      props: {
        modelValue: ['entry', 'language', 'translate', 'tag'],
        columns: searchConditionList,
        colPrefName,
        persistMode: 'selectionOnly',
      },
      ...mountStubs,
    })

    await wrapper.vm.handleReset()

    expect(wrapper.emitted('update:modelValue')?.[0]?.[0]).toEqual([
      'entry',
      'language',
      'translate',
    ])
    expect(localStorage.getItem(colPrefName)).toBe(
      JSON.stringify({ displayColumn: 'entry,language,translate' })
    )
  })

  it('应支持 title、buttonText、ghost props', () => {
    const wrapper = mount(ColumnFilter, {
      props: {
        modelValue: [],
        columns: searchConditionList,
        title: '展示条件',
        buttonText: '展示条件',
        ghost: true,
      },
      ...mountStubs,
    })

    expect(wrapper.props('title')).toBe('展示条件')
    expect(wrapper.props('buttonText')).toBe('展示条件')
    expect(wrapper.props('ghost')).toBe(true)
    expect(wrapper.vm.visibleColumns).toEqual(searchConditionList)
  })

  it('visibleColumns 应过滤 hidden 列', () => {
    const wrapper = mount(ColumnFilter, {
      props: {
        modelValue: [],
        columns: [
          { label: '词条', value: 'entry', index: 1, visible: true },
          { label: '隐藏项', value: 'hidden', index: 2, hidden: true },
        ],
      },
      ...mountStubs,
    })

    expect(wrapper.vm.visibleColumns.map((c) => c.value)).toEqual(['entry'])
  })
})
