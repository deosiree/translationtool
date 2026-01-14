/**
 * HTTP/请求处理工具函数
 * 包含异步请求处理、参数编码等功能
 */
import { message } from "ant-design-vue";
import { cancelRequest } from "@/http/request";

const requestDelId = [];// 存储删除请求的id，用于保留loading状态

/**
 * 处理异步请求的通用函数
 * @param {Object} validateRef - 表单验证引用对象，用于调用 validate 方法进行表单验证
 * @param {Function} getDataFn - 获取数据的异步函数，接收 params 和 data 作为参数
 * @param {Object} params - 传递给 getDataFn 的参数对象
 * @param {*} [data=null] - 可选参数，传递给 getDataFn 的额外数据
 * @returns {Promise<Array>} - 返回一个 Promise，解析为数据列表数组，如果出错则返回空数组
 */
export async function handleAsyncRequest(closeLoading, validateRef, getDataFn, params = null, data = null, returnParams = 'data.list') {
  try {
    // 执行表单验证
    await validateRef.validate();
    console.log("表单验证结果", validateRef.validate());
  }
  catch (err) {
    // 数据获取失败，提示错误信息
    // console.log("表单校验失败！", err.message, closeLoading);
    closeLoading();
    return [];
  }
  try {
    // 调用 getDataFn 并等待其结果
    const res = await getDataFn(params, data);
    if (returnParams) {
      const result = returnParams.split('.').reduce((obj, key) => obj && obj[key], res);// 默认为 res.data.list
      // console.log("返回值为", result);
      return result;
    }
    else {
      return [];
    }
  } catch (err) {
    if (err.name === 'ValidationError') {
      // 校验参数未通过，不提示错误信息
      return [];
    }
    // 数据获取失败，提示错误信息
    message.error("数据获取失败！", err.message);
    console.log("数据获取失败", err);
    return [];
  }
}

/**
 * 对接口入参进行编码转译，使用 encodeURIComponent 处理
 * @param {string | object | array} input - 输入的参数，可以是字符串、对象或数组
 * @returns {string | object | array} - 转译后的参数
 */
export function encodeParams(input) {
  if (typeof input === 'string') {
    return encodeURIComponent(input);
  } else if (Array.isArray(input)) {
    return input.map(item => encodeParams(item));
  } else if (typeof input === 'object' && input !== null) {
    const newObj = {};
    for (const key in input) {
      if (input.hasOwnProperty(key)) {
        newObj[key] = encodeParams(input[key]);
      }
    }
    return newObj;
  }
  return input;
}

/**
 * 查询
 * 查询按钮共用多个接口，并维护loading状态
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
