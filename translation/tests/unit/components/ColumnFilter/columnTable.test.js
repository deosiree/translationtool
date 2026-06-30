import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  buildCol,
  buildTable,
  filterWbColsForCtx,
} from '@/components/ColumnFilter/columnBuilder.js'
import {
  changeColumn,
  getColPref,
  mergeColumnSelection,
  getRequiredColumnValues,
  getDefaultColumnSelection,
  pruneColumnsToSelection,
  findTableHost,
} from '@/components/ColumnFilter/columnTable.js'

const mockColumnSettingsList = [
  { label: '序号', value: 'index', index: 0, required: true },
  { label: '词条', value: 'entry', index: 2, required: true },
  { label: 'tag', value: 'tag', index: 3 },
  { label: '英文', value: 'english', index: 12 },
  { label: '操作', value: 'operation', index: 100, required: true },
]

const wbAllCols = [
  { label: '序号', value: 'index', index: 0, required: true },
  { label: '词条', value: 'entry', index: 1, required: true },
  { label: '翻译', value: 'translate', index: 5, required: true },
  { label: 'tag', value: 'tag', index: 7, visible: true },
  { label: 'comment', value: 'comment', index: 8, visible: true },
  { label: '英文', value: 'english', index: 12, visible: false },
  { label: '存在状态', value: 'isExist', index: 2, visible: false },
]

describe('columnBuilder', () => {
  it('buildCol 应为 entry 设置 fixed left', () => {
    const col = buildCol({ label: '词条', value: 'entry', index: 1 }, {}, 100)
    expect(col.fixed).toBe('left')
    expect(col.colValue).toBe('entry')
  })

  it('buildCol 在 needFilter 时为 entrySource 添加筛选', () => {
    const col = buildCol(
      { label: '词条来源', value: 'entrySource', index: 2 },
      {},
      100,
      true
    )
    expect(col.customFilterDropdown).toBe(true)
    expect(col.onFilter('test', { entrySource: 'test source' })).toBe(true)
  })

  it('buildTable 应解析动态 dataIndex 并排除当前语种列', () => {
    const task = {
      transMap: {
        value: 'english',
        interpretation: 'englishInterp',
        state: 'englishTranslateState',
      },
    }
    const { columnSettingsList, columns } = buildTable(
      wbAllCols,
      { ovrd: [{ label: '审核意见', hidden: true }], defaults: null },
      { task, pagination: { pageSize: 20, current: 1 } },
      100,
      false,
      filterWbColsForCtx
    )
    expect(columnSettingsList.find((c) => c.value === 'english')).toBeUndefined()
    expect(columns.find((c) => c.colValue === 'translate')).toMatchObject({
      dataIndex: 'english',
    })
  })
})

describe('columnTable', () => {
  describe('findTableHost', () => {
    it('应向上查找含 columnSettingsList 与 columns 的 vm', () => {
      const host = {
        columnSettingsList: [],
        columns: [],
        $parent: { $parent: null },
      }
      const child = { $parent: host }
      expect(findTableHost(child)).toBe(host)
    })

    it('超过 maxDepth 应返回 null', () => {
      let current = { $parent: null }
      for (let i = 0; i < 10; i++) {
        current = { $parent: current }
      }
      expect(findTableHost(current, 3)).toBeNull()
    })
  })

  describe('mergeColumnSelection', () => {
    it('应合并必选列与用户勾选的可选列', () => {
      const result = mergeColumnSelection(['tag', 'english'], mockColumnSettingsList)
      expect(result).toEqual(['index', 'entry', 'tag', 'english', 'operation'])
    })

    it('getRequiredColumnValues 应返回必选列 value', () => {
      expect(getRequiredColumnValues(mockColumnSettingsList)).toEqual([
        'index',
        'entry',
        'operation',
      ])
    })
  })

  describe('getDefaultColumnSelection', () => {
    it('应包含 required 列与 visible 不为 false 的可选列', () => {
      const list = [
        { value: 'index', required: true },
        { value: 'entry', required: true },
        { value: 'tag', visible: true },
        { value: 'comment', visible: false },
        { value: 'operation', required: true },
      ]
      expect(getDefaultColumnSelection(list)).toEqual([
        'index',
        'entry',
        'tag',
        'operation',
      ])
    })
  })

  describe('pruneColumnsToSelection', () => {
    it('应通过 colValue 保留动态 dataIndex 的必选列', () => {
      const vm = {
        columns: [
          { dataIndex: 'index', colValue: 'index' },
          { dataIndex: 'entry', colValue: 'entry' },
          { dataIndex: 'english', colValue: 'translate' },
          { dataIndex: 'tag', colValue: 'tag' },
        ],
      }
      pruneColumnsToSelection(vm, ['index', 'entry'], [
        { label: '序号', value: 'index', required: true },
        { label: '词条', value: 'entry', required: true },
        { label: '翻译', value: 'translate', required: true },
      ])
      expect(vm.columns.map((c) => c.colValue)).toEqual(['index', 'entry', 'translate'])
    })
  })

  describe('changeColumn', () => {
    const colPrefName = 'test-col-pref-required'

    beforeEach(() => {
      localStorage.removeItem(colPrefName)
    })

    afterEach(() => {
      localStorage.removeItem(colPrefName)
    })

    it('不应从 columns 中移除必选列', () => {
      const vm = {
        checkedColumn: [],
        colBuildCtx: { pagination: { pageSize: 20, current: 1 } },
        columnSettingsList: mockColumnSettingsList,
        columns: [
          { dataIndex: 'index', colValue: 'index', index: 0 },
          { dataIndex: 'entry', colValue: 'entry', index: 2 },
          { dataIndex: 'tag', colValue: 'tag', index: 3 },
          { dataIndex: 'english', colValue: 'english', index: 12 },
          { dataIndex: 'operation', colValue: 'operation', index: 100 },
        ],
      }

      changeColumn(
        colPrefName,
        100,
        ['index', 'entry', 'operation'],
        vm,
        false,
        mockColumnSettingsList
      )

      expect(vm.columns.map((col) => col.colValue)).toEqual([
        'index',
        'entry',
        'operation',
      ])
    })
  })

  describe('getColPref', () => {
    const colPrefName = 'test-col-pref'

    beforeEach(() => {
      localStorage.removeItem(colPrefName)
    })

    afterEach(() => {
      localStorage.removeItem(colPrefName)
    })

    it('无 localStorage 时应使用 columnSettingsList 默认勾选', () => {
      const vm = {
        checkedColumn: [],
        colBuildCtx: { pagination: { pageSize: 20, current: 1 } },
        columnSettingsList: mockColumnSettingsList,
        columns: [
          { dataIndex: 'index', colValue: 'index', index: 0, label: '序号', required: true },
          { dataIndex: 'entry', colValue: 'entry', index: 2, label: '词条', required: true },
          { dataIndex: 'tag', colValue: 'tag', index: 3, label: 'tag' },
          { dataIndex: 'english', colValue: 'english', index: 12, label: '英文' },
          { dataIndex: 'operation', colValue: 'operation', index: 100, label: '操作', required: true },
        ],
      }

      getColPref(colPrefName, 100, vm, false, mockColumnSettingsList)

      expect(vm.checkedColumn).toEqual(['index', 'entry', 'tag', 'english', 'operation'])
      expect(localStorage.getItem(colPrefName)).toBe(
        JSON.stringify({ displayColumn: 'tag,english' })
      )
    })
  })
})
