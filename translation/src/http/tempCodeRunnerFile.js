import axios from "axios";  //引入axios
import env from "./env"; 
import { message } from 'ant-design-vue';
import router from '@/router/index'
import store from '@/store'

// 创建axios实例
const service = axios.create({
	//这里拿线上接口测试
	baseURL: env.dev.baseUrl,
    headers:{ //请求头
        // 'Content-Type': 'application/json;charset=UTF-8',
    },
    settimeout:180000,//超时时间
});
// 请求拦截器
service.interceptors.request.use(
config => {
    // 在发送请求之前做些什么 验证token之类的

    // 从vuex中获取token
    // config.headers.token = sessionStorage.getItem('token')
    config.headers.token = store.state.token
    // console.log("请求的数据:", config);
    return config; //记得一定要 返回config
},
error => {
    // 对请求错误做些什么
    // console.log("请求的数据11:", error);
    return Promise.reject(error);
}
);
let messageFlag = true
// 响应拦截器
service.interceptors.response.use(
response => {
    // console.log("返回的数据", response);
    // 这里拦截401错误，并重新跳入登页重新获取token
    if (response.status && response.status === 200) {
      // 通讯成功
      // if(response.data.code === 205){
      //   // 如果是token过期，跳转至登录
      //   message.error("登录已过期，请重新登录！");
      //    // 移除token，跳转至登录页面
      //   store.commit("removeData")
      //   router.push({ path: '/' })
      // }else{
      //   return response.data
      // }
      if (response.data.code === 200) {
        return response.data
      } else if (response.data.code === 205) { 
        // 如果是token过期，跳转至登录
        if(messageFlag){
          messageFlag = false
          message.error({content:"登录已过期，请重新登录！",onClose:(() => {messageFlag = true})});
        }
        
         // 移除token，跳转至登录页面
        store.commit("removeData")
        router.push({ path: '/' })
      }else{
        // console.log("-----------------------")
        message.error(response.data.message || '操作失败!')
        return Promise.reject(response)
      }
      return Promise.resolve(response.data)
    }else{
      message.error('请求失败!')
      return Promise.reject(response)
    }
},
error => {
    // 对响应错误做点什么
    return Promise.reject(error);
}
);
export default service;