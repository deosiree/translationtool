import { describe, expect, it, vi } from 'vitest'
import { getEntryInfoList } from '@/http/api/workbench'
import ArchiveModal from '@/views/workbench/archiveModal.vue'
import ExamineModal from '@/views/workbench/examineModal.vue'
import ExamineTranslateModal from '@/views/workbench/examineTranslateModal.vue'
import ImportModal from '@/views/workbench/importModal.vue'
import TranslateModal from '@/views/workbench/translateModal.vue'
import { createQueryVersionGuard } from '@/utils/queryVersionGuard'

vi.mock('@/http/api/workbench', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, getEntryInfoList: vi.fn() }
})

const selectionTables = [
  { name: '归档', component: ArchiveModal, selection: 'archiveRowSelection' },
  { name: '词条审核', component: ExamineModal, selection: 'examineRowSelection' },
  { name: '翻译审核', component: ExamineTranslateModal, selection: 'examineTranslateRowSelection' },
  { name: '导入', component: ImportModal, selection: 'importRowSelection' },
  { name: '翻译', component: TranslateModal, selection: 'translateRowSelection' },
]

/**
 * 创建弹窗表格选择回调所需的最小组件上下文。
 * @param {Object} component - 弹窗组件定义。
 * @returns {Object} 带当前过滤范围和初始选择的测试上下文。
 */
function createSelectionContext(component) {
  const vm = {
    selectedRows: [
      { id: 'outside', group: 'other' },
      { id: 'active-1', group: 'active', value: 'old' },
      { id: 'inactive', group: 'inactive' },
    ],
    selectedRowKeys: ['outside', 'active-1', 'inactive'],
    dataSource: [
      { id: 'active-1', group: 'active', value: 'new' },
      { id: 'active-2', group: 'active' },
      { id: 'inactive', group: 'inactive' },
    ],
    filters: { group: ['active'] },
  }
  vm.onSelect = component.methods.onSelect.bind(vm)
  vm.onSelectAll = component.methods.onSelectAll.bind(vm)
  return vm
}

describe.each(selectionTables)('$name 表格全量选择', ({ component, selection }) => {
  it('表头使用原生选择回调，取勾只移除当前筛选结果', () => {
    const vm = createSelectionContext(component)
    const rowSelection = component.computed[selection].call(vm)

    expect(rowSelection.onSelect).toBeTypeOf('function')
    expect(rowSelection.onSelectAll).toBeTypeOf('function')
    expect(rowSelection.onChange).toBeUndefined()
    expect(rowSelection.selections).toBeUndefined()

    rowSelection.onSelectAll(false)

    expect(vm.selectedRowKeys).toEqual(['outside', 'inactive'])
    expect(vm.selectedRows).toEqual([
      { id: 'outside', group: 'other' },
      { id: 'inactive', group: 'inactive' },
    ])
  })

  it('表头全选当前完整筛选结果并用新查询对象覆盖同 ID 缓存', () => {
    const vm = createSelectionContext(component)
    const rowSelection = component.computed[selection].call(vm)

    rowSelection.onSelectAll(true)

    expect(vm.selectedRowKeys).toEqual(['outside', 'active-1', 'inactive', 'active-2'])
    expect(vm.selectedRows.find((row) => row.id === 'active-1').value).toBe('new')
  })
})

describe('工作台弹窗列表竞态', () => {
  const taskSwitchTables = [
    { name: '归档', component: ArchiveModal },
    { name: '词条审核', component: ExamineModal },
    { name: '翻译审核', component: ExamineTranslateModal },
    { name: '导入', component: ImportModal },
    { name: '翻译', component: TranslateModal },
  ]

  it.each(taskSwitchTables)('$name 切换任务时清空旧行与选择并使旧列表失效', ({ component }) => {
    const queryGuard = createQueryVersionGuard()
    const importGuard = createQueryVersionGuard()
    const oldToken = queryGuard.next()
    const oldImportToken = importGuard.next()
    const oldTask = { id: 'task-old', translateType: '英文', department: '部门' }
    const vm = {
      task: oldTask,
      visible: false,
      queryGuard,
      importGuard,
      queryScope: 'old-query',
      queryLoading: true,
      selectedRows: [{ id: 'old-row' }],
      selectedRowKeys: ['old-row'],
      dataSource: [{ id: 'old-row' }],
      allData: [{ id: 'old-row' }],
      search: {},
      getTaskEntry: vi.fn(),
      getTranslateEntry: vi.fn(),
      initTaskEntry: vi.fn(),
    }

    component.watch.currentTask.call(
      vm,
      { ...oldTask, id: 'task-new' },
      oldTask,
    )

    expect(queryGuard.isCurrent(oldToken)).toBe(false)
    expect(vm.dataSource).toEqual([])
    expect(vm.allData).toEqual([])
    expect(vm.selectedRows).toEqual([])
    expect(vm.selectedRowKeys).toEqual([])
    expect(vm.queryLoading).toBe(false)
    if (component === ImportModal) {
      expect(importGuard.isCurrent(oldImportToken)).toBe(false)
    }
  })

  it('任务切换后忽略旧任务的迟到列表响应', async () => {
    const vm = {
      task: { id: 'task-old' },
      keyWords: '',
      translateState: null,
      queryGuard: createQueryVersionGuard(),
      queryScope: '',
      selectedRows: [],
      selectedRowKeys: [],
      dataSource: [],
      filters: {},
    }
    vm.getTaskEntry = ArchiveModal.methods.getTaskEntry.bind(vm)
    let resolveOld
    let resolveNew
    getEntryInfoList.mockReset()
    getEntryInfoList.mockImplementation(({ taskID }) => new Promise((resolve) => {
      if (taskID === 'task-old') resolveOld = resolve
      else resolveNew = resolve
    }))

    const oldRequest = vm.getTaskEntry()
    vm.task = { id: 'task-new' }
    const newRequest = vm.getTaskEntry()
    resolveNew({ data: { list: [{ id: 'new-row' }] } })
    await newRequest
    resolveOld({ data: { list: [{ id: 'old-row' }] } })
    await oldRequest

    expect(vm.dataSource).toEqual([{ id: 'new-row' }])
  })

  it('关闭后重开时旧请求结束不得提前关闭新列表 loading', async () => {
    const vm = {
      task: { id: 'task-old' },
      keyWords: '',
      entryState: null,
      translateState: null,
      queryGuard: createQueryVersionGuard(),
      queryScope: '',
      queryLoading: false,
      selectedRows: [],
      selectedRowKeys: [],
      dataSource: [],
      filters: null,
    }
    vm.getTaskEntry = ArchiveModal.methods.getTaskEntry.bind(vm)
    let resolveOld
    let resolveNew
    getEntryInfoList.mockReset()
    getEntryInfoList.mockImplementation(({ taskID }) => new Promise((resolve) => {
      if (taskID === 'task-old') resolveOld = resolve
      else resolveNew = resolve
    }))

    const oldRequest = vm.getTaskEntry()
    expect(vm.queryLoading).toBe(true)
    await ArchiveModal.watch.visible.handler.call(vm, false)
    vm.task = { id: 'task-new' }
    const newRequest = vm.getTaskEntry()

    resolveOld({ data: { list: [{ id: 'old-row' }] } })
    await oldRequest
    expect(vm.queryLoading).toBe(true)
    expect(vm.dataSource).toEqual([])

    resolveNew({ data: { list: [{ id: 'new-row' }] } })
    await newRequest

    expect(vm.queryLoading).toBe(false)
    expect(vm.dataSource).toEqual([{ id: 'new-row' }])
  })

  it('归档服务端筛选范围变化后保留旧范围勾选', async () => {
    const vm = {
      task: { id: 'task-1' },
      keyWords: 'new keyword',
      entryState: null,
      translateState: null,
      queryGuard: createQueryVersionGuard(),
      queryScope: 'old-scope',
      queryLoading: false,
      selectedRows: [{ id: 'old-selected' }],
      selectedRowKeys: ['old-selected'],
      dataSource: [{ id: 'old-row' }],
      allData: [{ id: 'old-row' }],
      filters: null,
    }
    vm.getTaskEntry = ArchiveModal.methods.getTaskEntry.bind(vm)
    getEntryInfoList.mockReset()
    getEntryInfoList.mockResolvedValue({ data: { list: [{ id: 'new-row' }] } })

    await vm.getTaskEntry()

    expect(vm.dataSource).toEqual([{ id: 'new-row' }])
    expect(vm.allData).toEqual([{ id: 'new-row' }])
    expect(vm.selectedRows).toEqual([{ id: 'old-selected' }])
    expect(vm.selectedRowKeys).toEqual(['old-selected'])
  })

  it.each(taskSwitchTables)('$name 关闭弹窗时清空旧查询会话', async ({ component }) => {
    const queryGuard = createQueryVersionGuard()
    const importGuard = createQueryVersionGuard()
    const oldQueryToken = queryGuard.next()
    const oldImportToken = importGuard.next()
    const vm = {
      queryGuard,
      importGuard,
      queryLoading: true,
      queryScope: 'old-scope',
      dataSource: [{ id: 'old-row' }],
      allData: [{ id: 'old-row' }],
      selectedRows: [{ id: 'old-row' }],
      selectedRowKeys: ['old-row'],
      selectedRowIndex: 'old-row',
      editableData: { 'old-row': { value: 'draft' } },
      cellErrors: { 'old-row': { value: 'invalid' } },
    }

    await component.watch.visible.handler.call(vm, false)

    expect(queryGuard.isCurrent(oldQueryToken)).toBe(false)
    expect(vm.queryScope).toBe('')
    expect(vm.queryLoading).toBe(false)
    expect(vm.dataSource).toEqual([])
    expect(vm.allData).toEqual([])
    expect(vm.selectedRows).toEqual([])
    expect(vm.selectedRowKeys).toEqual([])
    expect(vm.selectedRowIndex).toBeNull()
    expect(vm.editableData).toEqual({})
    expect(vm.cellErrors).toEqual({})
    if (component === ImportModal) {
      expect(importGuard.isCurrent(oldImportToken)).toBe(false)
    }
  })

  it('关闭翻译弹窗时重置翻译专属异常状态', () => {
    const queryGuard = createQueryVersionGuard()
    const oldToken = queryGuard.next()
    const vm = {
      queryGuard,
      queryLoading: true,
      queryScope: 'old-query',
      dataSource: [{ id: 'old-row' }],
      allData: [{ id: 'old-row' }],
      selectedRows: [{ id: 'old-row' }],
      selectedRowKeys: ['old-row'],
      selectedRowIndex: 'old-row',
      editableData: { 'old-row': { value: 'draft' } },
      selectedArr: {
        updateArr: [{ id: 'old-row' }],
        noTranslateIds: new Set(['old-row']),
        toLongIds: new Set(['old-row']),
        specialIds: new Set(['old-row']),
        redHighlightIds: new Set(['old-row']),
      },
      $emit: vi.fn(),
    }

    TranslateModal.methods.handleClose.call(vm)

    expect(queryGuard.isCurrent(oldToken)).toBe(false)
    expect(vm.queryScope).toBe('')
    expect(vm.queryLoading).toBe(false)
    expect(vm.dataSource).toEqual([])
    expect(vm.allData).toEqual([])
    expect(vm.selectedRows).toEqual([])
    expect(vm.selectedRowKeys).toEqual([])
    expect(vm.selectedRowIndex).toBeNull()
    expect(vm.editableData).toEqual({})
    expect(vm.selectedArr.updateArr).toEqual([])
    expect(vm.selectedArr.noTranslateIds.size).toBe(0)
    expect(vm.selectedArr.toLongIds.size).toBe(0)
    expect(vm.selectedArr.specialIds.size).toBe(0)
    expect(vm.selectedArr.redHighlightIds.size).toBe(0)
    expect(vm.$emit).toHaveBeenCalledWith('handleClose')
  })
})
