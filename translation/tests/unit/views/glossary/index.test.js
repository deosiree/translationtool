import { describe, it, expect, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import GlossaryIndex from '@/views/glossary/index.vue'
import { createUserStoreMock } from '../../testUtils/userStoreMock'

vi.mock('@/utils/domUtils', () => ({
  clickInput: vi.fn(),
  setModalAriaHidden: vi.fn(),
}))

vi.mock('@/http/api/glossary', () => ({
  getSykEntry: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  checkSameEntry: vi.fn(),
  getSykNotUsed: vi.fn(),
  checkSykEntry: vi.fn(),
  updateSykEntry: vi.fn(),
  getSykEntryRelation: vi.fn(),
  getSameEntryRelation: vi.fn(),
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))

vi.mock('@/http/api/userPartiality', () => ({
  updateUserPartiality: vi.fn(),
}))

vi.mock('@/utils/tableUtils', () => ({
  setTableHeight: vi.fn(),
  handleResizeColumn: vi.fn(),
  getRowClassName: vi.fn(),
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

describe('Glossary - 浏览省略与编辑文本域', () => {
  let wrapper

  function mountWithTableStub() {
    return mount(GlossaryIndex, {
      global: {
        mocks: createUserStoreMock(),
        stubs: {
          SearchBox: { template: '<div><slot name="form" /></div>' },
          DataBox: { template: '<div><slot name="operate" /><slot name="data" /></div>' },
          'a-table': tableBodyStub,
          'a-form': { template: '<form><slot /></form>' },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-button': true,
          'a-input': true,
          'a-select': true,
          ResetButton: true,
          BatchSelectButton: true,
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
        },
      },
    })
  }

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('浏览 translate 有 Tooltip；编辑为 TextArea；operation 无 Tooltip', async () => {
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
    expect(translateBrowse.find('.cell-overflow-tooltip-stub').exists()).toBe(true)
    expect(wrapper.find('[data-col="translateState"]').find('.cell-overflow-tooltip-stub').exists()).toBe(true)
    expect(wrapper.find('[data-col="operation"]').find('.cell-overflow-tooltip-stub').exists()).toBe(false)
    expect(wrapper.find('[data-col="index"]').find('.cell-overflow-tooltip-stub').exists()).toBe(false)

    wrapper.vm.editableData = { 'g-1': { id: 'g-1', translate: 'hello' } }
    await nextTick()
    const translateEdit = wrapper.find('[data-col="translate"]')
    expect(translateEdit.exists()).toBe(true)
    expect(translateEdit.findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(translateEdit.find('.cell-overflow-tooltip-stub').exists()).toBe(false)
    expect(wrapper.find('[data-col="translateState"]').find('.trans-state-badge-stub').exists()).toBe(true)
  })

  it('applyTable 应锁定单元格宽且表格带 table-cell-overflow', async () => {
    wrapper = mountWithTableStub()
    await flushPromises()
    expect(wrapper.vm.$columnFilterPref?.lockCellSize).toBe(true)
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })
})
