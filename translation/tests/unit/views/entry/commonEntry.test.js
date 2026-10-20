/**
 * CommonEntry 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import CommonEntry from '@/views/entry/commonEntry.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'
import { createDeferred } from '../../testUtils/deferred'
import { deletePublicEntry as deletePublicEntryApi, getPublicEntry, updatePublicEntry } from '@/http/api/entryManage'
import { message, Modal } from 'ant-design-vue'

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
  },
  Modal: {
    confirm: vi.fn(({ onOk }) => onOk?.())
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

describe('CommonEntry - 查询响应竞态', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  /**
   * 挂载公共词条表并等待初始化查询完成。
   * @returns {Promise<any>} 初始化完成后的当前测试组件。
   */
  async function mountReadyEntry() {
    wrapper = mount(CommonEntry, {
      props: {
        currentCommon: { department: '测试部门' },
        boxHeight: 600,
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          SearchBox: true,
          DataBox: true,
          'a-table': true,
          'a-button': true,
        },
      },
    })
    await flushPromises()
    getPublicEntry.mockClear()
    message.error.mockClear()
    return wrapper
  }

  it('只提交最新查询结果，较早成功响应不能覆盖新结果', async () => {
    await mountReadyEntry()
    const olderRequest = createDeferred()
    const latestRequest = createDeferred()
    getPublicEntry
      .mockReturnValueOnce(olderRequest.promise)
      .mockReturnValueOnce(latestRequest.promise)

    wrapper.vm.search.entry = '旧条件'
    wrapper.vm.getCommonEntry()
    wrapper.vm.search.entry = '新条件'
    wrapper.vm.getCommonEntry()

    latestRequest.resolve({ data: { list: [{ id: 'latest' }] } })
    await flushPromises()
    olderRequest.resolve({ data: { list: [{ id: 'stale' }] } })
    await flushPromises()

    expect(wrapper.vm.dataSource).toEqual([{ id: 'latest' }])
    expect(wrapper.vm.loading).toBe(false)
  })

  it('过期查询失败不提示错误，也不提前结束最新查询的 loading', async () => {
    await mountReadyEntry()
    const olderRequest = createDeferred()
    const latestRequest = createDeferred()
    getPublicEntry
      .mockReturnValueOnce(olderRequest.promise)
      .mockReturnValueOnce(latestRequest.promise)

    wrapper.vm.getCommonEntry()
    wrapper.vm.search.entry = '新条件'
    wrapper.vm.getCommonEntry()
    olderRequest.reject(new Error('旧查询失败'))
    await flushPromises()

    expect(message.error).not.toHaveBeenCalled()
    expect(wrapper.vm.loading).toBe(true)

    latestRequest.resolve({ data: { list: [{ id: 'latest' }] } })
    await flushPromises()
    expect(wrapper.vm.dataSource).toEqual([{ id: 'latest' }])
    expect(wrapper.vm.loading).toBe(false)
  })

  it('重置触发的新查询优先于重置前的查询结果', async () => {
    await mountReadyEntry()
    const beforeReset = createDeferred()
    const afterReset = createDeferred()
    getPublicEntry
      .mockReturnValueOnce(beforeReset.promise)
      .mockReturnValueOnce(afterReset.promise)

    wrapper.vm.search.entry = '旧条件'
    wrapper.vm.getCommonEntry()
    wrapper.vm.reset()

    afterReset.resolve({ data: { list: [{ id: 'reset' }] } })
    await flushPromises()
    beforeReset.resolve({ data: { list: [{ id: 'stale' }] } })
    await flushPromises()

    expect(wrapper.vm.dataSource).toEqual([{ id: 'reset' }])
  })

  it('保存和删除后的刷新共享最新查询保护', async () => {
    await mountReadyEntry()
    const beforeWrite = createDeferred()
    const afterSave = createDeferred()
    const afterDelete = createDeferred()
    getPublicEntry
      .mockReturnValueOnce(beforeWrite.promise)
      .mockReturnValueOnce(afterSave.promise)
      .mockReturnValueOnce(afterDelete.promise)
    updatePublicEntry.mockResolvedValue({})
    deletePublicEntryApi.mockResolvedValue({})

    wrapper.vm.getCommonEntry()
    wrapper.vm.editableData = { row: { id: 'row', translate: 'new' } }
    wrapper.vm.save({ id: 'row' })
    await flushPromises()

    wrapper.vm.selectedRowKeys = ['row']
    wrapper.vm.deletePublicEntry()
    await flushPromises()

    afterDelete.resolve({ data: { list: [{ id: 'latest' }] } })
    await flushPromises()
    afterSave.resolve({ data: { list: [{ id: 'stale-save' }] } })
    beforeWrite.resolve({ data: { list: [{ id: 'stale-query' }] } })
    await flushPromises()

    expect(Modal.confirm).toHaveBeenCalled()
    expect(wrapper.vm.dataSource).toEqual([{ id: 'latest' }])
  })
})
