import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import TranslateModal from '@/views/workbench/translateModal.vue'
import { createUserStoreMock } from '../../testUtils/userStoreMock'

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
          'a-row': { template: '<div><slot /></div>' },
          'a-col': { template: '<div><slot /></div>' },
          WorkbenchFormBar: { template: '<div><slot /></div>' },
          WorkbenchTaskInfo: { template: '<div><slot name="extra" /></div>' },
          WorkbenchColumnActions: true,
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

  afterEach(() => {
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

  it('applyTable 应锁定单元格宽且表格带 table-cell-overflow', async () => {
    wrapper = mountWithTableStub()
    wrapper.vm.language = { value: 'english' }
    wrapper.vm.task = { transMap: { value: 'english' } }
    await wrapper.setProps({ visible: false })
    await nextTick()
    await wrapper.setProps({ visible: true })
    await nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.$columnFilterPref?.lockCellSize).toBe(true)
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })
})
