import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import TranslateModal from '@/views/workbench/translateModal.vue'
import { createUserStoreMock } from '../../testUtils/userStoreMock'
import * as validationUtils from '@/utils/validationUtils'
import { isLoading, resetLoading, startLoading, endLoading } from '@/composables/useLoading'

vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  clickInput: vi.fn(),
  createDragModalDirective: vi.fn(() => ({})),
}))

vi.mock('@/http/api/workbench', () => ({
  getEntryTempByTaskID: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  getEntryInfoList: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  updateEntryList: vi.fn(),
  preTranslate: vi.fn(),
  importCommonExcle: vi.fn(),
  capitalizeWords: vi.fn(),
  replaceWords: vi.fn(),
  deleteEntryInfoByTaskID: vi.fn(),
}))

vi.mock('@/http/api/entryManage', () => ({
  translate: vi.fn(),
  workImportExcleTrans: vi.fn(),
}))

vi.mock('@/http/api/download', () => ({
  entryExportByCondition: vi.fn(),
}))

vi.mock('@/http/api/entry', () => ({
  importExcle: vi.fn(),
}))

vi.mock('@/http/api/userPartiality', () => ({
  queryUserPartiality: vi.fn(),
  updateUserPartiality: vi.fn(),
}))

vi.mock('@/http/api/glossary', () => ({
  checkSykEntryBeforeSave: vi.fn(),
}))

vi.mock('ant-design-vue', () => ({
  default: { install: vi.fn() },
  message: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
  Modal: {},
}))

beforeEach(async () => {
  const { getEntryInfoList, updateEntryList } = await import('@/http/api/workbench')
  const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
  getEntryInfoList.mockReset().mockResolvedValue({ data: { list: [], totalNum: 0 } })
  updateEntryList.mockReset().mockResolvedValue({ data: { list: [], totalNum: 0 } })
  checkSykEntryBeforeSave.mockReset().mockResolvedValue({ data: [] })
})

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

describe('TranslateModal - 浏览省略与编辑文本域', () => {
  let wrapper

  function mountWithTableStub() {
    // 保留表格 bodyCell 渲染链路，只替换第三方组件以聚焦翻译态的 UI 行为。
    return mount(TranslateModal, {
      props: {
        visible: true,
        currentTask: { translateType: '英文' },
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          Modal: { template: '<div class="modal-stub"><slot /></div>' },
          'a-table': tableBodyStub,
          'a-form': { template: '<form><slot /></form>' },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-button': true,
          'a-select': true,
          'a-input': true,
          'a-empty': true,
          'a-row': { template: '<div><slot /></div>' },
          'a-col': { template: '<div><slot /></div>' },
          PipelineToolbar: { template: '<div><slot name="taskExtra" /><slot /></div>' },
          ColumnActions: true,
          RulesDropdown: true,
          TransStateSelect: true,
          TransStateBadge: true,
          CellOverflowTooltip: {
            name: 'CellOverflowTooltip',
            props: ['content'],
            template: '<span class="cell-overflow-tooltip-stub"><slot /></span>',
          },
        },
      },
    })
  }

  it('本地筛选后的表头全选使用完整缓存中的最新行对象', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    wrapper.vm.allData = [
      { id: 'entry-1', entry: '命中', english: 'latest' },
      { id: 'entry-2', entry: '未显示', english: 'other' },
    ]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '命中', english: 'stale' },
    ]
    wrapper.vm.pagination.total = 1
    wrapper.vm.filters = null

    wrapper.vm.onSelectAll(true)

    expect(wrapper.vm.selectedRows).toEqual([
      { id: 'entry-1', entry: '命中', english: 'latest' },
    ])
    expect(wrapper.vm.selectedRowKeys).toEqual(['entry-1'])
  })

  afterEach(() => {
    resetLoading()
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('编辑态翻译列为 TextArea 且无 pressEnter 保存；审核意见浏览走 catch-all Tooltip', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.language = { value: 'english' }
    wrapper.vm.editList_needValidate = ['english']
    wrapper.vm.columns = [
      { dataIndex: 'english', title: '英文', colValue: 'translate' },
      { dataIndex: 'englishAuditSuggest', title: '审核意见', colValue: 'auditSuggest' },
    ]
    wrapper.vm.dataSource = [
      { id: 'entry-1', english: 'hello', englishAuditSuggest: 'ok' },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', english: 'hello', englishAuditSuggest: 'ok' },
    }
    await nextTick()

    const translateCell = wrapper.find('[data-col="english"]')
    expect(translateCell.findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(translateCell.findComponent({ name: 'TextAreaIME' }).props('autoSize')).toEqual({
      minRows: 1,
      maxRows: 5,
    })
    expect(translateCell.find('.cell-overflow-tooltip-stub').exists()).toBe(false)

    expect(wrapper.find('[data-col="englishAuditSuggest"]').find('.cell-overflow-tooltip-stub').exists()).toBe(true)
  })

  it('双击只开编辑态，不调用 applyCell', async () => {
    const applySpy = vi.spyOn(validationUtils, 'applyCell')
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.language = { value: 'english' }
    wrapper.vm.columns = [{ dataIndex: 'english' }]
    const record = { id: 'entry-1', entry: '%1', english: 'Press % 1' }
    wrapper.vm.dataSource = [record]
    wrapper.vm.editableData = {}
    wrapper.vm.cellErrors = {}
    const rowEvents = wrapper.vm.customRow(record, 0)
    await rowEvents.onDblclick()
    await nextTick()
    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(applySpy).not.toHaveBeenCalled()
    expect(wrapper.vm.cellErrors['entry-1']).toBeUndefined()
    applySpy.mockRestore()
  })

  it('applyTable 仅提供列元数据，表格带 table-cell-overflow', async () => {
    wrapper = mountWithTableStub()
    wrapper.vm.language = { value: 'english' }
    wrapper.vm.task = { transMap: { value: 'english' } }
    await wrapper.setProps({ visible: false })
    await nextTick()
    await wrapper.setProps({ visible: true })
    await nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.$columnFilterPref).toEqual(expect.objectContaining({
      colPrefName: 'colPref-translateModal',
      needFilter: true,
    }))
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })

  it('同步展示列偏好后替换 columns 引用以重建表格布局', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    const prefName = 'colPref-translateModal-layout-test'
    wrapper.vm.$columnFilterPref = {
      colPrefName: prefName,
      normalWidth: 100,
      needFilter: false,
    }
    wrapper.vm.columnSettingsList = [
      { value: 'entry', required: true, index: 0 },
      { value: 'comment', required: false, index: 1 },
    ]
    wrapper.vm.columns = [
      { colValue: 'entry', dataIndex: 'entry', index: 0 },
      { colValue: 'comment', dataIndex: 'comment', index: 1 },
    ]
    localStorage.setItem(prefName, JSON.stringify({ displayColumn: '' }))
    const previousColumns = wrapper.vm.columns

    wrapper.vm.syncColumnsFromPref()

    expect(wrapper.vm.columns).not.toBe(previousColumns)
    expect(wrapper.vm.columns.map((column) => column.colValue)).toEqual(['entry'])
    localStorage.removeItem(prefName)
  })

  it('scrollTableToRow 使用实际内容区高度定位编辑行', async () => {
    const content = { clientHeight: 240, scrollTop: 0 }
    TranslateModal.methods.scrollTableToRow.call({
      $nextTick: (callback) => callback(),
      $refs: {
        tableContainer: {
          $el: {
            querySelectorAll: () => Array.from({ length: 13 }, () => ({ offsetHeight: 30 })),
            querySelector: () => content,
          },
        },
      },
      pagination: { current: 1, pageSize: 50 },
      tableHeight: { y: 415 },
    }, 12)

    expect(content.scrollTop).toBe(170)
  })

  it('handleOK 先校验当前 editableData，失败行不先被清掉编辑态', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    updateEntryList.mockResolvedValue({ data: { totalNum: 0 } })
    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = { id: 'task-1' }
    wrapper.vm.language = { value: 'english', state: 'englishState' }
    wrapper.vm.columns = [{ dataIndex: 'english' }]
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: false },
      { key: 'toLong', checked: true },
    ]
    const stale = {
      id: 'entry-1',
      entry: '词条',
      english: 'ok',
      englishState: '0',
      maxLength: 20,
    }
    wrapper.vm.dataSource = [{ ...stale }]
    wrapper.vm.selectedRows = [stale]
    wrapper.vm.selectedRowKeys = ['entry-1']
    wrapper.vm.editableData = {
      'entry-1': { ...stale, english: 'a'.repeat(30) },
    }
    wrapper.vm.allData = [{ id: 'entry-1' }]
    wrapper.vm.getTranslateEntry = vi.fn()

    await wrapper.vm.handleOK()
    await nextTick()

    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(wrapper.vm.cellErrors['entry-1'].english).toContain('允许最大字符数为20')
    const saved = updateEntryList.mock.calls[0]?.[1] || []
    expect(saved.find((r) => r.id === 'entry-1')).toBeUndefined()
  })

  it('handleOK 通过后再 merge，保存的是当前编辑译文', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    updateEntryList.mockResolvedValue({ data: { totalNum: 0, list: [] } })
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = { id: 'task-1' }
    wrapper.vm.language = { value: 'english', state: 'englishState' }
    wrapper.vm.columns = [{ dataIndex: 'english' }]
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: false },
      { key: 'toLong', checked: true },
    ]
    await nextTick()
    await flushPromises()
    const stale = {
      id: 'entry-1',
      entry: '词条',
      english: 'old',
      englishState: '0',
      maxLength: 200,
    }
    wrapper.vm.dataSource = [{ ...stale }]
    wrapper.vm.selectedRows = [stale]
    wrapper.vm.selectedRowKeys = ['entry-1']
    wrapper.vm.editableData = {
      'entry-1': { ...stale, english: 'hello from edit' },
    }
    wrapper.vm.allData = [{ id: 'entry-1' }]
    wrapper.vm.getTranslateEntry = vi.fn()

    await wrapper.vm.handleOK()
    await nextTick()

    expect(updateEntryList).toHaveBeenCalled()
    const payload = updateEntryList.mock.calls[0][1]
    expect(payload[0].english).toBe('hello from edit')
    expect(payload[0].englishState).toBe('1')
    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(wrapper.vm.dataSource[0].english).toBe('hello from edit')
    expect(wrapper.vm.dataSource[0].englishState).toBe('1')
  })

  it('handleOK 保存失败时不回写行状态并保留编辑态', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    updateEntryList.mockRejectedValueOnce(new Error('保存失败'))
    checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = { id: 'task-1' }
    wrapper.vm.language = { value: 'english', state: 'englishState' }
    wrapper.vm.columns = [{ dataIndex: 'english' }]
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: false },
      { key: 'toLong', checked: false },
    ]
    await nextTick()
    await flushPromises()
    const row = {
      id: 'entry-1',
      entry: '词条',
      english: 'old value',
      englishState: '0',
      maxLength: 200,
    }
    wrapper.vm.dataSource = [{ ...row }]
    wrapper.vm.selectedRows = [wrapper.vm.dataSource[0]]
    wrapper.vm.selectedRowKeys = ['entry-1']
    wrapper.vm.editableData = {
      'entry-1': { ...row, english: 'edited value' },
    }
    wrapper.vm.getTranslateEntry = vi.fn()

    await wrapper.vm.handleOK()
    await nextTick()

    expect(updateEntryList).toHaveBeenCalled()
    expect(wrapper.vm.dataSource[0].english).toBe('old value')
    expect(wrapper.vm.dataSource[0].englishState).toBe('0')
    expect(wrapper.vm.editableData['entry-1'].english).toBe('edited value')
  })

  it('handleOK 部分成功时只回写成功行并保留失败行编辑态', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    updateEntryList.mockResolvedValueOnce({
      data: { totalNum: 1, list: [{ id: 'entry-2' }] },
    })
    checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = { id: 'task-1' }
    wrapper.vm.language = { value: 'english', state: 'englishState' }
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: false },
      { key: 'toLong', checked: false },
    ]
    await nextTick()
    await flushPromises()
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: 'first', english: 'old first', englishState: '0' },
      { id: 'entry-2', entry: 'second', english: 'old second', englishState: '0' },
    ]
    wrapper.vm.allData = wrapper.vm.dataSource
    wrapper.vm.selectedRows = [...wrapper.vm.dataSource]
    wrapper.vm.selectedRowKeys = ['entry-1', 'entry-2']
    wrapper.vm.editableData = {
      'entry-1': { ...wrapper.vm.dataSource[0], english: 'saved first' },
      'entry-2': { ...wrapper.vm.dataSource[1], english: 'failed second' },
    }
    wrapper.vm.getTranslateEntry = vi.fn()

    await wrapper.vm.handleOK()

    expect(wrapper.vm.dataSource[0]).toMatchObject({
      english: 'saved first',
      englishState: '1',
    })
    expect(wrapper.vm.dataSource[1]).toMatchObject({
      english: 'old second',
      englishState: '0',
    })
    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(wrapper.vm.editableData['entry-2'].english).toBe('failed second')
    expect(wrapper.vm.selectedRows[0].english).toBe('saved first')
    expect(wrapper.vm.selectedRows[1].english).toBe('old second')
  })

  it('getTranslateEntry：revalidateLoaded 期间 loading 为 true，结束后复位', async () => {
    const revalSpy = vi.spyOn(validationUtils, 'revalidateLoaded').mockImplementation(async () => {
      expect(isLoading()).toBe(true)
    })
    const { getEntryInfoList } = await import('@/http/api/workbench')
    getEntryInfoList.mockResolvedValue({
      data: {
        list: [{ id: 'entry-1', entry: '词条', english: 'ok', englishState: '0' }],
      },
    })
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = { id: 'task-1' }
    wrapper.vm.language = { value: 'english', state: 'englishState' }

    await wrapper.vm.getTranslateEntry()
    await nextTick()

    expect(revalSpy).toHaveBeenCalled()
    expect(isLoading()).toBe(false)
    revalSpy.mockRestore()
  })

  it('子弹窗 AfterClose 不再 resetLoading：不击杀并发在途计数，仅重置弹窗自身状态', async () => {
    wrapper = mountWithTableStub()
    await nextTick()

    // 模拟外部在途任务（如父组件红点刷新）占用计数
    startLoading()
    expect(isLoading()).toBe(true)

    // 子弹窗关闭只重置自身状态，不得清零全局计数（否则并发请求的遮罩提前消失）
    wrapper.vm.preTranslateAfterClose()
    expect(isLoading()).toBe(true) // 在途计数保留
    expect(wrapper.vm.preTran.priority).toBeNull()

    wrapper.vm.exportAfterClose()
    expect(isLoading()).toBe(true)
    expect(wrapper.vm.exportModal.field).toEqual(['abbr', '词条'])

    wrapper.vm.replaceAfterClose()
    expect(isLoading()).toBe(true)
    expect(wrapper.vm.replaceModal).toEqual({ sourceStr: null, replaceStr: null })

    // 外部任务正常结束后计数归零（配对由 withLoading / finally 保证，无残留）
    endLoading()
    expect(isLoading()).toBe(false)
  })

  it('preTranslateOK 校验抛错时 endLoading 与关窗仍必达（计数配对优于 resetLoading 兜底）', async () => {
    const { preTranslate } = await import('@/http/api/workbench')
    preTranslate.mockResolvedValueOnce({ data: { list: [] } })
    const revalSpy = vi
      .spyOn(validationUtils, 'verifyArray_workbench_page')
      .mockRejectedValueOnce(new Error('verify failed'))

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = { id: 'task-1' }
    wrapper.vm.language = { value: 'english', state: 'englishState' }
    wrapper.vm.dataSource = [{ id: 'e1', entry: '词条', english: 'ok' }]
    wrapper.vm.preTranslateVisible = true

    // preTranslateOK 未返回内部 promise 链，需等待微任务队列走完（含 async finally）
    wrapper.vm.preTranslateOK()
    await new Promise(resolve => setTimeout(resolve, 50))

    // 校验失败：loading 不残留、子弹窗仍被关闭（原先依赖 afterClose resetLoading 兜底的泄漏源已修复）
    expect(isLoading()).toBe(false)
    expect(wrapper.vm.preTranslateVisible).toBe(false)
    revalSpy.mockRestore()
  })
})
