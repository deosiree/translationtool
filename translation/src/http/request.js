import axios from "axios";  //引入axios
import env from "./env";
import { message } from 'ant-design-vue';
import router from '@/router/index'
import store from '@/store'
import LoadingService from '@/http/loading'

// 创建axios实例
const service = axios.create({
  //这里拿线上接口测试
  baseURL: env.dev.baseUrl,
  headers: { //请求头
    // 'Content-Type': 'application/json;charset=UTF-8',
  },
  settimeout: 360000,//超时时间
  // settimeout: 180000,//超时时间
});

// 定义一个存储所有 AbortController 实例的对象
const controllers = {};
// 记录当前正在进行的请求数量
let requestCount = 0;
// 延迟隐藏 loading 的定时器
let hideLoadingTimer = null;
// 延迟时间，单位为毫秒
const DELAY_TIME = 1000; 

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 请求开始，计数器加 1
    requestCount++;
    // 清除延迟隐藏 loading 的定时器
    if (hideLoadingTimer) {
      clearTimeout(hideLoadingTimer);
      hideLoadingTimer = null;
    }
    if (requestCount === 1) {
      // 只有在第一个请求开始时显示 loading
      LoadingService.show();
    }

    // 取消上次请求
    if (Object(config.params)['requestId']) {// 只有提供了requestId的请求才可以取消
      const controller = new AbortController();
      controllers[config.params.requestId] = controller;
      config.signal = controller.signal;
      requestCount--;
      handleHideLoading();
    }

    // 在发送请求之前做些什么 验证token之类的
    // 从vuex中获取token
    // config.headers.token = sessionStorage.getItem('token')
    config.headers.token = store.state.token
    // console.log("请求的数据:", config);
    return config; //记得一定要 返回config
  },
  error => {
    // 请求失败，计数器减 1
    requestCount--;
    handleHideLoading();

    // 对请求错误做些什么
    // console.log("请求的数据11:", error);
    return Promise.reject(error);
  }
);
let messageFlag = true
// 响应拦截器
service.interceptors.response.use(
  response => {
    // 请求成功，计数器减 1
    requestCount--;
    handleHideLoading();

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
  },
  error => {
    // 请求失败，计数器减 1
    requestCount--;
    handleHideLoading();

    if (axios.isCancel(error)) {
      // console.log("请求已取消:", error.message);
    } else {
      // 对响应错误做点什么
      return Promise.reject(error);
    }
  }
);

// 处理隐藏 loading 的逻辑
export function handleHideLoading() {
  if (requestCount === 0) {
    // 设置延迟隐藏 loading 的定时器
    hideLoadingTimer = setTimeout(() => {
      LoadingService.hide();
      hideLoadingTimer = null;
    }, DELAY_TIME);
  }
}

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

export default service;