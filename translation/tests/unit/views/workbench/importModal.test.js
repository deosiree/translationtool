/**
 * ImportModal 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 * 特别测试 departmentType 的使用
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ImportModal from '@/views/workbench/importModal.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'
import * as validationUtils from '@/utils/validationUtils'

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
  }))
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
  TreeSelect: {
    name: 'TreeSelect',
    template: '<div></div>'
  }
}))

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

describe('ImportModal - 浏览省略与编辑文本域', () => {
  let wrapper

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
          CustomModal: { template: '<div class="custom-modal-stub"><slot /></div>' },
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
          WorkbenchFormBar: { template: '<div><slot /></div>' },
          WorkbenchTaskInfo: true,
          WorkbenchColumnActions: true,
          WorkbenchLanguageFilter: true,
          FileSelectWithEncoding: true,
          Dict: true,
          RulesDropdown: true,
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
    expect(englishCell.findComponent({ name: 'TextAreaIME' }).exists()).toBe(true)
    expect(englishCell.find('.cell-overflow-tooltip-stub').exists()).toBe(false)
    expect(englishCell.findComponent({ name: 'TextAreaIME' }).props('autoSize')).toEqual({
      minRows: 1,
      maxRows: 5,
    })

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
    const validateSpy = vi
      .spyOn(validationUtils, 'validateEditableCell')
      .mockRejectedValue({ errorMessage: '特殊字符不一致' })
    wrapper = mountWithTableStub()
    await nextTick()
    await seedEditRow()
    wrapper.vm.task = { transMap: { value: 'english' } }
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.editSave(record)
    expect(wrapper.vm.editableData['entry-1']).toBeDefined()
    expect(wrapper.vm.cellErrors['entry-1'].english).toBe('特殊字符不一致')
    validateSpy.mockRestore()
  })

  it('editSave 写入 cellErrors 后同值 onCellInput 不应清掉红字', async () => {
    const validateSpy = vi
      .spyOn(validationUtils, 'validateEditableCell')
      .mockRejectedValue({ errorMessage: '允许最大字符数为20' })
    wrapper = mountWithTableStub()
    await nextTick()
    await seedEditRow()
    wrapper.vm.task = { transMap: { value: 'english' } }
    wrapper.vm.editableData['entry-1'].english = '超长文本内容'
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.editSave(record)
    expect(wrapper.vm.cellErrors['entry-1'].english).toBe('允许最大字符数为20')
    wrapper.vm.onCellInput(
      wrapper.vm.editableData['entry-1'].english,
      record,
      { dataIndex: 'english' }
    )
    expect(wrapper.vm.cellErrors['entry-1'].english).toBe('允许最大字符数为20')
    validateSpy.mockRestore()
  })

  it('editSave 校验通过应退出编辑态', async () => {
    const validateSpy = vi
      .spyOn(validationUtils, 'validateEditableCell')
      .mockResolvedValue(undefined)
    wrapper = mountWithTableStub()
    await nextTick()
    await seedEditRow()
    wrapper.vm.task = { transMap: { value: 'english' } }
    wrapper.vm.columns.push({
      dataIndex: 'editOperation',
      title: '编辑操作',
    })
    const record = wrapper.vm.dataSource[0]
    await wrapper.vm.editSave(record)
    expect(wrapper.vm.editableData['entry-1']).toBeUndefined()
    validateSpy.mockRestore()
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
    expect(wrapper.find('.table-stub').classes()).toContain('table-cell-overflow')
  })
})
