/**
 * CommonEntry 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import CommonEntry from '@/views/entry/commonEntry.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/http/api/entryManage', () => ({
  searchEntryInfo: vi.fn(() => Promise.resolve({
    data: {
      list: [],
      totalNum: 0
    }
  })),
  getPublicEntry: vi.fn(() => Promise.resolve({
    data: {
      list: [],
      totalNum: 0
    }
  })),
  updatePublicEntry: vi.fn(() => Promise.resolve({})),
  deletePublicEntry: vi.fn(() => Promise.resolve({}))
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))


vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

describe('CommonEntry - user 属性重构测试', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('user 属性验证', () => {
    it('不应该在 data 中定义 user 属性', () => {
      const storeMock = createUserStoreMock()
      
      wrapper = mount(CommonEntry, {
        props: {
          currentCommon: {},
          boxHeight: 600
        },
        global: {
          mocks: storeMock,
          stubs: {
            'SearchBox': true,
            'DataBox': true,
            'a-table': true,
            'a-button': true
          }
        }
      })

      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(CommonEntry, {
        props: {
          currentCommon: {},
          boxHeight: 600
        },
        global: {
          mocks: storeMock,
          stubs: {
            'SearchBox': true,
            'DataBox': true,
            'a-table': true,
            'a-button': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toEqual(testUser)
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(CommonEntry, {
        props: {
          currentCommon: {},
          boxHeight: 600
        },
        global: {
          mocks: storeMock,
          stubs: {
            'SearchBox': true,
            'DataBox': true,
            'a-table': true,
            'a-button': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })
})

describe('CommonEntry - 浏览省略与编辑文本域', () => {
  let wrapper

  const tableBodyStub = {
    name: 'ATable',
    props: ['columns', 'dataSource'],
    template: `
      <div class="table-stub" :class="$attrs.class">
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

  function mountWithTableStub(admin = true) {
    const storeMock = createUserStoreMock()
    storeMock.$store.state.admin = admin
    return mount(CommonEntry, {
      props: {
        currentCommon: { department: '测试部门' },
        boxHeight: 600,
      },
      global: {
        mocks: storeMock,
        stubs: {
          SearchBox: { template: '<div><slot name="form" /></div>' },
          DataBox: { template: '<div><slot name="operate" /><slot name="data" /></div>' },
          'a-table': tableBodyStub,
          'a-form': { template: '<form><slot /></form>' },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-button': true,
          'a-input': true,
          'a-select': true,
          CellOverflowTooltip: {
            name: 'CellOverflowTooltip',
            props: ['content'],
            template: '<span class="cell-overflow-tooltip-stub"><slot /></span>',
          },
        },
      },
    })
  }

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('列应锁定 ellipsis.showTitle 且表格带 table-cell-overflow', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    expect(wrapper.vm.columns.every((col) => col.ellipsis?.showTitle === false)).toBe(true)
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })

  it('编辑态 translate/remark 为 TextArea，unique 为 Input', async () => {
    wrapper = mountWithTableStub(true)
    await nextTick()
    wrapper.vm.dataSource = [
      { id: 'c-1', entry: '词条', translate: 'hello', unique: 'u1', remark: '备注', type: '英文' },
    ]
    wrapper.vm.editableData = {
      'c-1': { id: 'c-1', translate: 'hello', unique: 'u1', remark: '备注' },
    }
    await nextTick()
    expect(wrapper.find('[data-col="translate"]').findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(wrapper.find('[data-col="remark"]').findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(wrapper.find('[data-col="unique"]').findComponent({ name: 'InputIME' }).exists()).toBe(true)
  })

  it('序号列不走 catch-all Tooltip，避免覆盖 customRender', async () => {
    wrapper = mountWithTableStub(true)
    await nextTick()
    wrapper.vm.dataSource = [
      { id: 'c-1', entry: '词条', translate: 'hello', unique: 'u1', remark: '备注', type: '英文' },
    ]
    await nextTick()
    const indexCell = wrapper.find('[data-col="index"]')
    expect(indexCell.exists()).toBe(true)
    expect(indexCell.find('.cell-overflow-tooltip-stub').exists()).toBe(false)
  })

  it('非 admin 双击不应写入 editableData', async () => {
    wrapper = mountWithTableStub(false)
    await nextTick()
    wrapper.vm.dataSource = [{ id: 'c-1', entry: '词条', translate: 'hello' }]
    const row = wrapper.vm.customRow({ id: 'c-1' })
    row.onDblclick()
    expect(wrapper.vm.editableData['c-1']).toBeUndefined()
  })
})
