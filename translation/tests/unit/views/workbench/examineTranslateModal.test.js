import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ExamineTranslateModal from '@/views/workbench/examineTranslateModal.vue'
import { createUserStoreMock } from '../../testUtils/userStoreMock'

vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  createDragModalDirective: vi.fn(() => ({})),
}))

vi.mock('@/http/api/workbench', () => ({
  getEntryTempByTaskID: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  updateEntryTemp: vi.fn(),
  getEntryInfoList: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  updateEntryList: vi.fn(),
  deleteEntryInfoByTaskID: vi.fn(),
}))

vi.mock('@/http/api/glossary', () => ({
  checkSykEntryBeforeSave: vi.fn(() => Promise.resolve({ data: [] })),
}))

vi.mock('ant-design-vue', () => ({
  default: { install: vi.fn() },
  message: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn(), warn: vi.fn() },
  Modal: {},
}))

beforeEach(async () => {
  const { getEntryInfoList, updateEntryList } = await import('@/http/api/workbench')
  const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
  getEntryInfoList.mockReset().mockResolvedValue({ data: { list: [], totalNum: 0 } })
  updateEntryList.mockReset().mockResolvedValue({ data: { list: [], totalNum: 0 } })
  checkSykEntryBeforeSave.mockReset().mockResolvedValue({ data: [] })
})

vi.mock('keymaster', () => ({
  default: vi.fn(),
}))

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

describe('ExamineTranslateModal - 浏览省略与编辑文本域', () => {
  let wrapper

  function mountWithTableStub() {
    // 仅隔离 Ant 外壳，保留 bodyCell 插槽以验证浏览态与编辑态的真实切换。
    return mount(ExamineTranslateModal, {
      props: {
        visible: true,
        currentTask: { translateType: '英文' },
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          Modal: { template: '<div><slot /></div>' },
          PipeShell: { template: '<div><slot /></div>', props: ['visible'] },
          'a-table': tableBodyStub,
          'a-form': { template: '<form><slot /></form>' },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-button': true,
          'a-input': true,
          'a-empty': true,
          PipelineToolbar: { template: '<div><slot name="taskExtra" /><slot /><slot name="subToolbar" /></div>' },
          RulesDropdown: true,
          TransStateSelect: true,
          IsExistBadge: true,
          EntryStateBadge: true,
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

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('浏览态当前语种翻译在编辑列初始化前仍使用 TableCellText', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = {
      transMap: {
        value: 'english',
        interpretation: 'englishInterpretation',
        auditSuggest: 'englishAuditSuggest',
      },
    }
    wrapper.vm.editList_needValidate = []
    wrapper.vm.editList = []
    wrapper.vm.columns = [
      { dataIndex: 'entry', title: '词条', colValue: 'entry' },
      { dataIndex: 'english', title: '英文', colValue: 'english' },
    ]
    wrapper.vm.dataSource = [{ id: 'entry-1', entry: '词条', english: 'long translation' }]
    wrapper.vm.editableData = {}
    await nextTick()

    const translationCell = wrapper.find('[data-col="english"]').findComponent({ name: 'TableCellText' })
    expect(translationCell.exists()).toBe(true)
    expect(translationCell.props()).toEqual(expect.objectContaining({
      text: 'long translation',
      editing: false,
    }))
  })

  it('编辑态翻译与审核意见为 TextArea', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.editList_needValidate = ['english']
    wrapper.vm.editList = ['englishInterpretation', 'englishAuditSuggest']
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

    expect(wrapper.find('[data-col="english"]').findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(wrapper.find('[data-col="englishAuditSuggest"]').findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
  })

  it('handleOK：verifyArray_workbench 返回 errorIds 时不调用 updateEntryList', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: 1, parentID: '' },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue', auditState: 1, parentID: '' },
    }
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: false }]

    await wrapper.vm.handleOK()
    await nextTick()

    expect(updateEntryList).not.toHaveBeenCalled()
  })

  it('handleOK：所有行通过校验时调用 updateEntryList', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [] })
    updateEntryList.mockResolvedValue({ data: {} })

    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: 'hello', english: 'hello', auditState: 1, parentID: '' },
    ]
    wrapper.vm.editableData = {}
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: false }]

    await wrapper.vm.handleOK()
    await nextTick()

    expect(updateEntryList).toHaveBeenCalled()
  })

  it('handleOK：保存失败时保留原行与编辑值，不提前更新状态', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    updateEntryList.mockRejectedValueOnce(new Error('保存失败'))
    checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: false },
      { key: 'toLong', checked: false },
    ]
    await nextTick()
    await flushPromises()
    const row = {
      id: 'entry-1',
      entry: 'term',
      english: 'old value',
      englishState: '0',
      auditState: 1,
      parentID: '',
    }
    wrapper.vm.dataSource = [{ ...row }]
    wrapper.vm.editableData = {
      'entry-1': { ...row, english: 'edited value' },
    }
    await wrapper.vm.handleOK()

    expect(updateEntryList).toHaveBeenCalled()
    expect(wrapper.vm.dataSource[0].english).toBe('old value')
    expect(wrapper.vm.dataSource[0].englishState).toBe('0')
    expect(wrapper.vm.editableData['entry-1'].english).toBe('edited value')
  })

  it('handleOK：仅编辑翻译未设 auditState，点保存应被拦截（校验前置）', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: false },
      { key: 'toLong', checked: false },
    ]
    await nextTick()
    await flushPromises()
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    // 未设 auditState，仅在 editableData 有编辑
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined, parentID: '' },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue', parentID: '' },
    }
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: false }]

    await wrapper.vm.handleOK()
    await nextTick()

    expect(updateEntryList).not.toHaveBeenCalled()
  })

  it('handleOK：校验失败后 editableData 应完整保留（不被清空）', async () => {
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: 1, parentID: '' },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue', parentID: '' },
    }
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: false }]

    await wrapper.vm.handleOK()
    await nextTick()

    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
  })

  it('handleOK：直接保存正确翻译时应先校验并回填到 dataSource', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [] })
    updateEntryList.mockResolvedValueOnce({ data: { totalNum: 0, list: [] } })

    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: 1, parentID: '' },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press %1 to continue again', parentID: '' },
    }
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: false }]
    wrapper.vm.getTaskEntry = vi.fn()

    await wrapper.vm.handleOK()
    await nextTick()

    expect(wrapper.vm.dataSource[0].english).toBe('Press %1 to continue again')
    expect(wrapper.vm.dataSource[0].englishState).toBe('3')
    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(updateEntryList).toHaveBeenCalled()
  })

  it('handleOK：部分成功只回写成功行并保留失败行编辑态', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    updateEntryList.mockResolvedValueOnce({
      data: { totalNum: 1, list: [{ id: 'entry-2' }] },
    })
    checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

    wrapper = mountWithTableStub()
    await nextTick()
    await flushPromises()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: 'first', english: 'old first', englishState: '0', auditState: 1 },
      { id: 'entry-2', entry: 'second', english: 'old second', englishState: '0', auditState: 1 },
    ]
    wrapper.vm.allData = wrapper.vm.dataSource
    wrapper.vm.selectedRows = [...wrapper.vm.dataSource]
    wrapper.vm.selectedRowKeys = ['entry-1', 'entry-2']
    wrapper.vm.editableData = {
      'entry-1': { ...wrapper.vm.dataSource[0], english: 'saved first' },
      'entry-2': { ...wrapper.vm.dataSource[1], english: 'failed second' },
    }
    wrapper.vm.getTaskEntry = vi.fn()

    await wrapper.vm.handleOK()

    expect(wrapper.vm.dataSource[0]).toMatchObject({
      english: 'saved first',
      englishState: '3',
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

  it('doubleClick：进入编辑态时不应主动触发表单校验或写红字', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    const record = { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue', auditState: undefined, parentID: '' }
    wrapper.vm.dataSource = [record]

    const rowEvents = wrapper.vm.doubleClick(record, 0)
    rowEvents.onDblclick()
    await nextTick()

    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(wrapper.vm.cellErrors?.['entry-1']?.english).toBeUndefined()
  })

  it('scrollTableToRow 使用实际内容区高度定位编辑行', async () => {
    const content = { clientHeight: 240, scrollTop: 0 }
    ExamineTranslateModal.methods.scrollTableToRow.call({
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

    expect(content.scrollTop).toBe(120)
  })

  it('edit：错误翻译点击行内✓时应拦截并保留编辑态', async () => {
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    const record = { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined, parentID: '' }
    wrapper.vm.dataSource = [record]

    const rowEvents = wrapper.vm.doubleClick(record, 0)
    rowEvents.onDblclick()
    wrapper.vm.onCellInput('Press % 1 to continue', record, { dataIndex: 'english' })
    await wrapper.vm.edit(record)
    await nextTick()

    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(wrapper.vm.cellErrors?.['entry-1']?.english).toBeTruthy()
  })

  it('edit：仅勾选 toLong 时，不应因 special 不一致拦截', async () => {
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    const record = { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined, parentID: '', maxLength: 200 }
    wrapper.vm.dataSource = [record]
    wrapper.vm.rulesOptions = [{ key: 'special', checked: false }, { key: 'toLong', checked: true }]
    await nextTick()
    await flushPromises()

    const rowEvents = wrapper.vm.doubleClick(record, 0)
    await rowEvents.onDblclick()
    wrapper.vm.onCellInput('Press % 1 to continue', record, { dataIndex: 'english' })
    await wrapper.vm.edit(record)
    await nextTick()

    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(wrapper.vm.cellErrors?.['entry-1']?.english).toBeUndefined()
    expect(record.english).toBe('Press % 1 to continue')
  })

  it('handleOK：仅勾选 toLong 时，special 不一致不应阻断底部保存', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })
    updateEntryList.mockResolvedValue({ data: {} })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: 1, parentID: '', maxLength: 200 },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue', auditState: 1, parentID: '', maxLength: 200 },
    }
    wrapper.vm.rulesOptions = [{ key: 'special', checked: false }, { key: 'toLong', checked: true }]

    await wrapper.vm.handleOK()
    await nextTick()

    expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
    expect(updateEntryList).toHaveBeenCalled()
  })

  it('handleOK：规则切换后应复检浏览态待流转词条并拦截', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    const record = { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined, parentID: '', maxLength: 200 }
    wrapper.vm.dataSource = [record]

    // 第一步：关闭 special，让错误翻译通过行内 ✓ 退出编辑态
    wrapper.vm.rulesOptions = [{ key: 'special', checked: false }, { key: 'toLong', checked: true }]
    await nextTick()
    await flushPromises()
    const rowEvents = wrapper.vm.doubleClick(record, 0)
    await rowEvents.onDblclick()
    wrapper.vm.onCellInput('Press % 1 to continue', record, { dataIndex: 'english' })
    await wrapper.vm.edit(record)
    await nextTick()

    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(record.english).toBe('Press % 1 to continue')

    // 第二步：重新打开 special，并将该浏览态词条标记为通过
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: true }]
    record.auditState = 1
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    await wrapper.vm.handleOK()
    await nextTick()

    expect(updateEntryList).not.toHaveBeenCalled()
    expect(wrapper.vm.cellErrors?.['entry-1']?.english).toBeTruthy()
  })

  it('edit：全部规则关闭时，不应触发长度或 special 拦截', async () => {
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    const record = { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined, parentID: '', maxLength: 1 }
    wrapper.vm.dataSource = [record]
    wrapper.vm.rulesOptions = [{ key: 'special', checked: false }, { key: 'toLong', checked: false }]
    await nextTick()
    await flushPromises()

    const rowEvents = wrapper.vm.doubleClick(record, 0)
    await rowEvents.onDblclick()
    wrapper.vm.onCellInput('Press % 1 to continue', record, { dataIndex: 'english' })
    await wrapper.vm.edit(record)
    await nextTick()

    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(wrapper.vm.cellErrors?.['entry-1']?.english).toBeUndefined()
  })

  it('applyTable 仅提供列元数据，表格带 table-cell-overflow', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    wrapper.vm.task = {
      transMap: {
        value: 'english',
        interpretation: 'englishInterpretation',
        auditSuggest: 'englishAuditSuggest',
      },
    }
    await wrapper.setProps({ visible: false })
    await nextTick()
    await wrapper.setProps({ visible: true })
    await nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.$columnFilterPref).toEqual(expect.objectContaining({
      colPrefName: 'colPref-examineTranslateModal',
      needFilter: true,
    }))
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })
})
