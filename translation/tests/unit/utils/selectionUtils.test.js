import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  pageChange,
  onSelectChange,
  onSelect,
  onSelectAll,
  selectAllEntry,
  clearAllEntry,
  mergeSelectedEntriesById
} from '@/utils/selectionUtils'

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

    it('应忽略空数组与无 id 的项', () => {
      expect(mergeSelectedEntriesById(null, [{ id: 1 }])).toEqual([{ id: 1 }])
      expect(mergeSelectedEntriesById([{ id: 1 }, null, {}], [{ id: 2 }])).toEqual([
        { id: 1 },
        { id: 2 },
      ])
    })
  })
})
