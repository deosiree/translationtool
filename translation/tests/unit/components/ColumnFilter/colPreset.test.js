import { describe, it, expect } from 'vitest'
import { mergeColPresets, resolvePresetCols, defaultSelectionFromCols, colsToFieldOptions } from '@/components/ColumnFilter/colPreset.js'

const allCols = [
  { label: '序号', value: 'index', index: 0, required: true },
  { label: '词条', value: 'entry', index: 1, required: true },
  { label: 'tag', value: 'tag', index: 2, visible: true },
  { label: 'comment', value: 'comment', index: 3, visible: false },
  { label: '操作', value: 'operation', index: 100, required: true },
]

describe('colPreset', () => {
  it('mergeColPresets 应支持 label 覆盖与 hidden', () => {
    const result = mergeColPresets(allCols, [
      { label: 'tag', visible: false },
      { label: 'comment', hidden: true },
    ])
    expect(result.find((c) => c.value === 'tag')).toMatchObject({ visible: false })
    expect(result.find((c) => c.value === 'comment')).toBeUndefined()
  })

  it('mergeColPresets defaults 应批量覆盖', () => {
    const result = mergeColPresets(allCols, [{ label: 'tag', visible: true }], {
      visible: false,
    })
    expect(result.find((c) => c.value === 'tag')).toMatchObject({ visible: true })
    expect(result.find((c) => c.value === 'entry')).toMatchObject({ visible: false })
  })

  it('resolvePresetCols 应解析 preset 对象', () => {
    const cols = resolvePresetCols({ ovrd: [{ label: 'tag', hidden: true }], defaults: null }, allCols)
    expect(cols.some((c) => c.value === 'tag')).toBe(false)
  })

  it('defaultSelectionFromCols 应计算默认勾选', () => {
    expect(defaultSelectionFromCols(allCols)).toEqual([
      'index',
      'entry',
      'tag',
      'operation',
    ])
  })

  it('mergeColPresets 应排除 allCols 上 hidden:true 的列', () => {
    const cols = [
      { label: '词条', value: 'entry', index: 1 },
      { label: '创建人', value: 'creator', index: 33, hidden: true },
    ]
    const result = mergeColPresets(cols, [], null)
    expect(result.map((c) => c.value)).toEqual(['entry'])
  })

  it('mergeColPresets defaults hidden:false 应覆盖 allCols hidden:true', () => {
    const cols = [
      { label: '词条', value: 'entry', index: 1 },
      { label: '创建人', value: 'creator', index: 33, hidden: true },
    ]
    const result = mergeColPresets(cols, [], { hidden: false })
    expect(result.map((c) => c.value)).toEqual(['entry', 'creator'])
  })

  it('colsToFieldOptions 应映射 label/value/index', () => {
    expect(colsToFieldOptions([{ label: '词条', value: 'entry', index: 2, visible: true }])).toEqual([
      { label: '词条', value: 'entry', index: 2 },
    ])
  })

  it('entry export preset 应排除 allCols hidden 列并保留导出字段', async () => {
    const { entryAllCols, entryPresets, entryParams } = await import('@/constants/commonParam.js')
    const exportCols = resolvePresetCols(entryPresets.export, entryAllCols)
    expect(exportCols.some((c) => c.value === 'creator')).toBe(false)
    expect(exportCols.some((c) => c.value === 'entry')).toBe(true)
    expect(entryParams.exportFields.length).toBe(34)
    expect(entryParams.exportFields.some((f) => f.value === 'creator')).toBe(false)
    const productCols = resolvePresetCols(entryPresets.productEntry, entryAllCols)
    expect(productCols.some((c) => c.value === 'creator')).toBe(true)
  })

  it('entry 展示条件默认勾选应仅含词条、翻译语种、翻译结果', async () => {
    const { entryParams } = await import('@/constants/commonParam.js')
    expect(defaultSelectionFromCols(entryParams.searchConditionList)).toEqual([
      'entry',
      'language',
      'translate',
    ])
    expect(entryParams.checkedSearchCondition).toEqual([
      'entry',
      'language',
      'translate',
    ])
  })
})
