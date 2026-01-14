/**
 * 数据结构处理工具函数
 * 包含数组过滤、树形数据处理等函数
 */

/**
 * 根据节点key获取状态路径
 * @description 递归遍历树形数据结构，查找指定key的节点并返回从根节点到该节点的完整路径
 * @param {Array} treeData - 树形数据数组，每个节点包含key、title和可选的children属性
 * @param {string|number} targetKey - 要查找的目标节点的key值
 * @param {Array} path - 可选参数，当前路径数组，默认为空数组
 * @returns {Array|null} 找到目标节点时返回包含路径title的数组，未找到时返回null
 * @example
 * // 示例树结构
 * const tree = [
 *   { key: '1', title: '根节点', children: [
 *     { key: '2', title: '子节点1' },
 *     { key: '3', title: '子节点2', children: [
 *       { key: '4', title: '孙节点1' }
 *     ]}
 *   ]}
 * ];
 * // 获取key为'4'的节点路径
 * const path = getPathByKey(tree, '4'); // 返回 ['根节点', '子节点2', '孙节点1']
 */
export function getPathByKey(treeData, targetKey, path = []) {
  for (const node of treeData) {
    // 创建当前路径副本并添加当前节点
    const currentPath = [...path, node.title];

    // 如果找到目标节点，返回完整路径
    if (node.key === targetKey) {
      return currentPath;
    }

    // 如果当前节点有子节点，递归搜索
    if (node.children && node.children.length > 0) {
      const foundPath = getPathByKey(
        node.children,
        targetKey,
        currentPath
      );
      if (foundPath) {
        return foundPath;
      }
    }
  }
  // 未找到目标节点
  return null;
}

/**
 * 从 filterSource 中移除 arr 中的数据
 * @param {Array} filterSource - 需要过滤的源数组，每个对象必须有一个唯一的 id 属性
 * @param {Array} arr - 包含要移除的对象数组，每个对象必须有一个唯一的 id 属性
 * @returns {Array} - 过滤后的数组
 */
export function filter_arr(filterSource, arr) {
  filterSource = filterSource.filter((item) => {
    return !arr.some(
      (arrItem) => arrItem.id === item.id
    );
  });
  return filterSource;
}

/**
 * 从 filterSource 中移除 arr 中的数据
 * @param {Array} filterSource - 需要过滤的源数组，每个元素是一个唯一的键值,代表id属性
 * @param {Array} arr - 包含要移除的对象数组，每个元素是一个唯一的键值,代表id属性
 * @returns {Array} - 过滤后的数组
 */
export function filter_arr_keys(filterSource, arr) {
  filterSource = filterSource.filter((key) => {
    return !arr.some(
      (arrItem) => arrItem.id === key
    );
  });
  return filterSource;
}

/**
 * 两个数组取交集
 * @param {Array} nums1 - 第一个数组
 * @param {Array} nums2 - 第二个数组
 * @return {Array} - 交集数组
 */
export function intersection(nums1, nums2) {
  if (nums1.length === 0) {
    return nums2;
  }
  if (nums2.length === 0) {
    return nums1;
  }
  let a = new Set(nums1);
  let b = new Set(nums2);
  let arr = Array.from(new Set([...b].filter((x) => a.has(x))));
  return arr;
}
