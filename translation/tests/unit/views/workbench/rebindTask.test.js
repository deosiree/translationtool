import { describe, it, expect } from 'vitest'

/**
 * 与 workbench/index.vue#rebindTask 同口径的纯函数，便于单测对象引用重绑。
 * @param {Array} dataSource
 * @param {'tree'|'2d'} isTreeOr2D
 * @param {string|number|null|undefined} id
 * @returns {Object|null}
 */
function findTaskForRebind(dataSource, isTreeOr2D, id) {
  if (!id) return null
  const flat =
    isTreeOr2D === 'tree'
      ? (dataSource || []).flatMap((b) => b.child || [])
      : dataSource || []
  return flat.find((t) => t.id === id) || null
}

describe('rebindTask 口径', () => {
  it('平铺：同 id 换新对象引用', () => {
    const oldRow = { id: 't1', num_entryExamine: 99 }
    const newRow = { id: 't1', num_entryExamine: 0 }
    const hit = findTaskForRebind([newRow, { id: 't2' }], '2d', oldRow.id)
    expect(hit).toBe(newRow)
    expect(hit).not.toBe(oldRow)
    expect(hit.num_entryExamine).toBe(0)
  })

  it('层级：从 child 找回', () => {
    const child = { id: 't1', num_entryExamine: 0 }
    const hit = findTaskForRebind(
      [{ id: 'branch_a', child: [child] }],
      'tree',
      't1'
    )
    expect(hit).toBe(child)
  })

  it('id 空或找不到：返回 null', () => {
    expect(findTaskForRebind([{ id: 't1' }], '2d', null)).toBeNull()
    expect(findTaskForRebind([{ id: 't1' }], '2d', 'missing')).toBeNull()
  })
})
