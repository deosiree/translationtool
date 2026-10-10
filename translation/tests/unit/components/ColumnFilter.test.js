import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ColumnFilter from '@/components/ColumnFilter/ColumnFilter.vue'

const mockColumns = [
  { label: '序号', value: 'index', index: 0, required: true },
  { label: '词条', value: 'entry', index: 2, required: true },
  { label: 'tag', value: 'tag', index: 3, visible: true },
  { label: 'comment', value: 'comment', index: 4, visible: false },
  { label: '操作', value: 'operation', index: 100, required: true },
]

describe('ColumnFilter', () => {
  const globalStubs = {
    'a-popover': {
      template: '<div><slot name="content" /></div>',
    },
    'a-checkbox-group': {
      template: '<div><slot /></div>',
    },
    'a-row': true,
    'a-col': true,
    'a-checkbox': true,
    'a-button': {
      template: '<button @click="$attrs.onClick"><slot name="icon" /><slot /></button>',
    },
    SettingOutlined: true,
  }

  it('应渲染列设置头部文案', () => {
    const wrapper = mount(ColumnFilter, {
      props: {
        modelValue: ['index', 'entry', 'tag', 'operation'],
        columns: mockColumns,
      },
      global: { stubs: globalStubs },
    })

    expect(wrapper.text()).toContain('展示列')
    expect(wrapper.text()).toContain('重置')
  })

  it('勾选变化应 emit 合并必选列后的 change', async () => {
    const wrapper = mount(ColumnFilter, {
      props: {
        modelValue: ['index', 'entry', 'operation'],
        columns: mockColumns,
      },
      global: { stubs: globalStubs },
    })

    wrapper.vm.onChange(['tag'])

    expect(wrapper.emitted('change')?.[0]?.[0]).toEqual([
      'index',
      'entry',
      'tag',
      'operation',
    ])
  })

  it('点击重置应 emit 默认列 selection', async () => {
    const wrapper = mount(ColumnFilter, {
      props: {
        modelValue: ['index', 'entry'],
        columns: mockColumns,
      },
      global: { stubs: globalStubs },
    })

    const resetBtn = wrapper.findAll('button').find((b) => b.text() === '重置')
    await resetBtn.trigger('click')

    expect(wrapper.emitted('change')?.[0]?.[0]).toEqual([
      'index',
      'entry',
      'tag',
      'operation',
    ])
  })
})
