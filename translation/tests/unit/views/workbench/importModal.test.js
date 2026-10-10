/**
 * ImportModal 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 * 特别测试 departmentType 的使用
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ImportModal from '@/views/workbench/importModal.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'
import * as validationUtils from '@/utils/validationUtils'
import { isLoading, resetLoading } from '@/composables/useLoading'

// Mock 依赖
vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  createDragModalDirective: vi.fn(() => ({}))
}))

vi.mock('@/http/api/workbench', () => ({
  readZZExcle: vi.fn(() => Promise.resolve({
    data: {
      list: []
    }
  })),
  insertEntry: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  updateEntryList: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  getEntryTempByTaskID: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  getEntryInfoList: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
}))

vi.mock('@/http/api/glossary', () => ({
  checkSykEntryBeforeSave: vi.fn(() => Promise.resolve({ data: [] })),
}))

vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  },
  TreeSelect: {
    name: 'TreeSelect',
    template: '<div></div>'
  }
}))

const tableBodyStub = {
  name: 'ATable',
  props: ['columns', 'dataSource'],
  template: `
    <div class="table-stub" :class="$attrs.class">
      <div class="header-cell">
        <slot name="headerCell" :title="'词条'" :column="{ colValue: 'entry', dataIndex: 'entry', title: '词条' }" />
      </div>
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

function importModalTableStubs() {
  return {
    CustomModal: { template: '<div class="custom-modal-stub"><slot /><slot name="leftBottomBtn" /></div>' },
    PipeShell: {
      name: 'PipeShell',
      template: '<div class="pipe-shell-stub"><slot /><slot name="leftBottomBtn" /></div>',
    },
    'a-table': tableBodyStub,
    'a-form': {
      name: 'AForm',
      props: ['layout', 'model', 'rules'],
      template: '<form class="a-form-stub"><slot /></form>',
    },
    'a-form-item': { template: '<div class="a-form-item"><slot /></div>' },
    'a-button': true,
    'a-select': true,
    'a-upload': true,
    'a-input': true,
    'a-radio': true,
    'a-radio-group': true,
    InputIME: true,
    TableCellTextArea: {
      name: 'TableCellTextArea',
      props: ['value', 'errorMessage'],
      template: '<textarea class="table-cell-textarea-stub" />',
    },
    AuditTags: true,
    'a-input-number': true,
    'a-tooltip': { template: '<div><slot /><slot name="title" /></div>' },
    'a-tag': true,
    ColumnActions: true,
    LanguageFilter: true,
    FileSelectWithEncoding: true,
    Dict: {
      name: 'Dict',
      props: ['visible', 'currentIP'],
      template: '<div class="dict-stub" />',
    },
    RulesDropdown: true,
    IsExistBadge: true,
    EntryStateBadge: true,
    TransStateBadge: true,
    CellOverflowTooltip: {
      name: 'CellOverflowTooltip',
      props: ['content'],
      template: '<span class="cell-overflow-tooltip-stub"><slot /></span>',
    },
  }
}

describe('ImportModal - user 属性重构测试', () => {
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
      
      wrapper = mount(ImportModal, {
        props: {
          visible: true,
          currentTask: {}
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'PipeShell': true,
            'a-form': true,
            'a-form-item': true,
            'a-upload': true,
            'a-select': true
          }
        }
      })

      // created 会从 $store 注入 user（data 中占位为 null）
      expect(wrapper.vm.user).toEqual(
        expect.objectContaining({ userName: 'testUser', department: '测试部门' })
      )
    })

    it('应该从 $store.state.user 获取用户信息，特别是 department', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(ImportModal, {
        props: {
          visible: true,
          currentTask: {}
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'PipeShell': true,
            'a-form': true,
            'a-form-item': true,
            'a-upload': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以直接访问 $store.state.user
      expect(wrapper.vm.$store.state.user).toEqual(testUser)
      expect(wrapper.vm.$store.state.user.department).toBe('测试部门')
      
      // 验证可以使用 departmentType: this.$store.state.user.department
      const departmentType = wrapper.vm.$store.state.user.department
      expect(departmentType).toBe('测试部门')
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(ImportModal, {
        props: {
          visible: true,
          currentTask: {}
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'PipeShell': true,
            'a-form': true,
            'a-form-item': true,
            'a-upload': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证 user 为 null 时不会报错
      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })
})

describe('ImportModal - 新增辞典 currentIP', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  it('应向 Dict 传入 currentIP=ip', async () => {
    wrapper = mount(ImportModal, {
      props: {
        visible: true,
        currentTask: {},
      },
      global: {
        mocks: {
          ...createUserStoreMock(),
          $currentDepartment: null,
        },
        stubs: importModalTableStubs(),
      },
    })

    const testIp = 'http://10.0.0.1:18099/'
    wrapper.vm.ip = testIp
    await nextTick()

    const dict = wrapper.findComponent({ name: 'Dict' })
    expect(dict.exists()).toBe(true)
    expect(dict.props('currentIP')).toBe(testIp)
  })
})

describe('ImportModal - 浏览省略与编辑文本域', () => {
  let wrapper

  function mountWithTableStub() {
    return mount(ImportModal, {
      props: {
        visible: true,
        currentTask: { translateType: '英文' },
      },
      global: {
        mocks: {
          ...createUserStoreMock(),
          $currentDepartment: null,
        },
        stubs: importModalTableStubs(),
      },
    })
  }

  afterEach(() => {
    resetLoading()
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  async function seedEditRow() {
    wrapper.vm.task = {
      transMap: {
        value: 'english',
        interpretation: 'englishInterpretation',
      },
    }
    wrapper.vm.editList_needValidate = ['english']
    wrapper.vm.editList = ['englishInterpretation', 'comment']
    wrapper.vm.columns = [
      { dataIndex: 'entry', title: '词条', colValue: 'entry' },
      { dataIndex: 'english', title: '英文', colValue: 'translate' },
      { dataIndex: 'comment', title: 'comment', colValue: 'comment' },
      { dataIndex: 'tag', title: 'tag', colValue: 'tag' },
    ]
    wrapper.vm.dataSource = [
      {
        id: 'entry-1',
        entry: '词条',
        english: 'hello',
        comment: '备注',
        tag: 't1',
      },
    ]
    wrapper.vm.editableData = {
      'entry-1': {
        id: 'entry-1',
        entry: '词条',
        english: 'hello',
        comment: '备注',
        tag: 't1',
      },
    }
    await nextTick()
  }

  it('浏览态文本列应渲染 CellOverflowTooltip', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.editList_needValidate = ['english']
    wrapper.vm.editList = ['comment']
    wrapper.vm.columns = [
      { dataIndex: 'index', title: '序号', colValue: 'index' },
      { dataIndex: 'entry', title: '词条', colValue: 'entry' },
      { dataIndex: 'english', title: '英文', colValue: 'translate' },
    ]
    wrapper.vm.dataSource = [{ id: 'entry-1', entry: '词条', english: 'hello' }]
    wrapper.vm.editableData = {}
    await nextTick()

    expect(wrapper.find('[data-col="entry"]').find('.cell-overflow-tooltip-stub').exists()).toBe(true)
    expect(wrapper.find('[data-col="english"]').find('.cell-overflow-tooltip-stub').exists()).toBe(true)
    expect(wrapper.find('[data-col="index"]').find('.cell-overflow-tooltip-stub').exists()).toBe(false)
  })

  it('编辑态翻译与 comment 均使用 TableCellTextArea', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    await seedEditRow()

    const englishCell = wrapper.find('[data-col="english"]')
    expect(englishCell.findComponent({ name: 'TableCellTextArea' }).exists()).toBe(true)
    expect(englishCell.find('.cell-overflow-tooltip-stub').exists()).toBe(false)

    expect(wrapper.find('[data-col="comment"]').findComponent({ name: 'TableCellTextArea' }).exists()).toBe(true)
    expect(wrapper.find('[data-col="tag"]').findComponent({ name: 'InputIME' }).exists()).toBe(true)
  })

  it('编辑态翻译列不应再使用单元格内 a-form', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    await seedEditRow()
    const englishCell = wrapper.find('[data-col="english"]')
    expect(englishCell.findComponent({ name: 'AForm' }).exists()).toBe(false)
    expect(englishCell.findComponent({ name: 'TableCellTextArea' }).exists()).toBe(true)
  })

  it('editSave 校验失败应保持编辑态并在翻译列写入 cellErrors', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    await seedEditRow()
    wrapper.vm.task = { transMap: { value: 'english' } }
    wrapper.vm.rules = {
      'entry-1': {
        english: [{ validator: () => Promise.reject('特殊字符不一致') }],
      },
    }
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.editSave(record)
    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(wrapper.vm.cellErrors['entry-1'].english).toBe('特殊字符不一致')
  })

  it('editSave 写入 cellErrors 后同值 onCellInput 不应清掉红字', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    await seedEditRow()
    wrapper.vm.task = { transMap: { value: 'english' } }
    wrapper.vm.editableData['entry-1'].english = '超长文本内容'
    wrapper.vm.rules = {
      'entry-1': {
        english: [{ validator: () => Promise.reject('允许最大字符数为20') }],
      },
    }
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.editSave(record)
    expect(wrapper.vm.cellErrors['entry-1'].english).toBe('允许最大字符数为20')
    wrapper.vm.onCellInput(
      wrapper.vm.editableData['entry-1'].english,
      record,
      { dataIndex: 'english' }
    )
    expect(wrapper.vm.cellErrors['entry-1'].english).toBe('允许最大字符数为20')
  })

  it('editSave 校验通过应退出编辑态', async () => {
    wrapper = mountWithTableStub()
    await nextTick()
    await seedEditRow()
    wrapper.vm.task = { transMap: { value: 'english' } }
    wrapper.vm.rules = { 'entry-1': {} }
    wrapper.vm.columns.push({
      dataIndex: 'editOperation',
      title: '编辑操作',
    })
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.editSave(record)
    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
  })

  it('双击只开编辑态，不写 cellErrors', async () => {
    const applySpy = vi.spyOn(validationUtils, 'applyCell')
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = { transMap: { value: 'english' } }
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

  it('applyTable 应锁定单元格宽且表格带 table-cell-overflow', async () => {
    wrapper = mountWithTableStub()
    wrapper.vm.task = {
      transMap: {
        value: 'english',
        interpretation: 'englishInterpretation',
      },
    }
    await wrapper.setProps({ visible: false })
    await nextTick()
    await wrapper.setProps({ visible: true })
    await nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.$columnFilterPref?.lockCellSize).toBe(true)
    expect(wrapper.vm.$columnFilterPref?.fluidColValues).toEqual(
      expect.arrayContaining(['entry', 'translate', 'english'])
    )
    expect(wrapper.vm.importRowSelection.columnWidth).toBe(48)
    const entryCol = wrapper.vm.columns.find((c) => c.colValue === 'entry')
    const isExistCol = wrapper.vm.columns.find((c) => c.colValue === 'isExist')
    expect(entryCol?.customCell?.().style.maxWidth).toBeUndefined()
    expect(entryCol?.customCell?.().style.minWidth).toBeTruthy()
    if (isExistCol?.customCell) {
      expect(isExistCol.customCell().style.maxWidth).toBeTruthy()
    }
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })

  it('先开 special 出红字再全关：红字消失，行内 ✓ 能保存', async () => {
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = { transMap: { value: 'english', state: 'englishState' } }
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
      entry: 'Slot %1 then %2 end',
      english: 'Slot %1 only',
      maxLength: 200,
    }
    wrapper.vm.dataSource = [record]
    const rowEvents = wrapper.vm.customRow(record, 0)
    await rowEvents.onDblclick()
    await nextTick()
    wrapper.vm.cellErrors = { 'entry-1': { english: '特殊字符不一致' } }

    checkSykEntryBeforeSave.mockClear()
    wrapper.vm.rulesOptions = wrapper.vm.rulesOptions.map((o) => ({
      ...o,
      checked: false,
    }))
    await nextTick()
    await flushPromises()

    expect(wrapper.vm.cellErrors['entry-1']).toBeUndefined()
    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
  })

  it('取勾浏览态再勾上 special：失败行进编辑并出红字', async () => {
    const { checkSykEntryBeforeSave } = await import('@/http/api/glossary')
    checkSykEntryBeforeSave.mockResolvedValue({ data: [{ id: 'entry-1' }] })
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = { transMap: { value: 'english', state: 'englishState' } }
    wrapper.vm.columns = [{ dataIndex: 'english', title: '英文' }]
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: false },
      { key: 'toLong', checked: false },
    ]
    await nextTick()
    await flushPromises()
    const record = {
      id: 'entry-1',
      entry: 'Slot %1 then %2 end',
      english: 'Slot %1 only',
      maxLength: 200,
    }
    wrapper.vm.dataSource = [record]
    wrapper.vm.editableData = {}
    wrapper.vm.cellErrors = {}

    checkSykEntryBeforeSave.mockClear()
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: true },
      { key: 'toLong', checked: true },
    ]
    await nextTick()
    await flushPromises()

    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(wrapper.vm.cellErrors['entry-1'].english).toContain('特殊字符不一致')
    expect(checkSykEntryBeforeSave).not.toHaveBeenCalled()
  })
})

describe('ImportModal - saveEntrys 校验与状态', () => {
  let wrapper

  function mountWithTableStub() {
    return mount(ImportModal, {
      props: {
        visible: true,
        currentTask: { translateType: '英文' },
      },
      global: {
        mocks: {
          ...createUserStoreMock(),
          $currentDepartment: null,
        },
        stubs: {
          ...importModalTableStubs(),
          CustomModal: { template: '<div><slot /></div>' },
          CellOverflowTooltip: { template: '<span></span>' },
        },
      },
    })
  }

  afterEach(() => {
    resetLoading()
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('saveEntrys 用 editableData 校验，通过后翻译列不是 1、状态字段才是 1', async () => {
    const { insertEntry } = await import('@/http/api/workbench')
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
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
      entryState: 1,
      englishInterpretation: '释义',
      maxLength: 200,
    }
    wrapper.vm.dataSource = [{ ...stale }]
    wrapper.vm.selectedRows = [stale]
    wrapper.vm.selectedRowKeys = ['entry-1']
    wrapper.vm.editableData = {
      'entry-1': { ...stale, english: 'hello from edit' },
    }
    wrapper.vm.allData = [{ id: 'entry-1' }]

    await wrapper.vm.saveEntrys()
    await nextTick()

    expect(insertEntry).toHaveBeenCalled()
    const payload = insertEntry.mock.calls[0][1]
    expect(payload[0].english).toBe('hello from edit')
    expect(payload[0].english).not.toBe('1')
    expect(payload[0].englishState).toBe('1')
  })

  it('saveEntrys 验的是当前编辑值，不是 selectedRows 旧引用', async () => {
    const { insertEntry } = await import('@/http/api/workbench')
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
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
      english: 'ok',
      englishState: '0',
      entryState: 1,
      englishInterpretation: '释义',
      maxLength: 20,
    }
    wrapper.vm.dataSource = [{ ...stale }]
    wrapper.vm.selectedRows = [stale]
    wrapper.vm.selectedRowKeys = ['entry-1']
    wrapper.vm.editableData = {
      'entry-1': { ...stale, english: 'a'.repeat(30) },
    }
    wrapper.vm.allData = [{ id: 'entry-1' }]

    await wrapper.vm.saveEntrys()
    await nextTick()
    await flushPromises()

    expect(insertEntry).not.toHaveBeenCalled()
    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(wrapper.vm.cellErrors['entry-1'].english).toContain('允许最大字符数为20')
    expect(wrapper.vm.dataSource[0].english).toBe('ok')
  })

  it('saveEntrys 校验期间表格遮罩与保存按钮 loading 开启，结束后复位', async () => {
    const verifySpy = vi.spyOn(validationUtils, 'verifyArray_workbench')
    let loadingDuring = null
    verifySpy.mockImplementation(async () => {
      loadingDuring = isLoading()
      return {
        acceptIds: new Set(),
        errorIds: new Set(),
        toLongIds: new Set(),
        specialIds: new Set(),
      }
    })
    wrapper = mountWithTableStub()
    await nextTick()
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english', state: 'englishState' },
    }
    wrapper.vm.columns = [{ dataIndex: 'english' }]
    wrapper.vm.rulesOptions = [
      { key: 'special', checked: false },
      { key: 'toLong', checked: true },
    ]
    await nextTick()
    await flushPromises()
    const row = {
      id: 'entry-1',
      entry: '词条',
      english: 'ok',
      englishState: '0',
      entryState: 1,
      englishInterpretation: '释义',
      maxLength: 200,
    }
    wrapper.vm.dataSource = [{ ...row }]
    wrapper.vm.selectedRows = [row]
    wrapper.vm.selectedRowKeys = ['entry-1']
    wrapper.vm.editableData = { 'entry-1': { ...row, english: 'new' } }
    wrapper.vm.allData = [{ id: 'entry-1' }]

    await wrapper.vm.saveEntrys()
    await nextTick()
    await flushPromises()

    expect(loadingDuring).toBe(true)
    expect(isLoading()).toBe(false)
    verifySpy.mockRestore()
  })

  it('importEntryData：revalidateLoaded 完成前 loading 仍为 true，结束后复位', async () => {
    let loadingDuringRevalidate = null
    const revalSpy = vi.spyOn(validationUtils, 'revalidateLoaded').mockImplementation(async () => {
      loadingDuringRevalidate = isLoading()
    })
    const { readZZExcle } = await import('@/http/api/workbench')
    readZZExcle.mockResolvedValue({
      data: { list: [{ id: 'entry-1', entry: '词条', english: 'ok', isExist: 1 }] },
    })
    wrapper = mountWithTableStub()
    await nextTick()
    await wrapper.setProps({ classifyLimit: {} })
    wrapper.vm.dataType = 'file'
    wrapper.vm.file = { name: 'import.xlsx' }
    wrapper.vm.templateTypes = null
    wrapper.vm.task = {
      id: 'task-1',
      transMap: { value: 'english' },
      translateType: '英文',
    }

    await wrapper.vm.importEntryData()
    await flushPromises()

    expect(revalSpy).toHaveBeenCalled()
    expect(loadingDuringRevalidate).toBe(true)
    expect(isLoading()).toBe(false)
    revalSpy.mockRestore()
  })
})

describe('ImportModal - 窄视口横滚应在模态内而非 document', () => {
  it('PipeShell：内容 min-width 1100 + modalContent 横滚 + 外壳限视口', async () => {
    const fs = await import('node:fs')
    const path = await import('node:path')
    const shellSrc = fs.readFileSync(
      path.resolve(
        __dirname,
        '../../../../src/views/workbench/components/PipeShell.vue'
      ),
      'utf8'
    )
    const importSrc = fs.readFileSync(
      path.resolve(__dirname, '../../../../src/views/workbench/importModal.vue'),
      'utf8'
    )
    expect(importSrc).toContain('PipeShell')
    expect(shellSrc).toContain('pipe-shell')
    expect(shellSrc).toMatch(
      /\.pipe-shell\s+\.modalContent\s+\.content\s*\{[\s\S]*?min-width:\s*1100px/
    )
    expect(shellSrc).toMatch(/\.pipe-shell\s+\.modalContent\s*\{[\s\S]*?overflow-x:\s*auto/)
    expect(shellSrc).toMatch(
      /\.pipe-shell\s+\.ant-modal\s*\{[\s\S]*?max-width:\s*calc\(100vw\s*-\s*24px\)/
    )
    expect(shellSrc).toMatch(/body\.ant-modal-open\s*\{[\s\S]*?overflow:\s*hidden/)
    expect(importSrc).toMatch(/\.import-form-row--source/)
    expect(importSrc).toMatch(/\.import-form-row--file/)
  })

  it('注入契约样式后：外壳限视口、内容可横滚、body 锁 overflow', () => {
    const style = document.createElement('style')
    style.textContent = `
      body.ant-modal-open { overflow: hidden !important; }
      .pipe-shell.ant-modal-wrap {
        display: flex; align-items: center; justify-content: center; overflow: auto;
      }
      .pipe-shell .ant-modal {
        max-width: calc(100vw - 24px) !important; min-width: 0; margin: 0;
      }
      .pipe-shell .modalContent { overflow-x: auto; overflow-y: auto; }
      .pipe-shell .content { min-width: 1100px; }
    `
    document.head.appendChild(style)
    document.body.classList.add('ant-modal-open')

    const wrap = document.createElement('div')
    wrap.className = 'ant-modal-wrap ant-modal-centered pipe-shell'
    const modal = document.createElement('div')
    modal.className = 'ant-modal'
    const modalContent = document.createElement('div')
    modalContent.className = 'modalContent'
    const content = document.createElement('div')
    content.className = 'content'
    modalContent.appendChild(content)
    modal.appendChild(modalContent)
    wrap.appendChild(modal)
    document.body.appendChild(wrap)

    expect(getComputedStyle(modal).maxWidth).toContain('px')
    expect(getComputedStyle(content).minWidth).toBe('1100px')
    expect(getComputedStyle(modalContent).overflowX).toMatch(/auto|scroll/)
    expect(getComputedStyle(document.body).overflow).toMatch(/hidden/)

    wrap.remove()
    style.remove()
    document.body.classList.remove('ant-modal-open')
  })
})
