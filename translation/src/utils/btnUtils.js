/**
 * 重置
 * 重置查询条件和分页信息，并调用查询接口函数
 * @param {Object} vm - Vue 实例，包含查询条件和分页信息
 * @param {Function|null} fetchData - 用于查询数据的函数，可能为 null
 */
export function reset(vm, fetchData) {
  for (let key in vm.search) {
    // 重置查询条件
    if (vm.search.hasOwnProperty(key)) {
      vm.search[key] = null;
    }
  }
  vm.pagination.current = 1; // 重置页数
  // 调用传入的查询接口函数
  if (typeof fetchData === 'function') {// 有可能不传，就是null
    fetchData();
  }
}