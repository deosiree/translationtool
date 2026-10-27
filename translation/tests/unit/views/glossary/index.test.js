import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import GlossaryIndex from '@/views/glossary/index.vue'
import { getTranslateStateLabel } from '@/constants/statusMaps.js'
import { createUserStoreMock } from '../../testUtils/userStoreMock'
import { getSykEntry, getSykEntryRelation } from '@/http/api/glossary'
import { cancelRequest } from '@/http/request'
import { message } from 'ant-design-vue'

vi.mock('@/utils/domUtils', () => ({
  clickInput: vi.fn(),
  setModalAriaHidden: vi.fn(),
}))

vi.mock('@/constants/statusMaps.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    getTranslateStateLabel: vi.fn(actual.getTranslateStateLabel),
  }
})

vi.mock('@/http/api/glossary', () => ({
  getSykEntry: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  checkSameEntry: vi.fn(),
  getSykNotUsed: vi.fn(),
  checkSykEntry: vi.fn(),
  updateSykEntry: vi.fn(),
  getSykEntryRelation: vi.fn(() => Promise.resolve({ data: { list: [] } })),
  getSameEntryRelation: vi.fn(),
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))

vi.mock('@/http/request', async (importOriginal) => ({
  ...await importOriginal(),
  cancelRequest: vi.fn(),
}))

vi.mock('@/http/api/userPartiality', () => ({
  updateUserPartiality: vi.fn(),
}))

vi.mock('@/utils/tableUtils', () => ({ getRowClassName: vi.fn() }))

vi.mock('ant-design-vue', () => ({
  default: { install: vi.fn() },
  message: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
  Modal: {},
}))

const tableBodyStub = {
  name: 'ATable',
  props: ['columns', 'dataSource', 'rowSelection', 'pagination', 'loading', 'tableLayout', 'bordered'],
  template: `
    <div class="table-stub" :class="$attrs.class">
      <slot name="headerCell" :title="'tag'" :column="{ dataIndex: 'tag' }" />
      <slot name="headerCell" :title="'comment'" :column="{ dataIndex: 'comment' }" />
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

describe('Glossary - 浏览省略与编辑文本域', () => {
  let wrapper

  /**
   * 使用轻量表格和搜索框替身挂载术语库页面。
   * @returns {Object} Vue Test Utils 挂载包装器。
   */
  function mountWithTableStub() {
    return mount(GlossaryIndex, {
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          SearchBox: {
            template: '<div><slot name="form" /><slot name="operate" /></div>',
          },
          DataBox: {
            name: 'DataBox',
            props: ['height'],
            template: '<div><slot name="operate" /><slot name="data" /></div>',
          },
          'a-table': tableBodyStub,
          'a-form': { template: '<form><slot /></form>' },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-spin': {
            props: ['spinning', 'tip', 'wrapperClassName'],
            template: `
              <div class="ant-spin-nested-loading" :class="wrapperClassName">
                <div v-if="spinning" class="ant-spin ant-spin-spinning" v-bind="$attrs">
                  <span class="ant-spin-text">{{ tip }}</span>
                </div>
                <div class="ant-spin-container" :class="{ 'ant-spin-blur': spinning }">
                  <slot />
                </div>
              </div>
            `,
          },
          'a-button': { template: '<button><slot /></button>' },
          'a-badge': { template: '<span><slot /></span>' },
          'a-input': true,
          'a-select': true,
          ResetButton: {
            props: ['disabled'],
            template: '<button :disabled="disabled">重置</button>',
          },
          BatchSelectModal: true,
          TransStateSelect: true,
          TransStateBadge: {
            name: 'TransStateBadge',
            template: '<span class="trans-state-badge-stub" />',
          },
          RelationModal: true,
          ColumnFilter: true,
          CellOverflowTooltip: {
            name: 'CellOverflowTooltip',
            props: ['content'],
            template: '<span class="cell-overflow-tooltip-stub"><slot /></span>',
          },
          TableCellText: {
            name: 'TableCellText',
            props: ['text', 'value', 'editing'],
            template: '<span class="table-cell-text-stub">{{ editing ? value : text }}</span>',
          },
          TableHeaderHint: {
            props: ['title'],
            template: '<span class="table-header-hint-stub">{{ title }}</span>',
          },
        },
      },
    })
  }

  /**
   * 卸载组件并重置 API mock，避免测试之间共享请求状态。
   * @returns {void} 无返回值。
   */
  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
    getSykEntry.mockReset().mockResolvedValue({ data: { list: [], totalNum: 0 } })
    vi.unstubAllGlobals()
  })

  it('updates toolbar loading as a full-selection operation starts and settles', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    wrapper.vm.selectionScope = 'toolbar-query'

    expect(wrapper.vm.selectionToolbarState.loading).toBe(false)

    const operation = wrapper.vm.selectionOperations.begin('toolbar-query', true)
    await nextTick()
    expect(wrapper.vm.selectionToolbarState.loading).toBe(true)

    wrapper.vm.selectionOperations.reject(operation)
    await nextTick()
    expect(wrapper.vm.selectionToolbarState.loading).toBe(false)
  })

  it('全量收集期间禁用查询区，取消时中止请求并丢弃部分结果', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    let resolveSelection
    getSykEntry.mockImplementation(({ pageSize }) => {
      if (pageSize === 100) {
        return new Promise((resolve) => { resolveSelection = resolve })
      }
      return Promise.resolve({ data: { list: [], totalNum: 0 } })
    })

    const pendingSelection = wrapper.vm.onSelectAll(true)
    await nextTick()

    expect(wrapper.find('.glossary-search-content').attributes('inert')).toBeDefined()
    expect(wrapper.find('.glossary-search-content').attributes('aria-disabled')).toBe('true')
    expect(wrapper.find('.glossary-search > .glossary-search-mask').exists()).toBe(true)
    expect(wrapper.find('.ant-spin-container').classes()).toContain('ant-spin-blur')
    expect(wrapper.find('.ant-spin').attributes('role')).toBe('status')
    expect(wrapper.find('.ant-spin').attributes('aria-live')).toBe('polite')
    expect(wrapper.find('.ant-spin-text').text()).toBe('正在全量加载查询结果，完成后可修改查询条件')
    const searchSelects = wrapper.findAll('.glossary-search-content a-select-stub')
    expect(searchSelects.length).toBeGreaterThan(0)
    expect(searchSelects.every((select) => select.attributes('disabled') === 'true')).toBe(true)
    const queryButton = wrapper
      .findAll('.glossary-search-content button')
      .find((button) => button.text().trim() === '查询')
    expect(queryButton?.attributes('disabled')).toBeDefined()
    const resetButton = wrapper
      .findAll('.glossary-search-content button')
      .find((button) => button.text().trim() === '重置')
    expect(resetButton?.attributes('disabled')).toBeDefined()
    const clearButton = wrapper.findAll('button').find((button) => button.text().includes('取消选择'))
    expect(clearButton?.attributes('disabled')).toBeUndefined()

    wrapper.vm.clearSelectionState()
    expect(cancelRequest).toHaveBeenCalledWith(expect.stringMatching(/^glossary-/))
    expect(wrapper.vm.isCollectingSelection).toBe(false)
    await nextTick()
    expect(wrapper.find('.glossary-search-content').attributes('inert')).toBeUndefined()
    expect(wrapper.find('.ant-spin-container').classes()).not.toContain('ant-spin-blur')
    expect(wrapper.find('.ant-spin').exists()).toBe(false)

    resolveSelection({ data: { totalNum: 3, list: [{ id: 'partial-row' }] } })
    await pendingSelection

    expect(getSykEntry).toHaveBeenCalledTimes(1)
    expect(wrapper.vm.selectEntry).toEqual([])
    expect(message.error).not.toHaveBeenCalled()
  })

  it('取消导致当前分页请求拒绝时不提示错误或继续请求', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    let rejectSelection
    getSykEntry.mockImplementation(({ pageSize }) => {
      if (pageSize === 100) {
        return new Promise((resolve, reject) => { rejectSelection = reject })
      }
      return Promise.resolve({ data: { list: [], totalNum: 0 } })
    })

    const pendingSelection = wrapper.vm.onSelectAll(true)
    wrapper.vm.clearSelectionState()
    rejectSelection(new DOMException('The operation was aborted', 'AbortError'))
    await pendingSelection

    expect(cancelRequest).toHaveBeenCalledWith(expect.stringMatching(/^glossary-/))
    expect(getSykEntry).toHaveBeenCalledTimes(1)
    expect(wrapper.vm.selectEntry).toEqual([])
    expect(message.error).not.toHaveBeenCalled()
  })

  it('离开术语库页面时中止全量选择请求并忽略迟到结果', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    let resolveSelection
    getSykEntry.mockImplementation(({ pageSize }) => {
      if (pageSize === 100) {
        return new Promise((resolve) => { resolveSelection = resolve })
      }
      return Promise.resolve({ data: { list: [], totalNum: 0 } })
    })

    const pendingSelection = wrapper.vm.onSelectAll(true)
    wrapper.unmount()
    wrapper = null
    expect(cancelRequest).toHaveBeenCalledWith(expect.stringMatching(/^glossary-/))

    resolveSelection({ data: { totalNum: 3, list: [{ id: 'partial-row' }] } })
    await pendingSelection

    expect(getSykEntry).toHaveBeenCalledTimes(1)
    expect(message.error).not.toHaveBeenCalled()
  })

  it('开始路由离开时立即中止当前分页请求', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    let resolveSelection
    getSykEntry.mockImplementation(({ pageSize }) => {
      if (pageSize === 100) {
        return new Promise((resolve) => { resolveSelection = resolve })
      }
      return Promise.resolve({ data: { list: [], totalNum: 0 } })
    })

    const pendingSelection = wrapper.vm.onSelectAll(true)
    const next = vi.fn()
    GlossaryIndex.beforeRouteLeave.call(wrapper.vm, {}, {}, next)

    expect(cancelRequest).toHaveBeenCalledWith(expect.stringMatching(/^glossary-/))
    expect(next).toHaveBeenCalledOnce()
    resolveSelection({ data: { totalNum: 3, list: [{ id: 'stale-row' }] } })
    await pendingSelection

    expect(getSykEntry).toHaveBeenCalledTimes(1)
    expect(message.error).not.toHaveBeenCalled()
  })

  it('新的表头选择会中止旧的全量请求并只保留最新操作', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    const pendingResponses = []
    getSykEntry.mockImplementation(({ pageSize }) => {
      if (pageSize === 100) {
        return new Promise((resolve) => { pendingResponses.push(resolve) })
      }
      return Promise.resolve({ data: { list: [], totalNum: 0 } })
    })

    const firstSelection = wrapper.vm.onSelectAll(true)
    const firstRequestId = getSykEntry.mock.calls[0][0].requestId
    const secondSelection = wrapper.vm.onSelectAll(false)
    const secondRequestId = getSykEntry.mock.calls[1][0].requestId

    expect(cancelRequest).toHaveBeenCalledWith(firstRequestId)
    expect(wrapper.vm.selectionOperations.pendingCount).toBe(1)

    pendingResponses[0]({
      data: { totalNum: 3, list: [{ id: 'stale-row-1' }, { id: 'stale-row-2' }] },
    })
    await firstSelection
    expect(getSykEntry).toHaveBeenCalledTimes(2)

    pendingResponses[1]({ data: { totalNum: 1, list: [{ id: 'latest-row' }] } })
    await secondSelection

    expect(getSykEntry).toHaveBeenCalledTimes(2)
    expect(cancelRequest).toHaveBeenCalledTimes(1)
    expect(getSykEntry.mock.calls[1][0].requestId).toBe(secondRequestId)
    expect(wrapper.vm.selectEntry).toEqual([])
    expect(message.error).not.toHaveBeenCalled()
  })

  it('术语库表格内翻页不会中断全量收集', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    let resolveFirstSelectionPage
    getSykEntry.mockImplementation(({ pageIndex, pageSize }) => {
      if (pageSize === 100) {
        return new Promise((resolve) => { resolveFirstSelectionPage = resolve })
      }
      if (pageSize === 20) {
        return Promise.resolve({ data: { list: [{ id: 'visible-page-row' }], totalNum: 3 } })
      }
      return Promise.resolve({ data: { list: [{ id: 'collected-row-3' }], totalNum: 3 } })
    })

    const pendingSelection = wrapper.vm.onSelectAll(true)
    await nextTick()
    wrapper.vm.pagination.onChange(2, 20)
    await flushPromises()

    expect(cancelRequest).not.toHaveBeenCalled()
    expect(wrapper.vm.selectionOperations.pendingCount).toBe(1)

    resolveFirstSelectionPage({
      data: { totalNum: 3, list: [{ id: 'collected-row-1' }, { id: 'collected-row-2' }] },
    })
    await pendingSelection

    expect(getSykEntry.mock.calls.map(([params]) => [params.pageIndex, params.pageSize])).toEqual([
      [1, 100],
      [2, 20],
      [2, 2],
    ])
    expect(wrapper.vm.selectEntry.map((row) => row.id)).toEqual([
      'collected-row-1',
      'collected-row-2',
      'collected-row-3',
    ])
    expect(wrapper.vm.isCollectingSelection).toBe(false)
    expect(wrapper.find('.glossary-search-content').attributes('inert')).toBeUndefined()
  })

  it('全量选择接口失败时仍显示原错误', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    getSykEntry.mockRejectedValue(new Error('全量接口故障'))

    await wrapper.vm.onSelectAll(true)

    expect(message.error).toHaveBeenCalledWith('全量选择失败：全量接口故障')
  })

  it('词条和翻译使用共享文本单元格，翻译编辑态由页面状态控制', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    // getSearch() 会先清空 dataSource，再被接口回填；测单元格时禁止后续查询覆盖夹具
    wrapper.vm.getSearch = vi.fn()
    wrapper.vm.getSearchClick = vi.fn()
    wrapper.vm.columns = [
      { dataIndex: 'index', title: '序号', colValue: 'index' },
      { dataIndex: 'translate', title: '翻译', colValue: 'translate' },
      { dataIndex: 'translateState', title: '翻译状态', colValue: 'translateState' },
      { dataIndex: 'operation', title: '操作', colValue: 'operation' },
      { dataIndex: 'entry', title: '词条', colValue: 'entry' },
    ]
    wrapper.vm.dataSource = [
      { id: 'g-1', translate: 'hello', translateState: 0, relationCount: 1, entry: '词条' },
    ]
    wrapper.vm.editableData = {}
    await nextTick()

    const translateBrowse = wrapper.find('[data-col="translate"]')
    expect(translateBrowse.exists()).toBe(true)
    expect(translateBrowse.findComponent({ name: 'TableCellText' }).props()).toEqual(
      expect.objectContaining({ text: 'hello', editing: false })
    )
    expect(wrapper.find('[data-col="entry"]').findComponent({ name: 'TableCellText' }).exists()).toBe(true)
    expect(wrapper.find('[data-col="translateState"]').find('.cell-overflow-tooltip-stub').exists()).toBe(true)
    expect(getTranslateStateLabel).toHaveBeenCalledWith(0)
    expect(wrapper.find('[data-col="operation"]').find('.cell-overflow-tooltip-stub').exists()).toBe(false)
    expect(wrapper.find('[data-col="index"]').find('.cell-overflow-tooltip-stub').exists()).toBe(false)

    wrapper.vm.editableData = { 'g-1': { id: 'g-1', translate: 'hello' } }
    await nextTick()
    const translateEdit = wrapper.find('[data-col="translate"]')
    expect(translateEdit.exists()).toBe(true)
    expect(translateEdit.findComponent({ name: 'TableCellText' }).props()).toEqual(
      expect.objectContaining({ value: 'hello', editing: true })
    )
    expect(wrapper.find('[data-col="translateState"]').find('.trans-state-badge-stub').exists()).toBe(true)
  })

  it('applyTable 只生成列元数据且表格带 table-cell-overflow', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    expect(wrapper.vm.$columnFilterPref).toEqual(expect.objectContaining({
      colPrefName: 'colPref-glossary',
      needFilter: false,
    }))
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })

  it('术语库使用共享视口处理固定布局和内部横向滚动', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()

    const viewport = wrapper.findComponent({ name: 'TableViewport' })
    const table = wrapper.findComponent({ name: 'ATable' })
    expect(viewport.exists()).toBe(true)
    expect(viewport.props('scroll')).toEqual(wrapper.vm.tableHeight)
    expect(table.props('tableLayout')).toBe('fixed')
    expect(table.props('bordered')).toBe(true)
  })

  it('tag/comment 列头使用统一提示组件', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()

    expect(wrapper.findAll('.table-header-hint-stub').map((node) => node.text())).toEqual([
      'tag',
      'comment',
    ])
  })

  it('术语审核员列默认可见并使用配置宽度', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()

    expect(wrapper.vm.columnSettingsList.find((column) => column.value === 'entryAuditor')).toEqual(
      expect.objectContaining({ visible: true, width: 128 })
    )
  })

  it('术语库所有数据列均允许拖拽调整宽度', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()

    expect(wrapper.vm.columns.length).toBeGreaterThan(0)
    expect(wrapper.vm.columns.every((column) => column.resizable)).toBe(true)
  })

  it('DataBox 通过 flex 占用剩余高度，不再依赖 JS 固定高度测量', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    expect(wrapper.findComponent({ name: 'DataBox' }).props('height')).toBeUndefined()
    expect(wrapper.vm.dataHeight).toBeUndefined()
    expect(wrapper.vm.updateTableHeight).toBeUndefined()
    expect(wrapper.findComponent({ name: 'TableViewport' }).props('height')).toBe('100%')
  })

  it('词条和翻译列左对齐，且都可拖到 50px', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()

    expect(wrapper.vm.columns.find((column) => column.colValue === 'entry')).toEqual(
      expect.objectContaining({ align: 'left', minWidth: 50 })
    )
    expect(wrapper.vm.columns.find((column) => column.colValue === 'translate')).toEqual(
      expect.objectContaining({ align: 'left', minWidth: 50 })
    )
    expect(wrapper.vm.columns.find((column) => column.colValue === 'entry').customHeaderCell).toBeUndefined()
    expect(wrapper.vm.columns.find((column) => column.colValue === 'translate').customCell).toBeUndefined()
  })

  it('术语库不再维护页面级列宽状态', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()

    expect(wrapper.vm.handleResizeColumn).toBeUndefined()
    expect(wrapper.vm.recomputeAdaptiveColumnWidths).toBeUndefined()
    expect(wrapper.vm.columnWidthObserver).toBeUndefined()
  })

  it('关联信息未完成时保持表格 loading，全部渲染完成后才关闭', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()

    let resolveRelation
    getSykEntry.mockResolvedValue({
      data: { list: [{ id: 'loading-row' }], totalNum: 1 },
    })
    getSykEntryRelation.mockImplementation(
      () => new Promise((resolve) => { resolveRelation = resolve })
    )

    const request = wrapper.vm.getSearch()
    await flushPromises()
    expect(wrapper.vm.loading).toBe(true)

    resolveRelation({ data: { list: [{ id: 'relation' }] } })
    await request
    expect(wrapper.vm.loading).toBe(false)
    getSykEntryRelation.mockResolvedValue({ data: { list: [] } })
  })

  it('查询失败也等到两帧视图稳定后再关闭 loading', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    const frames = []
    vi.stubGlobal('requestAnimationFrame', (callback) => {
      frames.push(callback)
      return frames.length
    })
    getSykEntry.mockRejectedValue(new Error('offline'))

    const request = wrapper.vm.getSearch()
    await flushPromises()
    expect(wrapper.vm.loading).toBe(true)
    expect(frames).toHaveLength(1)

    frames.shift()()
    expect(wrapper.vm.loading).toBe(true)
    expect(frames).toHaveLength(1)

    frames.shift()()
    await request
    expect(wrapper.vm.loading).toBe(false)
    getSykEntry.mockResolvedValue({ data: { list: [], totalNum: 0 } })
  })

  it('表头全选收集当前查询全部分页，翻页后立即投影全选状态', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    wrapper.vm.search.searchType = 'getSykEntry'
    wrapper.vm.pagination.current = 1
    wrapper.vm.pagination.pageSize = 20
    getSykEntry.mockImplementation(async (params) => {
      const totalNum = 150
      const start = (params.pageIndex - 1) * params.pageSize
      const size = Math.min(params.pageSize, totalNum - start)
      return {
        data: {
          totalNum,
          list: Array.from({ length: size }, (_, index) => ({ id: start + index + 1 })),
        },
      }
    })
    await wrapper.vm.getSearch()

    await wrapper.vm.onSelectAll(true, [], [])

    expect(wrapper.vm.selectEntry).toHaveLength(150)
    expect(getSykEntry.mock.calls.map(([params]) => params.pageIndex)).toEqual([1, 1, 2])

    wrapper.vm.pagination.current = 2
    await wrapper.vm.getSearch()
    expect(wrapper.vm.tableSelectedRowKeys).toHaveLength(20)
    expect(wrapper.vm.tableSelectedRowKeys).toContain(21)
  })

  it('全量选择守卫开始和失败时立即刷新当前页投影', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    wrapper.vm.selectionScope = 'same-query'
    wrapper.vm.dataSource = [{ id: 'entry-1' }, { id: 'entry-2' }]
    wrapper.vm.selectedRowKeys = ['entry-1', 'entry-2']
    await nextTick()

    const operation = wrapper.vm.selectionOperations.begin('same-query', false)
    await nextTick()
    expect(wrapper.vm.tableSelectedRowKeys).toEqual([])

    wrapper.vm.selectionOperations.reject(operation)
    await nextTick()
    expect(wrapper.vm.tableSelectedRowKeys).toEqual(['entry-1', 'entry-2'])
  })

  it('默认条件查询完成后保留原始查询快照', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    wrapper.vm.search = {
      ...wrapper.vm.search,
      searchType: null,
    }
    getSykEntry.mockResolvedValue({ data: { list: [], totalNum: 0 } })
    await wrapper.vm.getSearch()
    expect(JSON.stringify(wrapper.vm.lastSearch)).toBe(JSON.stringify(wrapper.vm.search))
  })

  it('只提交最新条件查询的列表响应', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    let resolveFirst
    let resolveSecond
    getSykEntry
      .mockImplementationOnce(() => new Promise((resolve) => { resolveFirst = resolve }))
      .mockImplementationOnce(() => new Promise((resolve) => { resolveSecond = resolve }))

    wrapper.vm.search.entry = 'first'
    getSykEntryRelation.mockResolvedValue({ data: { list: [] } })
    const firstRequest = wrapper.vm.getSearch()
    wrapper.vm.search.entry = 'second'
    const secondRequest = wrapper.vm.getSearch()
    resolveSecond({ data: { list: [{ id: 'second' }], totalNum: 1 } })
    await secondRequest
    resolveFirst({ data: { list: [{ id: 'first' }], totalNum: 1 } })
    await firstRequest

    expect(wrapper.vm.dataSource.map((row) => row.id)).toEqual(['second'])
  })

  it('查询范围变化时保留已提交选择并忽略旧范围全量响应', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    vi.clearAllMocks()
    wrapper.vm.selectEntry = [{ id: 'outside-query', value: 'kept' }]
    wrapper.vm.selectedRows = wrapper.vm.selectEntry
    wrapper.vm.selectedRowKeys = ['outside-query']
    wrapper.vm.lastSearch = { searchType: 'getSykEntry' }

    let resolveSelection
    getSykEntry.mockImplementation(({ pageSize }) => {
      if (pageSize === 100) {
        return new Promise((resolve) => { resolveSelection = resolve })
      }
      return Promise.resolve({ data: { list: [{ id: 'new-query-row' }], totalNum: 1 } })
    })

    const pendingSelection = wrapper.vm.onSelectAll(true)
    expect(wrapper.vm.selectionOperations.pendingCount).toBe(1)

    wrapper.vm.search.entry = 'next query'
    await wrapper.vm.getSearch()
    await nextTick()
    expect(wrapper.vm.selectionOperations.pendingCount).toBe(0)
    expect(cancelRequest).toHaveBeenCalledWith(expect.stringMatching(/^glossary-/))
    resolveSelection({ data: { list: [{ id: 'stale-row' }], totalNum: 1 } })
    await pendingSelection

    expect(wrapper.vm.selectEntry).toEqual([{ id: 'outside-query', value: 'kept' }])
    expect(wrapper.vm.selectedRowKeys).toEqual(['outside-query'])
    expect(message.error).not.toHaveBeenCalled()
  })

  it('复选框常驻显示，工具栏保留取消选择和已选词条', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()

    const table = wrapper.findComponent({ name: 'ATable' })
    expect(table.props('rowSelection')).toEqual(expect.objectContaining({
      columnWidth: 48,
      onSelect: expect.any(Function),
      onSelectAll: expect.any(Function),
    }))
    expect(wrapper.text()).toContain('已选词条')
    expect(wrapper.text()).toContain('取消选择')
    expect(wrapper.text()).not.toContain('批量选择')
    const selectedButton = wrapper
      .findAll('.select-toolbar-button')
      .find((button) => button.text().trim() === '已选词条')
    expect(selectedButton.classes()).toContain('resetBtn')
  })
})
