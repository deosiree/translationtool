import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WorkbenchFormBar from '@/components/Workbench/WorkbenchFormBar.vue'
import WorkbenchActionGroup from '@/components/Workbench/WorkbenchActionGroup.vue'

describe('WorkbenchFormBar', () => {
  it('应渲染 main 与 trailing 槽', () => {
    const wrapper = mount(WorkbenchFormBar, {
      slots: {
        default: '<span class="main-content">查询</span>',
        trailing: '<button class="trailing-btn">展示列</button>',
      },
    })

    expect(wrapper.find('.workbench-form-bar__main .main-content').exists()).toBe(true)
    expect(wrapper.find('.workbench-form-bar__trailing .trailing-btn').exists()).toBe(true)
  })

  it('无 trailing 槽时不渲染 trailing 容器', () => {
    const wrapper = mount(WorkbenchFormBar, {
      slots: {
        default: '<span>main</span>',
      },
    })

    expect(wrapper.find('.workbench-form-bar__trailing').exists()).toBe(false)
  })
})

describe('WorkbenchActionGroup', () => {
  it('inlineOffset 时应带 offset 类名', () => {
    const wrapper = mount(WorkbenchActionGroup, {
      props: { inlineOffset: true },
      slots: { default: '<button>A</button><button>B</button>' },
    })

    expect(wrapper.classes()).toContain('workbench-action-group--offset')
  })

  it('默认不带 offset 类名', () => {
    const wrapper = mount(WorkbenchActionGroup, {
      slots: { default: '<button>A</button>' },
    })

    expect(wrapper.classes()).not.toContain('workbench-action-group--offset')
  })
})
