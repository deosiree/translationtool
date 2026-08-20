import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ExamineModal from '@/views/workbench/examineModal.vue'
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

describe('ExamineModal - 浏览省略与编辑文本域', () => {
  let wrapper

  function mountWithTableStub() {
    return mount(ExamineModal, {
      props: {
        visible: true,
        currentTask: { translateType: '英文' },
      },
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          CustomModal: { template: '<div><slot /></div>' },
          'a-table': tableBodyStub,
          'a-form': {
            name: 'AForm',
            props: ['layout', 'model', 'rules'],
            template: '<form class="a-form-stub"><slot /></form>',
          },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-button': true,
          'a-input': true,
          PipelineToolbar: { template: '<div><slot name="taskExtra" /><slot /><slot name="columnActions" /><slot name="subToolbar" /></div>' },
          ColumnActions: true,
          RulesDropdown: true,
          EntryStateSelect: true,
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

  it('编辑态翻译列为 TextArea；badge 与操作列互斥', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.editList_needValidate = ['english']
    wrapper.vm.editList = ['englishInterpretation', 'comment']
    wrapper.vm.columns = [
      { dataIndex: 'english', title: '英文', colValue: 'translate' },
      { dataIndex: 'entryState', title: '词条状态', colValue: 'entryState' },
      { dataIndex: 'operation', title: '操作', colValue: 'operation' },
      { dataIndex: 'diFileName', title: '辞典', colValue: 'diFileName' },
    ]
    wrapper.vm.dataSource = [
      {
        id: 'entry-1',
        english: 'hello',
        entryState: 0,
        operation: '',
        diFileName: 'a.dic',
        auditState: 1,
      },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', english: 'hello', diFileName: 'a.dic' },
    }
    await nextTick()

    const translateCell = wrapper.find('[data-col="english"]')
    expect(translateCell.findComponent({ name: 'TableCellTextArea' }).exists()).toBe(true)
    expect(translateCell.findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(translateCell.find('.cell-overflow-tooltip-stub').exists()).toBe(false)
    expect(translateCell.findComponent({ name: 'AForm' }).exists()).toBe(false)

    expect(wrapper.find('[data-col="entryState"]').find('.cell-overflow-tooltip-stub').exists()).toBe(true)
    expect(wrapper.find('[data-col="entryState"]').findComponent({ name: 'TextAreaIME' }).exists()).toBe(false)
    expect(wrapper.find('[data-col="operation"]').find('.editable-row-operations').exists()).toBe(true)
    expect(wrapper.find('[data-col="operation"]').find('.cell-overflow-tooltip-stub').exists()).toBe(false)
    expect(wrapper.find('[data-col="diFileName"]').findComponent({ name: 'InputIME' }).exists()).toBe(true)
  })

  it('handleOK：verifyArray_workbench 返回 errorIds 时不调用 updateEntryList', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    // 模拟 special 校验返回错误（id=entry-1）
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: 1, entryState: 2 },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue', auditState: 1, entryState: 2 },
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
    updateEntryList.mockResolvedValue({ data: { list: [], totalNum: 0 } })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: 'hello', english: 'hello', auditState: 1, entryState: 2 },
    ]
    wrapper.vm.editableData = {}
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: false }]

    await wrapper.vm.handleOK()
    await nextTick()

    expect(updateEntryList).toHaveBeenCalled()
  })

  it('handleOK：仅编辑翻译未设 auditState，点保存应被拦截（校验前置）', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    // 特殊字符不一致 -> 返回错误
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    // 行未设 auditState（不是 0 或 1），但在 editableData 中有编辑
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue' },
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
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: 1 },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue' },
    }
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: false }]

    await wrapper.vm.handleOK()
    await nextTick()

    // 校验失败，editableData 不应被清空
    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
  })

  it('handleOK：直接保存正确翻译时应先校验并回填到 dataSource', async () => {
    const { updateEntryList } = await import('@/http/api/workbench')
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press %1 to continue again' },
    }
    wrapper.vm.rulesOptions = [{ key: 'special', checked: true }, { key: 'toLong', checked: false }]

    await wrapper.vm.handleOK()
    await nextTick()

    expect(wrapper.vm.dataSource[0].english).toBe('Press %1 to continue again')
    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(updateEntryList).not.toHaveBeenCalled()
  })

  it('doubleClick：进入编辑态时不应主动触发表单校验或写红字', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    const record = { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue', auditState: undefined }
    wrapper.vm.dataSource = [record]

    const rowEvents = wrapper.vm.doubleClick(record, 0)
    rowEvents.onDblclick()
    await nextTick()

    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(wrapper.vm.cellErrors?.['entry-1']?.english).toBeUndefined()
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
    const record = { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined }
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
    const record = { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined, maxLength: 200 }
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
    updateEntryList.mockResolvedValue({ data: { list: [], totalNum: 0 } })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.dataSource = [
      { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: 1, entryState: 2, maxLength: 200 },
    ]
    wrapper.vm.editableData = {
      'entry-1': { id: 'entry-1', entry: '%1', english: 'Press % 1 to continue', auditState: 1, entryState: 2, maxLength: 200 },
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
    const record = { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined, entryState: 2, maxLength: 200 }
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
    const record = { id: 'entry-1', entry: '%1', english: 'Press %1 to continue', auditState: undefined, maxLength: 1 }
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

  it('先开 special 出红字再全关，行内 ✓ 应能保存', async () => {
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })

    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [
      { dataIndex: 'english', title: '英文' },
      { dataIndex: 'editOperation', title: '编辑操作' },
    ]
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: true },
      { key: 'toLong', checked: true },
    ]
    await nextTick()
    await flushPromises()
    const record = {
      id: 'entry-1',
      entry: '%1',
      english: 'Press % 1 to continue',
      auditState: undefined,
      maxLength: 200,
    }
    wrapper.vm.dataSource = [record]
    const rowEvents = wrapper.vm.doubleClick(record, 0)
    await rowEvents.onDblclick()
    await nextTick()
    wrapper.vm.cellErrors = { 'entry-1': { english: '特殊字符不一致' } }

    checkSykEntryBeforeSave.mockClear()
    wrapper.vm.rulesOptions = wrapper.vm.rulesOptions.map((o) => ({ ...o, checked: false }))
    await nextTick()
    await flushPromises()
    await vi.waitFor(() => {
      expect(wrapper.vm.cellErrors?.['entry-1']).toBeUndefined()
    })
    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
  })

  it('applyTable 应锁定单元格宽且表格带 table-cell-overflow', async () => {
    wrapper = mountWithTableStub()
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
    expect(wrapper.vm.$columnFilterPref?.lockCellSize).toBe(true)
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })
})
