import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WorkbenchColumnActions from '@/components/Workbench/WorkbenchColumnActions.vue'

const columns = [
  { label: '词条', value: 'entry', index: 2, required: true },
  { label: 'tag', value: 'tag', index: 3, visible: true },
]

describe('WorkbenchColumnActions', () => {
  it('默认仅渲染 ColumnFilter', () => {
    const wrapper = mount(WorkbenchColumnActions, {
      props: {
        modelValue: ['entry', 'tag'],
        columns,
        colPrefName: 'colPref-test',
      },
      global: {
        stubs: {
          ColumnFilter: {
            template: '<div class="column-filter-stub" />',
            props: ['modelValue', 'colPrefName', 'needFilter'],
          },
          CoverButton: true,
        },
      },
    })

    expect(wrapper.find('.column-filter-stub').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'CoverButton' }).exists()).toBe(false)
  })

  it('showCoverButton 为 true 时应渲染 CoverButton', () => {
    const wrapper = mount(WorkbenchColumnActions, {
      props: {
        modelValue: ['entry'],
        columns,
        colPrefName: 'colPref-test',
        showCoverButton: true,
        coverButtonProps: { translate: '英文' },
      },
      global: {
        stubs: {
          ColumnFilter: { template: '<div />' },
          CoverButton: { template: '<button class="cover-btn-stub" />' },
        },
      },
    })

    expect(wrapper.find('.cover-btn-stub').exists()).toBe(true)
  })
})
