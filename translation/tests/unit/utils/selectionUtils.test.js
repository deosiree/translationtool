import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  pageChange,
  onSelectChange,
  onSelect,
  onSelectAll,
  selectAllEntry,
  clearAllEntry,
  mergeSelectedEntriesById,
  refreshSelectedEntriesById,
  refreshTableSelectionById,
  removeSelectedEntriesById,
  syncTableSelectionById,
  updateTableSelectionById,
  getSelectionScopeRows,
  getCurrentSelectionRows,
  clearTableRowsContext,
  clearTableQueryContext,
  resetTableQuerySession,
  collectAllPages,
  createSelectionOperationGuard,
} from '@/utils/selectionUtils'
import { partitionSubmittedRows } from '@/utils/batchUpdateResults.js'
import { createQueryVersionGuard } from '@/utils/queryVersionGuard'

describe('selectionUtils - 表格选择/分页工具函数', () => {
  let mockVm

  beforeEach(() => {
    mockVm = {
      pagination: {
        current: 1,
        pageSize: 10
      },
      selectedRowKeys: [],
      selectedRows: [],
      dataSource: [
        { id: 1, name: 'Item 1' },
        { id: 2, name: 'Item 2' },
        { id: 3, name: 'Item 3' }
      ],
      filters: null
    }
  })

  describe('pageChange', () => {
    it('应该更新分页信息', () => {
      const fetchData = vi.fn()
      pageChange(mockVm, 2, 20, fetchData)

      expect(mockVm.pagination.current).toBe(2)
      expect(mockVm.pagination.pageSize).toBe(20)
      expect(fetchData).toHaveBeenCalled()
    })

    it('应该调用 fetchData 并传递参数', () => {
      const fetchData = vi.fn()
      pageChange(mockVm, 1, 10, fetchData, 'selectEntry', 'param1', 'param2')

      expect(fetchData).toHaveBeenCalledWith('param1', 'param2')
    })

    it('应该处理 fetchData 为 null 的情况', () => {
      pageChange(mockVm, 2, 20, null)

      expect(mockVm.pagination.current).toBe(2)
      expect(mockVm.pagination.pageSize).toBe(20)
    })

    it('应该保留之前勾选的词条', () => {
      mockVm.selectEntry = [
        { id: 1, name: 'Item 1' },
        { id: 2, name: 'Item 2' }
      ]
      mockVm.selectedRowKeys = []

      pageChange(mockVm, 2, 20, vi.fn())

      expect(mockVm.selectedRows).toEqual(mockVm.selectEntry)
      expect(mockVm.selectedRowKeys).toContain(1)
      expect(mockVm.selectedRowKeys).toContain(2)
    })
  })

  describe('onSelectChange', () => {
    it('应该更新选中的行和键', () => {
      const selectedRowKeys = [1, 2]
      const selectedRows = [
        { id: 1, name: 'Item 1' },
        { id: 2, name: 'Item 2' }
      ]

      onSelectChange(mockVm, selectedRowKeys, selectedRows)

      expect(mockVm.selectedRowKeys).toEqual([1, 2])
      expect(mockVm.selectedRows).toHaveLength(2)
    })

    it('应该去重选中的行', () => {
      const selectedRowKeys = [1, 1, 2, 2]
      const selectedRows = [
        { id: 1, name: 'Item 1' },
        { id: 1, name: 'Item 1' },
        { id: 2, name: 'Item 2' }
      ]

      onSelectChange(mockVm, selectedRowKeys, selectedRows)

      expect(mockVm.selectedRowKeys).toEqual([1, 2])
      expect(mockVm.selectedRows).toHaveLength(2)
    })
  })

  describe('onSelect', () => {
    it('应该在选中时添加记录', () => {
      const record = { id: 1, name: 'Item 1' }
      mockVm.selectedRows = []
      mockVm.selectedRowKeys = []

      onSelect(mockVm, record, true)

      expect(mockVm.selectedRows).toContainEqual(record)
      // 注意：onSelect 函数在选中时不会更新 selectedRowKeys
      // selectedRowKeys 只在取消选中时被过滤
    })

    it('应该在取消选中时移除记录', () => {
      mockVm.selectedRows = [{ id: 1, name: 'Item 1' }]
      // 注意：根据代码，selectedRowKeys 可能是对象数组（有 id 属性）
      // 第98-100行的代码：vm.selectedRowKeys.filter((item) => { return item.id !== record.id; })
      mockVm.selectedRowKeys = [{ id: 1 }] // selectedRowKeys 可能是对象数组
      const record = { id: 1, name: 'Item 1' }

      onSelect(mockVm, record, false)

      expect(mockVm.selectedRows).not.toContainEqual(record)
      // selectedRowKeys 会被过滤掉 id === record.id 的项
      const hasId1 = mockVm.selectedRowKeys.some(key => 
        (typeof key === 'object' ? key.id : key) === 1
      )
      expect(hasId1).toBe(false)
    })

    it('应该在 condition 为 false 时不执行操作', () => {
      const record = { id: 1, name: 'Item 1' }
      const initialRows = [...mockVm.selectedRows]

      onSelect(mockVm, record, true, false)

      expect(mockVm.selectedRows).toEqual(initialRows)
    })

    it('应该处理 selectEntry', () => {
      mockVm.selectEntry = []
      const record = { id: 1, name: 'Item 1' }

      onSelect(mockVm, record, true, true, 'selectEntry')

      expect(mockVm.selectEntry).toContainEqual(record)
    })

    it('应该避免重复添加', () => {
      const record = { id: 1, name: 'Item 1' }

      onSelect(mockVm, record, true)
      onSelect(mockVm, record, true)

      expect(mockVm.selectedRows.filter(r => r.id === 1)).toHaveLength(1)
    })
  })

  describe('onSelectAll', () => {
    it('应该全选当前页数据', () => {
      const selectedRows = []
      const changeRows = []

      onSelectAll(mockVm, true, selectedRows, changeRows)

      expect(mockVm.selectedRows.length).toBeGreaterThan(0)
      expect(mockVm.selectedRowKeys.length).toBeGreaterThan(0)
    })

    it('应该反选当前页数据', () => {
      mockVm.selectedRows = [
        { id: 1, name: 'Item 1' },
        { id: 2, name: 'Item 2' }
      ]
      mockVm.selectedRowKeys = [1, 2]
      const selectedRows = []
      const changeRows = []

      onSelectAll(mockVm, false, selectedRows, changeRows)

      expect(mockVm.selectedRows.length).toBe(0)
      expect(mockVm.selectedRowKeys.length).toBe(0)
    })

    it('应该根据筛选条件过滤数据', () => {
      mockVm.filters = {
        isExist: [1],
        entrySource: 'test'
      }
      mockVm.dataSource = [
        { id: 1, name: 'Item 1', isExist: 1, entrySource: 'test' },
        { id: 2, name: 'Item 2', isExist: 0, entrySource: 'test' },
        { id: 3, name: 'Item 3', isExist: 1, entrySource: 'other' }
      ]

      onSelectAll(mockVm, true, [], [])

      // 只有 id: 1 符合两个筛选条件
      expect(mockVm.selectedRows.some(r => r.id === 1)).toBe(true)
      expect(mockVm.selectedRows.some(r => r.id === 2)).toBe(false)
      expect(mockVm.selectedRows.some(r => r.id === 3)).toBe(false)
    })

    it('应该在 condition 为 false 时不执行操作', () => {
      const initialRows = [...mockVm.selectedRows]

      onSelectAll(mockVm, true, [], [], false)

      expect(mockVm.selectedRows).toEqual(initialRows)
    })
  })

  describe('selectAllEntry', () => {
    it('应该全选所有数据', () => {
      selectAllEntry(mockVm)

      expect(mockVm.selectedRowKeys.length).toBe(mockVm.dataSource.length)
      expect(mockVm.selectedRows.length).toBe(mockVm.dataSource.length)
    })

    it('应该清空之前的选中状态', () => {
      mockVm.selectedRowKeys = [999]
      mockVm.selectedRows = [{ id: 999 }]

      selectAllEntry(mockVm)

      expect(mockVm.selectedRowKeys).not.toContain(999)
      expect(mockVm.selectedRows.some(r => r.id === 999)).toBe(false)
    })

    it('应该根据筛选条件过滤', () => {
      mockVm.filters = {
        isExist: [1]
      }
      mockVm.dataSource = [
        { id: 1, name: 'Item 1', isExist: 1 },
        { id: 2, name: 'Item 2', isExist: 0 },
        { id: 3, name: 'Item 3', isExist: 1 }
      ]

      selectAllEntry(mockVm)

      expect(mockVm.selectedRows.length).toBe(2)
      expect(mockVm.selectedRows.every(r => r.isExist === 1)).toBe(true)
    })
  })

  describe('clearAllEntry', () => {
    it('应该清空所有选中状态', () => {
      mockVm.selectedRowKeys = [1, 2, 3]
      mockVm.selectedRows = [
        { id: 1 },
        { id: 2 },
        { id: 3 }
      ]
      mockVm.selectEntry = [{ id: 1 }]

      clearAllEntry(mockVm)

      expect(mockVm.selectedRowKeys).toEqual([])
      expect(mockVm.selectedRows).toEqual([])
      expect(mockVm.selectEntry).toEqual([])
    })

    it('应该处理不存在的 selectEntry', () => {
      mockVm.selectedRowKeys = [1, 2]
      mockVm.selectedRows = [{ id: 1 }, { id: 2 }]

      clearAllEntry(mockVm, 'nonExistent')

      expect(mockVm.selectedRowKeys).toEqual([])
      expect(mockVm.selectedRows).toEqual([])
    })
  })

  describe('mergeSelectedEntriesById', () => {
    it('当前页子集 + 全量应去重且长度等于全量', () => {
      const pageRows = [
        { id: 1, entry: 'a' },
        { id: 2, entry: 'b' },
      ]
      const allRows = [
        { id: 1, entry: 'a' },
        { id: 2, entry: 'b' },
        { id: 3, entry: 'c' },
      ]

      const merged = mergeSelectedEntriesById(pageRows, allRows)

      expect(merged).toHaveLength(3)
      expect(merged.map((r) => r.id).sort()).toEqual([1, 2, 3])
    })

    it('含其它产品 id 时合并后其它产品条目仍在', () => {
      const existing = [
        { id: 'a1', productID: 'productA' },
        { id: 'b1', productID: 'productB' },
      ]
      const incoming = [
        { id: 'b1', productID: 'productB', entry: 'updated' },
        { id: 'b2', productID: 'productB' },
      ]

      const merged = mergeSelectedEntriesById(existing, incoming)

      expect(merged.find((r) => r.id === 'a1')).toEqual({
        id: 'a1',
        productID: 'productA',
      })
      expect(merged.find((r) => r.id === 'b2')).toBeTruthy()
      expect(merged).toHaveLength(3)
    })

    it('同 id 时应以 incoming 覆盖', () => {
      const existing = [{ id: 1, name: 'old' }]
      const incoming = [{ id: 1, name: 'new' }]

      const merged = mergeSelectedEntriesById(existing, incoming)

      expect(merged).toEqual([{ id: 1, name: 'new' }])
    })

    it('刷新已选行时只更新现有 ID，不额外选中新查询中的行', () => {
      expect(refreshSelectedEntriesById(
        [{ id: 1, name: 'old' }, { id: 2, name: 'keep' }],
        [{ id: 1, name: 'new' }, { id: 3, name: 'not-selected' }]
      )).toEqual([{ id: 1, name: 'new' }, { id: 2, name: 'keep' }])
    })

    it('刷新树形查询时更新选中子行对象并同步行键', () => {
      const selectedChild = { id: 'child', value: 'old' }
      const vm = {
        selectedRows: [selectedChild, { id: 'outside', value: 'keep' }],
        selectedRowKeys: ['child', 'outside'],
      }

      refreshTableSelectionById(vm, [{ id: 'parent', child: [{ id: 'child', value: 'new' }] }])

      expect(vm.selectedRows).toEqual([
        { id: 'child', value: 'new' },
        { id: 'outside', value: 'keep' },
      ])
      expect(vm.selectedRowKeys).toEqual(['child', 'outside'])
    })

    it('刷新查询时按仅存的行键恢复当前范围内的已选行', () => {
      const vm = {
        selectedRows: [{ id: 'outside', value: 'keep' }],
        selectedRowKeys: ['current', 'outside'],
      }

      refreshTableSelectionById(vm, [{ id: 'current', value: 'server' }])

      expect(vm.selectedRows).toEqual([
        { id: 'current', value: 'server' },
        { id: 'outside', value: 'keep' },
      ])
      expect(vm.selectedRowKeys).toEqual(['current', 'outside'])
    })

    it('clearTableRowsContext retains cross-query selections while clearing rows', () => {
      const vm = {
        dataSource: [{ id: 'old-row' }],
        allData: [{ id: 'old-row' }],
        selectedRows: [{ id: 'old-row' }],
        selectedRowKeys: ['old-row'],
        editableData: { 'old-row': { value: 'draft' } },
        cellErrors: { 'old-row': { value: 'invalid' } },
        selectedRowIndex: 'old-row',
      }

      clearTableRowsContext(vm)

      expect(vm.dataSource).toEqual([])
      expect(vm.allData).toEqual([])
      expect(vm.selectedRows).toEqual([{ id: 'old-row' }])
      expect(vm.selectedRowKeys).toEqual(['old-row'])
      expect(vm.editableData).toEqual({})
      expect(vm.cellErrors).toEqual({})
      expect(vm.selectedRowIndex).toBeNull()
    })

    it('clearTableQueryContext clears selections when query context is discarded', () => {
      const vm = {
        dataSource: [{ id: 'old-row' }],
        allData: [{ id: 'old-row' }],
        selectedRows: [{ id: 'old-row' }],
        selectedRowKeys: ['old-row'],
      }

      clearTableQueryContext(vm)

      expect(vm.dataSource).toEqual([])
      expect(vm.allData).toEqual([])
      expect(vm.selectedRows).toEqual([])
      expect(vm.selectedRowKeys).toEqual([])
    })

    it('resetTableQuerySession使单个查询守卫失效并清空完整会话', () => {
      const queryGuard = createQueryVersionGuard()
      const oldToken = queryGuard.next()
      const vm = {
        queryGuard,
        queryLoading: true,
        queryScope: 'old-query',
        dataSource: [{ id: 'old-row' }],
        allData: [{ id: 'old-row' }],
        dataSourceAll: [{ id: 'old-row' }],
        selectedRows: [{ id: 'old-row' }],
        selectedRowKeys: ['old-row'],
        selectedRowIndex: 'old-row',
        editableData: { 'old-row': { value: 'draft' } },
        cellErrors: { 'old-row': { value: 'invalid' } },
      }

      resetTableQuerySession(vm)

      expect(queryGuard.isCurrent(oldToken)).toBe(false)
      expect(vm.queryLoading).toBe(false)
      expect(vm.queryScope).toBe('')
      expect(vm.dataSource).toEqual([])
      expect(vm.allData).toEqual([])
      expect(vm.dataSourceAll).toEqual([])
      expect(vm.selectedRows).toEqual([])
      expect(vm.selectedRowKeys).toEqual([])
      expect(vm.selectedRowIndex).toBeNull()
      expect(vm.editableData).toEqual({})
      expect(vm.cellErrors).toEqual({})
    })

    it('resetTableQuerySession支持同时失效多个查询守卫', () => {
      const queryGuard = createQueryVersionGuard()
      const importGuard = createQueryVersionGuard()
      const queryToken = queryGuard.next()
      const importToken = importGuard.next()
      const vm = {
        queryLoading: true,
        queryScope: 'old-query',
        dataSource: [{ id: 'old-row' }],
        selectedRows: [{ id: 'old-row' }],
        selectedRowKeys: ['old-row'],
      }

      resetTableQuerySession(vm, [queryGuard, importGuard])

      expect(queryGuard.isCurrent(queryToken)).toBe(false)
      expect(importGuard.isCurrent(importToken)).toBe(false)
      expect(vm.queryLoading).toBe(false)
      expect(vm.queryScope).toBe('')
      expect(vm.dataSource).toEqual([])
      expect(vm.selectedRows).toEqual([])
      expect(vm.selectedRowKeys).toEqual([])
    })

    it('应忽略空数组与无 id 的项', () => {
      expect(mergeSelectedEntriesById(null, [{ id: 1 }])).toEqual([{ id: 1 }])
      expect(mergeSelectedEntriesById([{ id: 1 }, null, {}], [{ id: 2 }])).toEqual([
        { id: 1 },
        { id: 2 },
      ])
    })
  })

  describe('跨页选择操作', () => {
    it('优先使用完整缓存，初始状态回退当前数据源', () => {
      expect(getSelectionScopeRows({
        allData: [{ id: 'all' }],
        dataSource: [{ id: 'page' }],
      })).toEqual([{ id: 'all' }])
      expect(getSelectionScopeRows({
        allData: [],
        dataSource: [{ id: 'page' }],
      })).toEqual([{ id: 'page' }])
    })
    it('本地筛选时从完整缓存恢复对象并只选择当前筛选子集', () => {
      const vm = {
        allData: [
          { id: 1, entry: '命中', isExist: 1 },
          { id: 2, entry: '未命中', isExist: 0 },
          { id: 3, entry: '命中', isExist: 1 },
        ],
        dataSource: [{ id: 1, entry: '旧对象', isExist: 1 }, { id: 3, entry: '命中', isExist: 1 }],
        pagination: { total: 2 },
        filters: { isExist: [1] },
      }

      expect(getCurrentSelectionRows(vm)).toEqual([
        { id: 1, entry: '命中', isExist: 1 },
        { id: 3, entry: '命中', isExist: 1 },
      ])
    })

    it('远程分页没有完整缓存时保留当前页数据，不扩展到其他页', () => {
      const vm = {
        allData: [{ id: 1 }],
        dataSource: [{ id: 1 }],
        pagination: { total: 3 },
        filters: null,
      }

      expect(getCurrentSelectionRows(vm)).toEqual([{ id: 1 }])
    })
    it('单行取勾只移除回调行并保留其他页选择', () => {
      mockVm.selectedRows = [
        { id: 1, name: '第一页' },
        { id: 2, name: '第二页' },
        { id: 3, name: '范围外' },
      ]
      mockVm.selectedRowKeys = [1, 2, 3]

      updateTableSelectionById(mockVm, [{ id: 1 }], false)

      expect(mockVm.selectedRowKeys).toEqual([2, 3])
      expect(mockVm.selectedRows).toEqual([
        { id: 2, name: '第二页' },
        { id: 3, name: '范围外' },
      ])
    })

    it('单行父级选择按树形语义同步子行', () => {
      updateTableSelectionById(
        mockVm,
        [{ id: 10, children: [{ id: 11 }, { id: 12 }] }],
        true,
      )

      expect(mockVm.selectedRowKeys).toEqual([10, 11, 12])
    })

    it('单行选择回调保留当前结果其他页和范围外的已选行', () => {
      const outsideScope = { id: 'other-query', name: '范围外' }
      const firstPageRow = { id: 'page-1', name: '旧对象' }
      const secondPageRow = { id: 'page-2', name: '第二页' }
      const refreshedFirstPageRow = { id: 'page-1', name: '新对象' }
      mockVm.selectedRows = [outsideScope, firstPageRow, secondPageRow]
      mockVm.selectedRowKeys = ['other-query', 'page-1', 'page-2']

      syncTableSelectionById(
        mockVm,
        [refreshedFirstPageRow],
        [firstPageRow, secondPageRow]
      )

      expect(mockVm.selectedRows).toEqual([outsideScope, refreshedFirstPageRow])
      expect(mockVm.selectedRowKeys).toEqual(['other-query', 'page-1'])
    })

    it('按完整树范围同步单行选择且按 ID 去重', () => {
      const parent = { id: 'parent', children: [{ id: 'child' }] }
      mockVm.selectedRows = [{ id: 'outside' }, parent, { id: 'child' }]
      mockVm.selectedRowKeys = ['outside', 'parent', 'child']

      syncTableSelectionById(
        mockVm,
        [{ id: 'parent', children: [{ id: 'child' }] }, { id: 'child' }],
        [parent]
      )

      expect(mockVm.selectedRowKeys).toEqual(['outside', 'parent', 'child'])
      expect(mockVm.selectedRows.filter((row) => row.id === 'child')).toHaveLength(1)
    })

    it('按 pageSize 逐页收集完整结果', async () => {
      const fetchPage = vi.fn(async (pageIndex, pageSize) => ({
        data: {
          totalNum: 5,
          list: Array.from({ length: Math.min(pageSize, 5 - (pageIndex - 1) * pageSize) }, (_, index) => ({
            id: (pageIndex - 1) * pageSize + index + 1,
          })),
        },
      }))

      const rows = await collectAllPages(fetchPage, 2)

      expect(fetchPage.mock.calls).toEqual([[1, 2], [2, 2], [3, 2]])
      expect(rows.map((row) => row.id)).toEqual([1, 2, 3, 4, 5])
    })

    it('当前收集操作失效后不再请求下一页且不返回部分结果', async () => {
      let isCurrent = true
      const fetchPage = vi.fn(async () => {
        isCurrent = false
        return { data: { totalNum: 3, list: [{ id: 1 }] } }
      })

      const rows = await collectAllPages(fetchPage, 2, () => isCurrent)

      expect(fetchPage).toHaveBeenCalledTimes(1)
      expect(rows).toEqual([])
    })

    it('分页请求失败时保留原始错误', async () => {
      const failure = new Error('请求失败')
      const fetchPage = vi.fn().mockRejectedValue(failure)

      await expect(collectAllPages(fetchPage, 2)).rejects.toBe(failure)
      expect(fetchPage).toHaveBeenCalledTimes(1)
    })

    it('后端实际页长小于请求页长时仍收齐 totalNum', async () => {
      const total = 76
      const responsePageSize = 20
      const fetchPage = vi.fn(async (pageIndex, pageSize) => {
        const effectivePageSize = Math.min(pageSize, responsePageSize)
        const start = (pageIndex - 1) * effectivePageSize
        return {
          data: {
            totalNum: total,
            list: Array.from(
              { length: Math.min(effectivePageSize, total - start) },
              (_, index) => ({ id: start + index + 1 })
            ),
          },
        }
      })

      const rows = await collectAllPages(fetchPage, 100)

      expect(fetchPage.mock.calls).toEqual([
        [1, 100],
        [2, 20],
        [3, 20],
        [4, 20],
      ])
      expect(rows).toHaveLength(total)
      expect(new Set(rows.map((row) => row.id)).size).toBe(total)
    })

    it('接口未返回 totalNum 时持续请求到不足一页', async () => {
      const fetchPage = vi.fn(async (pageIndex) => ({
        data: {
          list: pageIndex < 3 ? [{ id: pageIndex * 2 - 1 }, { id: pageIndex * 2 }] : [],
        },
      }))

      const rows = await collectAllPages(fetchPage, 2)

      expect(fetchPage).toHaveBeenCalledTimes(3)
      expect(rows.map((row) => row.id)).toEqual([1, 2, 3, 4])
    })

    it('取勾当前范围完整结果时保留其他范围的已选行', () => {
      expect(removeSelectedEntriesById(
        [{ id: 1 }, { id: 2 }, { id: 3 }],
        [{ id: 2 }]
      )).toEqual([{ id: 1 }, { id: 3 }])
    })

    it('父级全选请求期间，较新的子行操作覆盖同 ID 结果', () => {
      const guard = createSelectionOperationGuard()
      const operation = guard.begin('todo-tree-query', true)
      guard.record(2, false)

      expect(guard.project(2, 'todo-tree-query', false)).toBe(false)
      expect(guard.project(3, 'todo-tree-query', false)).toBe(true)
      expect(guard.resolve(operation, [{ id: 1 }, { id: 2 }, { id: 3 }], [], 'todo-tree-query'))
        .toEqual([{ id: 1 }, { id: 3 }])
    })

    it('父级全选期间对子项和兄弟项的最后操作分别生效', () => {
      const guard = createSelectionOperationGuard()
      const operation = guard.begin('parent-query', true)
      guard.record('child-a', false)
      guard.record('child-b', true)
      const currentRows = [{ id: 'child-b' }]

      const selectedRows = guard.resolve(
        operation,
        [
          { id: 'parent' },
          { id: 'child-a' },
          { id: 'child-b' },
          { id: 'sibling' },
        ],
        currentRows,
        'parent-query'
      )

      expect(selectedRows.map((row) => row.id).sort()).toEqual(['child-b', 'parent', 'sibling'])
    })

    it('全选与取勾响应逆序时仍按最后操作优先且不重复 ID', () => {
      const guard = createSelectionOperationGuard()
      const select = guard.begin('query', true)
      const deselect = guard.begin('query', false)

      const afterDeselect = guard.resolve(deselect, [{ id: 1 }, { id: 2 }], [], 'query')
      const afterSelect = guard.resolve(select, [{ id: 1 }, { id: 2 }], afterDeselect, 'query')

      expect(afterSelect).toEqual([])
    })

    it('失败或查询范围失效时不应用临时投影和迟到结果', () => {
      const guard = createSelectionOperationGuard()
      const failed = guard.begin('query-a', true)
      expect(guard.project(1, 'query-a', false)).toBe(true)
      guard.reject(failed)
      expect(guard.project(1, 'query-a', false)).toBe(false)

      const stale = guard.begin('query-a', true)
      expect(guard.isCurrent(stale, 'query-a')).toBe(true)
      guard.invalidate()
      expect(guard.isCurrent(stale, 'query-b')).toBe(false)
      expect(guard.resolve(stale, [{ id: 1 }], [], 'query-b')).toEqual([])
    })
  })

  describe('批量保存结果映射', () => {
    it('逐条结果只返回成功行，失败行保留原提交对象', () => {
      const submitted = [
        { id: 'ok', value: 'new' },
        { id: 'failed', value: 'edited' },
      ]
      const result = partitionSubmittedRows(submitted, {
        code: 203,
        data: {
          totalNum: 1,
          list: [
            { id: 'ok', success: true },
            { id: 'failed', success: false, message: '校验失败' },
          ],
        },
      })

      expect(result.successRows).toEqual([submitted[0]])
      expect(result.failedRows).toEqual([submitted[1]])
      expect(result.successCount).toBe(1)
      expect(result.failedCount).toBe(1)
    })

    it('兼容旧版只返回失败行的响应', () => {
      const submitted = [{ id: 'ok' }, { id: 'failed' }]
      const result = partitionSubmittedRows(submitted, {
        code: 200,
        data: { totalNum: 1, list: [{ id: 'failed' }] },
      })

      expect(result.successRows).toEqual([{ id: 'ok' }])
      expect(result.failedRows).toEqual([{ id: 'failed' }])
      expect(result.failedCount).toBe(1)
    })
  })
})
