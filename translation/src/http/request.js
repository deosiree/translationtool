/**
 * Axios 请求实例 - 支持多种编码类型
 * 
 * @module http/request
 * 
 * ## 编码类型说明
 * 
 * ### 1. JSON (默认) - request / requestJson
 * - Content-Type: application/json;charset=UTF-8
 * - 用途：CRUD 操作、查询、配置等常规接口
 * - 使用场景：传递对象、数组等结构化数据
 * - 示例：
 *   ```javascript
 *   import request from '@/http/request';
 *   request({ url: '/api/search', method: 'POST', data: { name: 'test' } });
 *   ```
 * 
 * ### 2. Form - requestForm
 * - Content-Type: application/x-www-form-urlencoded
 * - 用途：登录、认证等需要表单编码的场景
 * - 使用场景：后端要求 form-urlencoded 格式（注意：如果后端不支持，不要使用此实例）
 * - 示例：
 *   ```javascript
 *   import { requestForm } from '@/http/request';
 *   requestForm({ url: '/api/login', method: 'POST', data: { username: 'user', password: 'pass' } });
 *   ```
 * 
 * ### 3. Multipart - requestMultipart
 * - Content-Type: multipart/form-data（浏览器自动设置）
 * - 用途：文件上传、同时传递文件和参数
 * - 使用场景：
 *   - 仅传递 FormData：requestMultipart({ url: '/api/upload', method: 'POST', data: formData })
 *   - FormData + params：requestMultipart({ url: '/api/import', method: 'POST', params: { type: 'excel' }, data: formData })
 *     - params 会作为 URL query string: /api/import?type=excel
 *     - FormData 在 request body 中
 * - 示例：
 *   ```javascript
 *   import { requestMultipart } from '@/http/request';
 *   const formData = new FormData();
 *   formData.append('file', file);
 *   requestMultipart({ url: '/api/upload', method: 'POST', data: formData });
 *   
 *   // 同时传递 params 和 FormData（规范做法）
 *   requestMultipart({ url: '/api/import', method: 'POST', params: { lang: 'en' }, data: formData });
 *   ```
 * 
 * ### 4. Binary - requestBinary
 * - Content-Type: application/octet-stream
 * - 用途：大文件直传、二进制流传输
 * - 使用场景：传输原始二进制数据
 * - 示例：
 *   ```javascript
 *   import { requestBinary } from '@/http/request';
 *   requestBinary({ url: '/api/download', method: 'POST', data: binaryData, responseType: 'blob' });
 *   ```
 */

import axios from "axios";  //引入axios
import env from "./env";
import { message } from 'ant-design-vue';
import router from '@/router/index'
import store from '@/store'
// import LoadingService from '@/http/loading'

// 定义一个存储所有 AbortController 实例的对象（所有实例共享）
const controllers = {};
// // 记录当前正在进行的请求数量
// let requestCount = 0;
// // 延迟隐藏 loading 的定时器
// let hideLoadingTimer = null;
// // 延迟时间，单位为毫秒
// const DELAY_TIME = 1000; 

// 基础配置
const baseConfig = {
  baseURL: env.dev.baseUrl,
  timeout: 360000, // 超时时间（注意：原代码写的是 settimeout，应该是 timeout）
};

// ==================== 请求拦截器（所有实例共享） ====================
const requestInterceptor = (config) => {
  // // 请求开始，计数器加 1
  // requestCount++;
  // // 清除延迟隐藏 loading 的定时器
  // if (hideLoadingTimer) {
  //   clearTimeout(hideLoadingTimer);
  //   hideLoadingTimer = null;
  // }
  // if (requestCount === 1) {
  //   // 只有在第一个请求开始时显示 loading
  //   LoadingService.show();
  // }

  // 取消上次请求
  if (Object(config.params)['requestId']) {// 只有提供了requestId的请求才可以取消
    const controller = new AbortController();
    controllers[config.params.requestId] = controller;
    config.signal = controller.signal;
    // requestCount--;
    // handleHideLoading();
  }

  // 在发送请求之前做些什么 验证token之类的
  // 从vuex中获取token
  // config.headers.token = sessionStorage.getItem('token')
  config.headers.token = store.state.token
  // console.log("请求的数据:", config);
  return config; //记得一定要 返回config
};

const requestInterceptorError = (error) => {
  // // 请求失败，计数器减 1
  // requestCount--;
  // handleHideLoading();

  // 对请求错误做些什么
  // console.log("请求的数据11:", error);
  return Promise.reject(error);
};

// ==================== 响应拦截器（按实例分别注册） ====================
let messageFlag = true;

// JSON 响应拦截器（用于 jsonInstance / formInstance / multipartInstance）
const jsonResponseInterceptor = (response) => {
  // // 请求成功，计数器减 1
  // requestCount--;
  // handleHideLoading();

  // console.log("返回的数据", response);
  // 这里拦截401错误，并重新跳入登页重新获取token
  if (response.status && response.status === 200) {
    if (response.data.code === 200) {// 通讯成功
      return response.data
    } else if (response.data.code === 205) {
      // 如果是token过期，跳转至登录
      if (messageFlag) {
        messageFlag = false
        message.error({ content: "登录已过期，请重新登录！", onClose: (() => { messageFlag = true }) });
      }

      // 移除token，跳转至登录页面
      store.commit("removeData")
      router.push({ path: '/' })
    } else {
      // console.log("-----------------------")
      message.error(response.data.message || '操作失败!')
      return Promise.reject(response)
    }
    return Promise.resolve(response.data)
  } else {
    message.error('请求失败!')
    return Promise.reject(response)
  }
};

// Binary 响应拦截器（用于 binaryInstance，直接返回完整 response，调用方可读取 headers 与 data）
const binaryResponseInterceptor = (response) => {
  if (response && response.status && response.status === 200) {
    return response;
  } else {
    message.error('请求失败!');
    return Promise.reject(response);
  }
};

const responseInterceptorError = (error) => {
  // // 请求失败，计数器减 1
  // requestCount--;
  // handleHideLoading();

  if (axios.isCancel(error)) {
    // console.log("请求已取消:", error.message);
  } else {
    // 对响应错误做点什么
    return Promise.reject(error);
  }
};

// ==================== 创建 axios 实例 ====================

/**
 * JSON 实例（默认实例，向后兼容）
 * Content-Type: application/json;charset=UTF-8
 * 用于常规 CRUD、查询、配置等接口
 */
const jsonInstance = axios.create({
  ...baseConfig,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
});

// 添加拦截器
jsonInstance.interceptors.request.use(requestInterceptor, requestInterceptorError);
jsonInstance.interceptors.response.use(jsonResponseInterceptor, responseInterceptorError);

/**
 * Form 实例
 * Content-Type: application/x-www-form-urlencoded
 * 用于登录、认证等需要表单编码的场景
 */
const formInstance = axios.create({
  ...baseConfig,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
  transformRequest: [
    function (data) {
      // 如果是 FormData，直接返回（这种情况不应该发生，但兼容处理）
      if (data instanceof FormData) {
        return data;
      }
      // 将对象转换为 URLSearchParams 格式
      if (data && typeof data === 'object') {
        const params = new URLSearchParams();
        Object.keys(data).forEach(key => {
          if (data[key] !== undefined && data[key] !== null) {
            params.append(key, data[key]);
          }
        });
        return params.toString();
      }
      return data;
    }
  ],
});

// 添加拦截器
formInstance.interceptors.request.use(requestInterceptor, requestInterceptorError);
formInstance.interceptors.response.use(jsonResponseInterceptor, responseInterceptorError);

/**
 * Multipart 实例
 * Content-Type: multipart/form-data（浏览器自动设置，包含 boundary）
 * 用于文件上传、同时传递文件和参数
 * 注意：params 会作为 URL query string，FormData 在 request body 中
 */
const multipartInstance = axios.create({
  ...baseConfig,
  // 不手动设置 Content-Type，让浏览器自动设置（包含 boundary）
});

// 添加拦截器
multipartInstance.interceptors.request.use(requestInterceptor, requestInterceptorError);
multipartInstance.interceptors.response.use(jsonResponseInterceptor, responseInterceptorError);

/**
 * Binary 实例
 * Content-Type: application/octet-stream
 * 用于大文件直传、二进制流传输
 */
const binaryInstance = axios.create({
  ...baseConfig,
  headers: {
    'Content-Type': 'application/octet-stream',
  },
});

// 添加拦截器
binaryInstance.interceptors.request.use(requestInterceptor, requestInterceptorError);
binaryInstance.interceptors.response.use(binaryResponseInterceptor, responseInterceptorError);

// // 处理隐藏 loading 的逻辑
// export function handleHideLoading() {
//   if (requestCount === 0) {
//     // 设置延迟隐藏 loading 的定时器
//     hideLoadingTimer = setTimeout(() => {
//       LoadingService.hide();
//       hideLoadingTimer = null;
//     }, DELAY_TIME);
//   }
// }

// 导出一个取消指定请求的函数
export const cancelRequest = (requestId) => {
  // console.log("8.取消请求", requestId, controllers);
  if (controllers[requestId]) {
    controllers[requestId].abort();
    // console.log(`9.请求 ${requestId} 已取消`);
    delete controllers[requestId];
  }
};

// 导出一个取消所有请求的函数
export const cancelAllRequests = () => {
  Object.keys(controllers).forEach(requestId => {
    controllers[requestId].abort();
    // console.log(`请求 ${requestId} 已取消`);
    delete controllers[requestId];
  });
};

// 导出实例（命名导出）
export const requestJson = jsonInstance;
export const requestForm = formInstance;
export const requestMultipart = multipartInstance;
export const requestBinary = binaryInstance;

// 默认导出 JSON 实例（向后兼容）
export default jsonInstance;