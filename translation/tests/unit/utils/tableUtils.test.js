import { describe, it, expect, vi } from 'vitest'
import {
  handleResizeColumn,
  getRowClassName,
  handleSearch,
  handleReset,
  clearFilters,
  handleTableChange,
  setTableHeight,
} from '@/utils/tableUtils'

describe('tableUtils - 表格 UX 工具函数', () => {
  describe('handleResizeColumn', () => {
    it('应该更新列的宽度', () => {
      const col = { width: 100, dataIndex: 'test' }
      handleResizeColumn(150, col)
      expect(col.width).toBe(150)
    })
  })

  describe('getRowClassName', () => {
    it('应该为奇数行返回 table-striped', () => {
      expect(getRowClassName({ id: 1 }, 1, null)).toBe('table-striped')
    })

    it('应该为选中的偶数行返回 highlighted-row', () => {
      expect(getRowClassName({ id: 1 }, 0, 1)).toBe('highlighted-row')
    })
  })

  describe('handleSearch', () => {
    it('应该调用 confirm 并设置搜索状态', () => {
      const confirm = vi.fn()
      const vm = { state: {} }
      handleSearch(['keyword'], confirm, 'entry', vm)
      expect(confirm).toHaveBeenCalled()
      expect(vm.state.searchText).toBe('keyword')
    })
  })

  describe('handleReset', () => {
    it('应该调用 clearFilters 并清空搜索状态', () => {
      const clearFiltersFn = vi.fn()
      const vm = { state: { searchText: 'keyword' } }
      handleReset(clearFiltersFn, vm)
      expect(clearFiltersFn).toHaveBeenCalledWith({ confirm: true })
      expect(vm.state.searchText).toBe('')
    })
  })

  describe('clearFilters', () => {
    it('应该清空所有列的 filteredValue', () => {
      const vm = {
        filters: { isExist: [1] },
        columns: [{ dataIndex: 'isExist', filteredValue: [1] }],
      }
      clearFilters(vm)
      expect(vm.columns[0].filteredValue).toBe(null)
    })
  })

  describe('handleTableChange', () => {
    it('应该更新 filters 和列的 filteredValue', () => {
      const vm = {
        filters: {},
        columns: [{ dataIndex: 'isExist', filteredValue: null }],
        dataSource: [{ id: 1, isExist: 1 }],
      }
      handleTableChange({ current: 1, pageSize: 10 }, { isExist: [1] }, vm)
      expect(vm.columns[0].filteredValue).toEqual([1])
    })
  })

  describe('setTableHeight', () => {
    it('应该计算并设置表格高度', () => {
      const vm = {
        $nextTick: vi.fn((cb) => cb()),
        $refs: {
          search: { $el: { offsetHeight: 50 } },
          box: { offsetHeight: 800 },
          operationArea: { $el: { offsetHeight: 100 } },
          button: { offsetHeight: 40 },
        },
        dataHeight: 0,
        tableHeight: { y: 0 },
      }
      setTableHeight(vm)
      expect(vm.dataHeight).toBe(650)
    })
  })
})
