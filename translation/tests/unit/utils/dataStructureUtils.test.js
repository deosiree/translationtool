import { describe, it, expect } from 'vitest'
import { 
  filter_arr, 
  intersection,
  getPathByKey,
  filter_arr_keys
} from '@/utils/dataStructureUtils'

describe('dataStructureUtils - 数据结构处理工具函数', () => {
  describe('filter_arr', () => {
    it('应该从源数组中移除指定数组中的元素', () => {
      const source = [
        { id: 1, name: 'A' },
        { id: 2, name: 'B' },
        { id: 3, name: 'C' }
      ]
      const toRemove = [{ id: 2, name: 'B' }]
      const result = filter_arr(source, toRemove)
      
      expect(result).toHaveLength(2)
      expect(result).not.toContainEqual({ id: 2, name: 'B' })
    })

    it('应该处理空数组', () => {
      const source = []
      const toRemove = [{ id: 1 }]
      const result = filter_arr(source, toRemove)
      
      expect(result).toEqual([])
    })

    it('应该处理要移除的数组为空的情况', () => {
      const source = [
        { id: 1, name: 'A' },
        { id: 2, name: 'B' }
      ]
      const toRemove = []
      const result = filter_arr(source, toRemove)
      
      expect(result).toHaveLength(2)
    })

    it('应该处理多个要移除的元素', () => {
      const source = [
        { id: 1, name: 'A' },
        { id: 2, name: 'B' },
        { id: 3, name: 'C' },
        { id: 4, name: 'D' }
      ]
      const toRemove = [{ id: 2 }, { id: 4 }]
      const result = filter_arr(source, toRemove)
      
      expect(result).toHaveLength(2)
      expect(result.map(r => r.id)).toEqual([1, 3])
    })
  })

  describe('filter_arr_keys', () => {
    it('应该从源数组中移除指定数组中的键', () => {
      const source = [1, 2, 3, 4]
      const toRemove = [{ id: 2 }, { id: 3 }]
      const result = filter_arr_keys(source, toRemove)
      
      expect(result).toHaveLength(2)
      expect(result).not.toContain(2)
      expect(result).not.toContain(3)
    })

    it('应该处理空数组', () => {
      const source = []
      const toRemove = [{ id: 1 }]
      const result = filter_arr_keys(source, toRemove)
      
      expect(result).toEqual([])
    })
  })

  describe('intersection', () => {
    it('应该返回两个数组的交集', () => {
      const arr1 = [1, 2, 3, 4]
      const arr2 = [3, 4, 5, 6]
      const result = intersection(arr1, arr2)
      
      expect(result).toEqual([3, 4])
    })

    it('应该处理空数组', () => {
      expect(intersection([], [1, 2])).toEqual([1, 2])
      expect(intersection([1, 2], [])).toEqual([1, 2])
    })

    it('应该处理没有交集的情况', () => {
      const arr1 = [1, 2, 3]
      const arr2 = [4, 5, 6]
      const result = intersection(arr1, arr2)
      
      expect(result).toEqual([])
    })

    it('应该处理重复元素', () => {
      const arr1 = [1, 2, 2, 3]
      const arr2 = [2, 2, 3, 4]
      const result = intersection(arr1, arr2)
      
      expect(result).toEqual([2, 3])
    })
  })

  describe('getPathByKey', () => {
    it('应该返回树形数据中指定节点的路径', () => {
      const treeData = [
        {
          key: '1',
          title: '根节点',
          children: [
            { key: '2', title: '子节点1' },
            {
              key: '3',
              title: '子节点2',
              children: [
                { key: '4', title: '孙节点1' }
              ]
            }
          ]
        }
      ]
      
      const path = getPathByKey(treeData, '4')
      expect(path).toEqual(['根节点', '子节点2', '孙节点1'])
    })

    it('应该返回 null 当节点不存在时', () => {
      const treeData = [{ key: '1', title: '节点' }]
      const path = getPathByKey(treeData, '999')
      expect(path).toBeNull()
    })

    it('应该返回根节点的路径', () => {
      const treeData = [
        { key: '1', title: '根节点' },
        { key: '2', title: '另一个根节点' }
      ]
      
      const path = getPathByKey(treeData, '1')
      expect(path).toEqual(['根节点'])
    })

    it('应该处理多层嵌套', () => {
      const treeData = [
        {
          key: '1',
          title: 'L1',
          children: [
            {
              key: '2',
              title: 'L2',
              children: [
                {
                  key: '3',
                  title: 'L3',
                  children: [
                    { key: '4', title: 'L4' }
                  ]
                }
              ]
            }
          ]
        }
      ]
      
      const path = getPathByKey(treeData, '4')
      expect(path).toEqual(['L1', 'L2', 'L3', 'L4'])
    })

    it('应该处理空树', () => {
      const treeData = []
      const path = getPathByKey(treeData, '1')
      expect(path).toBeNull()
    })
  })
})
