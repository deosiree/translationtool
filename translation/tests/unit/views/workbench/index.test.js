/**
 * Workbench Index 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import WorkbenchIndex from '@/views/workbench/index.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'
import { getFinishTaskInfo, getTaskPending, getToDoTaskInfo, updateTaskInfo } from '@/http/api/task'

// Mock 依赖
vi.mock('@/http/api/task', () => ({
  getToDoTaskInfo: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  getFinishTaskInfo: vi.fn(() => Promise.resolve({ data: { list: [], totalNum: 0 } })),
  updateTaskInfo: vi.fn(() => Promise.resolve({ data: {} })),
  getTaskPending: vi.fn(() => Promise.resolve({ data: [] })),
}))

vi.mock('@/http/api/workbench', () => ({
  searchTaskInfo: vi.fn(() => Promise.resolve({
    data: {
      list: [],
      totalNum: 0
    }
  })),
  getEntryInfoList: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({
    data: {
      list: []
    }
  }))
}))

vi.mock('@/utils/tableUtils', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    setTableHeight: vi.fn(),
  }
})

vi.mock('@/views/workbench/components/BatchProgressOverlay.vue', () => ({
  default: { name: 'BatchProgressOverlayStub', template: '<div />' },
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

describe('Workbench Index - user 属性重构测试', () => {
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
      
      wrapper = mount(WorkbenchIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      // 验证组件实例不应该有 user 属性
      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(WorkbenchIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以直接访问 $store.state.user
      expect(wrapper.vm.$store.state.user).toEqual(testUser)
      expect(wrapper.vm.$store.state.user.userName).toBe('testUser')
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(WorkbenchIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
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

const tableStubs = {
  'a-table': true,
  'a-button': true,
  'a-form': true,
  'a-form-item': true,
  'a-input': true,
  'a-select': true,
  'a-modal': true,
  'a-badge': true,
  'a-card': true,
  SearchBox: true,
  DataBox: true,
  OperationArea: true,
  TimeLine: true,
  ImportModal: true,
  ExamineModal: true,
  TranslateModal: true,
  ExamineTranslateModal: true,
  ArchiveModal: true,
  TableViewport: true,
  SelectionToolbarButton: true,
  BatchProgressOverlay: { template: '<div />' },
}

function pendingResponse(countsById) {
  return {
    data: Object.entries(countsById).map(([id, totalCounts]) => ({
      taskID: Number(id),
      totalCounts,
      entryExamineCounts: 0,
      importNum: 0,
      translateCounts: 0,
      translateExamineCounts: 0,
    })),
  }
}

describe('Workbench Index - 更改翻译语种后批量刷新小红点', () => {
  let wrapper

  async function mountWorkbench() {
    wrapper = mount(WorkbenchIndex, {
      global: {
        mocks: createUserStoreMock(),
        stubs: tableStubs,
      },
    })
    await nextTick()
    await wrapper.vm.$nextTick()
    // 等 mounted 里 getTaskByCondition 结束，避免覆盖测试里写入的 dataSource
    await new Promise((resolve) => setTimeout(resolve, 0))
    await nextTick()
    vi.clearAllMocks()
    updateTaskInfo.mockResolvedValue({ data: {} })
  }

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
      wrapper = null
    }
    vi.clearAllMocks()
  })

  it('平铺多选改语种后只调用一次 getTaskPending，入参为全部成功任务 id', async () => {
    await mountWorkbench()
    const taskA = { id: 11, name: '任务A', translateType: '英文', num__total: 1 }
    const taskB = { id: 12, name: '任务B', translateType: '英文', num__total: 2 }
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.dataSource = [taskA, taskB]
    wrapper.vm.selectedRows = [taskA, taskB]
    wrapper.vm.selectedLanguage = '俄文'
    getTaskPending.mockResolvedValue(pendingResponse({ 11: 5, 12: 3 }))

    await wrapper.vm.confirmTranslateType()

    expect(updateTaskInfo).toHaveBeenCalledTimes(2)
    expect(getTaskPending).toHaveBeenCalledTimes(1)
    expect(getTaskPending).toHaveBeenCalledWith([11, 12])
    expect(wrapper.vm.dataSource[0].num__total).toBe(5)
    expect(wrapper.vm.dataSource[1].num__total).toBe(3)
  })

  it('层级多选改语种后只调用一次 getTaskPending，并更新分支合计', async () => {
    await mountWorkbench()
    const taskA = { id: 21, name: '任务A', translateType: '英文', num__total: 1 }
    const taskB = { id: 22, name: '任务B', translateType: '英文', num__total: 2 }
    wrapper.vm.isTreeOr2D = 'tree'
    wrapper.vm.dataSource = [
      {
        id: 'branch_dev',
        isBranch: true,
        child: [taskA, taskB],
        num__total: 3,
      },
    ]
    wrapper.vm.selectedRows = [taskA, taskB]
    wrapper.vm.selectedLanguage = '俄文'
    getTaskPending.mockResolvedValue(pendingResponse({ 21: 4, 22: 6 }))

    await wrapper.vm.confirmTranslateType()

    expect(getTaskPending).toHaveBeenCalledTimes(1)
    expect(getTaskPending).toHaveBeenCalledWith([21, 22])
    expect(wrapper.vm.dataSource[0].child[0].num__total).toBe(4)
    expect(wrapper.vm.dataSource[0].child[1].num__total).toBe(6)
    expect(wrapper.vm.dataSource[0].num__total).toBe(10)
  })

  it('已是目标语种时不调用 getTaskPending', async () => {
    await mountWorkbench()
    const taskA = { id: 31, name: '任务A', translateType: '俄文', num__total: 1 }
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.dataSource = [taskA]
    wrapper.vm.selectedRows = [taskA]
    wrapper.vm.selectedLanguage = '俄文'

    await wrapper.vm.confirmTranslateType()

    expect(updateTaskInfo).not.toHaveBeenCalled()
    expect(getTaskPending).not.toHaveBeenCalled()
  })

  it('改语种后勾选保留，已选数组与 keys 等长，value 为 pending 后的新引用', async () => {
    await mountWorkbench()
    const taskA = { id: 41, name: '任务A', translateType: '英文', num__total: 1 }
    const taskB = { id: 42, name: '任务B', translateType: '英文', num__total: 2 }
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.dataSource = [taskA, taskB]
    wrapper.vm.selectEntry = [taskA, taskB]
    wrapper.vm.syncSelection()
    wrapper.vm.selectedLanguage = '俄文'
    getTaskPending.mockResolvedValue(pendingResponse({ 41: 7, 42: 9 }))

    await wrapper.vm.confirmTranslateType()

    expect(wrapper.vm.selectEntry.length).toBe(wrapper.vm.selectedRowKeys.length)
    expect(wrapper.vm.selectedRowKeys).toEqual([41, 42])
    expect(wrapper.vm.selectEntry.find((task) => task.id === 41).num__total).toBe(7)
    expect(wrapper.vm.selectEntry.find((task) => task.id === 42).num__total).toBe(9)
    expect(wrapper.vm.selectedRows[0].num__total).toBe(7)
  })

  it('批量改语种仅更新成功任务，失败任务保留原始已选对象', async () => {
    await mountWorkbench()
    const failedTask = { id: 43, name: '失败任务', translateType: '英文', num__total: 1 }
    const savedTask = { id: 44, name: '成功任务', translateType: '英文', num__total: 2 }
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.dataSource = [failedTask, savedTask]
    wrapper.vm.selectEntry = [failedTask, savedTask]
    wrapper.vm.syncSelection()
    wrapper.vm.selectedLanguage = '俄文'
    updateTaskInfo.mockImplementation((task) =>
      task.id === failedTask.id
        ? Promise.reject(new Error('保存失败'))
        : Promise.resolve({ data: {} }),
    )
    getTaskPending.mockResolvedValue(pendingResponse({ [savedTask.id]: 5 }))

    await wrapper.vm.confirmTranslateType()

    expect(failedTask.translateType).toBe('英文')
    expect(wrapper.vm.selectEntry.find((task) => task.id === failedTask.id)).toEqual(failedTask)
    expect(wrapper.vm.dataSource.find((task) => task.id === failedTask.id)).toEqual(failedTask)
    expect(wrapper.vm.selectEntry.find((task) => task.id === savedTask.id).translateType).toBe('俄文')
    expect(wrapper.vm.dataSource.find((task) => task.id === savedTask.id).translateType).toBe('俄文')
    expect(getTaskPending).toHaveBeenCalledWith([savedTask.id])
  })

  it('改语种后再勾新行，旧勾选不丢', async () => {
    await mountWorkbench()
    const taskA = { id: 51, name: '任务A', translateType: '英文', num__total: 1 }
    const taskB = { id: 52, name: '任务B', translateType: '英文', num__total: 2 }
    const taskC = { id: 53, name: '任务C', translateType: '英文', num__total: 0 }
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.dataSource = [taskA, taskB, taskC]
    wrapper.vm.selectEntry = [taskA, taskB]
    wrapper.vm.syncSelection()
    wrapper.vm.selectedLanguage = '俄文'
    getTaskPending.mockResolvedValue(pendingResponse({ 51: 2, 52: 3 }))

    await wrapper.vm.confirmTranslateType()
    wrapper.vm.onSelect(taskC, true)

    expect(wrapper.vm.selectedRowKeys).toEqual(expect.arrayContaining([51, 52, 53]))
    expect(wrapper.vm.selectedRowKeys).toHaveLength(3)
    expect(wrapper.vm.selectEntry.length).toBe(3)
  })

  it('clearAllEntry 后 Map 与 keys 都空', async () => {
    await mountWorkbench()
    const taskA = { id: 61, name: '任务A', translateType: '英文', num__total: 1 }
    wrapper.vm.selectEntry = [taskA]
    wrapper.vm.syncSelection()

    wrapper.vm.clearAllEntry()

    expect(wrapper.vm.selectEntry.length).toBe(0)
    expect(wrapper.vm.selectedRowKeys).toEqual([])
    expect(wrapper.vm.selectedRows).toEqual([])
  })

  it('旧任务列表响应晚于新响应时不得覆盖新表格', async () => {
    await mountWorkbench()
    wrapper.vm.isTreeOr2D = '2D'
    let resolveOld
    let resolveNew
    getToDoTaskInfo
      .mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve }))
      .mockImplementationOnce(() => new Promise((resolve) => { resolveNew = resolve }))

    const oldRequest = wrapper.vm.getTaskByCondition({ name: 'old' })
    const newRequest = wrapper.vm.getTaskByCondition({ name: 'new' })
    resolveNew({ data: { list: [{ id: 2, name: 'new' }], totalNum: 1 } })
    await newRequest
    resolveOld({ data: { list: [{ id: 1, name: 'old' }], totalNum: 1 } })
    await oldRequest

    expect(wrapper.vm.dataSource.map((task) => task.id)).toEqual([2])
  })

  it('全选请求期间翻页时立即投影新页选择并最终收齐所有页', async () => {
    await mountWorkbench()
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.activeCard = 1
    wrapper.vm.selectionScope = 'todo-query'
    wrapper.vm.activeQueryParams = { name: 'same-query' }
    wrapper.vm.pagination.pageSize = 20
    await nextTick()
    wrapper.vm.dataSource = [{ id: 1, name: 'page-1' }]

    let resolveFirstPage
    getToDoTaskInfo
      .mockImplementationOnce(() => new Promise((resolve) => {
        resolveFirstPage = resolve
      }))
      .mockResolvedValueOnce({
        data: { totalNum: 101, list: [{ id: 101, name: 'page-6' }] },
      })

    const pendingSelection = wrapper.vm.onSelectAll(true)
    expect(wrapper.vm.tableSelectedRowKeys).toContain(1)

    wrapper.vm.pagination.current = 6
    wrapper.vm.dataSource = [{ id: 101, name: 'page-6' }]
    expect(wrapper.vm.tableSelectedRowKeys).toEqual([101])

    resolveFirstPage({
      data: {
        totalNum: 101,
        list: Array.from({ length: 100 }, (_, index) => ({ id: index + 1 })),
      },
    })
    await pendingSelection

    expect(wrapper.vm.selectEntry.length).toBe(101)
    expect(wrapper.vm.selectedRowKeys).toHaveLength(101)
    expect(new Set(wrapper.vm.selectedRowKeys).size).toBe(101)
  })

  it('真实翻页查询不清空同一查询范围的全选投影', async () => {
    await mountWorkbench()
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.activeCard = 1
    const query = { name: 'same-query' }
    wrapper.vm.pageChangeSearch = query
    wrapper.vm.activeQueryParams = query
    wrapper.vm.selectionScope = JSON.stringify({ activeCard: 1, query })
    wrapper.vm.dataSource = [{ id: 1, name: 'page-1' }]
    await nextTick()

    let resolveSelectionPage
    getToDoTaskInfo
      .mockImplementationOnce(() => new Promise((resolve) => {
        resolveSelectionPage = resolve
      }))
      .mockResolvedValueOnce({
        data: { totalNum: 2, list: [{ id: 2, name: 'page-2' }] },
      })

    const pendingSelection = wrapper.vm.onSelectAll(true)
    expect(wrapper.vm.tableSelectedRowKeys).toContain(1)

    wrapper.vm.pageChange(2, 20)
    await flushPromises()
    expect(wrapper.vm.selectionScope).toBe(JSON.stringify({ activeCard: 1, query }))
    expect(wrapper.vm.tableSelectedRowKeys).toContain(2)

    resolveSelectionPage({
      data: { totalNum: 1, list: [{ id: 1, name: 'page-1' }] },
    })
    await pendingSelection
  })

  it('全量选择后调整每页条数并翻页，新页行仍保持勾选', async () => {
    await mountWorkbench()
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.activeCard = 1
    const query = { name: 'same-query' }
    const allTasks = Array.from({ length: 76 }, (_, index) => ({
      id: index + 1,
      name: `task-${index + 1}`,
    }))
    getToDoTaskInfo.mockImplementation(async ({ pageIndex, pageSize }) => {
      const effectivePageSize = Math.min(pageSize, 20)
      const start = (pageIndex - 1) * effectivePageSize
      return {
        data: {
          totalNum: allTasks.length,
          list: allTasks.slice(start, start + effectivePageSize),
        },
      }
    })
    await wrapper.vm.getTaskByCondition(query)
    wrapper.vm.pageChangeSearch = query

    await wrapper.vm.onSelectAll(true)

    expect(wrapper.vm.selectEntry.length).toBe(76)
    wrapper.vm.pageChange(2, 20)
    await flushPromises()

    expect(wrapper.vm.dataSource.map((task) => task.id)).toEqual(
      Array.from({ length: 20 }, (_, index) => index + 21),
    )
    expect(wrapper.vm.tableSelectedRowKeys).toEqual(
      Array.from({ length: 76 }, (_, index) => index + 1),
    )
  })

  it('待办全选未完成时切换已办，旧范围响应不得回填选择', async () => {
    await mountWorkbench()
    wrapper.vm.isTreeOr2D = '2D'
    wrapper.vm.activeCard = 1
    wrapper.vm.selectionScope = 'todo-query'
    wrapper.vm.activeQueryParams = {}
    let resolveTodo
    getToDoTaskInfo.mockImplementationOnce(() => new Promise((resolve) => {
      resolveTodo = resolve
    }))

    const pendingSelection = wrapper.vm.onSelectAll(true)
    wrapper.vm.activeCard = 2
    getFinishTaskInfo.mockResolvedValue({
      data: { list: [{ id: 'finish-row' }], totalNum: 1 },
    })
    await wrapper.vm.getTaskByCondition({})
    resolveTodo({ data: { list: [{ id: 'todo-row' }], totalNum: 1 } })
    await pendingSelection

    expect(wrapper.vm.selectionScope).toContain('2')
    expect(wrapper.vm.selectEntry.length).toBe(0)
    expect(wrapper.vm.selectedRowKeys).toEqual([])
  })
})
