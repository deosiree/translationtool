import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ColumnFilter from '@/components/ColumnFilter/ColumnFilter.vue'

const columnSettingsList = [
  { label: '序号', value: 'index', index: 0, required: true },
  { label: '词条', value: 'entry', index: 2, required: true },
  { label: 'tag', value: 'tag', index: 3, visible: true },
  { label: '操作', value: 'operation', index: 100, required: true },
]

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
})
