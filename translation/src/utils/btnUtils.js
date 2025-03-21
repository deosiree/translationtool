/**
import default from './../../clearCopy/src/views/entry/common';
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

/**
 * 包裹 loading 效果
 * 为所有请求统一增加 loading 效果
 * @param {Object} vm - Vue 实例，用于控制 loading 状态
 * @param {Function} apiFunction - 要调用的 API 接口函数
 * @param {Object} params - 传递给 API 接口函数的参数
 * @param {Object} data - 传递给 API 接口函数的数据
 * @returns {Promise} - 返回一个 Promise 对象，用于处理 API 请求的结果
 */
export function requestLoading(vm, apiFunction, params, data) {
  vm.loading = true;
  return apiFunction(params, data).finally(() => {
    // 这里apiFunction()的()很重要，不加括号就不是函数不能执行，而作为参数时又必须不加()否则就会执行
    vm.loading = false;
  });
}

/**
 * 查询按钮点击事件处理函数
 * @param {Object} vm - Vue 实例，用于控制 loading 状态和调用 requestLoading 函数
 * @param {Object} params - 包含请求参数和数据的对象，结构为 { params: {...}, data: {...} }
 * @param {string} option - 用户选择的按钮名称，用于匹配 API 函数
 * @param {Object} apiFunctions - 包含按钮名称和对应 API 接口函数的对象，默认值为 { "按钮名称": "接口函数" }
 */
export function getSearch(vm, params, option, apiFunctions = { "按钮名称": "接口函数" }) {
  Object.entries(apiFunctions).forEach(([key, value]) => {
    if (option == key) {
      requestLoading(vm, value, params.params, params.data).then(
        (res) => {
          // console.log(`${key}成功`, res);
        }
      );
    }
  });
}