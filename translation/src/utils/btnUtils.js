import { cancelRequest, cancelAllRequests } from "@/http/request";

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


const requestDelId = [];// 存储删除请求的id，用于保留loading状态

/**
 * 查询按钮点击事件处理函数
 * @param {Object} vm - Vue 实例，用于控制 loading 状态
 * @param {Object} params - 包含请求参数和数据的对象，结构为 { params: {...}, data: {...} }
 * @param {string} option - 用户选择的按钮名称，用于匹配 API 函数
 * @param {Object} apiFunctions - 包含按钮名称和对应 API 接口函数的对象，默认值为 { "按钮名称": "接口函数" }
 */
export function getSearch(vm, params, option, apiFunctions = { "按钮名称": "接口函数" }) {
  vm.loading = true; // 开始请求前设置 loading 为 true

  Object.entries(apiFunctions).forEach(([key, value]) => {
    if (option == key) {
      if (params.lastRequestId) {// 如果需要取消请求就会有上次请求的id属性lastRequestId，则可以取消上次的请求
        requestDelId.push(params.lastRequestId);
        cancelRequest(params.lastRequestId);
      }
      value(params.params, params.data).then((response) => {// 调用接口函数
        if (!requestDelId.includes(params.params.requestId)) {
          vm.loading = false;
          // console.log(`${key}请求完了,${params.params.requestId}`);
        }
        else {
          // console.log(`${key}请求被取消了,${params.params.requestId}`);
          requestDelId.splice(requestDelId.indexOf(params.params.requestId), 1); // 已经用过了保存loading状态的作用
        }
      }
      );
    }
  });
}