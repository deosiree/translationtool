import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WorkbenchTaskInfo from '@/components/Workbench/WorkbenchTaskInfo.vue'

describe('WorkbenchTaskInfo', () => {
  const task = {
    name: '任务A',
    productName: '产品B',
    classifyName: '分类C',
    translateType: '英文',
  }

  it('应渲染四项任务信息', () => {
    const wrapper = mount(WorkbenchTaskInfo, {
      props: { task },
    })

    const text = wrapper.text()
    expect(text).toContain('任务名称：任务A')
    expect(text).toContain('产品名称：产品B')
    expect(text).toContain('上级分类名称：分类C')
    expect(text).toContain('翻译语种：英文')
  })

  it('应渲染 extra 槽内容', () => {
    const wrapper = mount(WorkbenchTaskInfo, {
      props: { task },
      slots: {
        extra: '<span class="rules-slot">RulesDropdown</span>',
      },
    })

    expect(wrapper.find('.rules-slot').exists()).toBe(true)
  })
})
