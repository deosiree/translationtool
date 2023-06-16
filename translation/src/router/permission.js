import router from '@/router/index'
import {fnAddDynamicMenuRoutes, initRouter} from '@/router/asyncRouter'
import store from '@/store'
import { message } from 'ant-design-vue';
 
// 白名单
const whiteList = ['/']
let isLoad = true
// 导航守卫
router.beforeEach((to, from,next) => {
  // 判断是否为带名单页面
  if (whiteList.indexOf(to.path) !== -1) {
    next()
  }else{
    try {
      // 判断是否已经获取过动态菜单，未获取，则需要获取一次  页面刷新获取一次
      if (store.state.menu.length != 0 && isLoad) {
        //获取路由
        let menuRouter = initRouter(store.state.menu)
        // console.log("menuRouter:",menuRouter)
        // 动态添加路由
        router.addRoute(menuRouter);
        next({...to, replace: true})
        //将路由存入vuex
        store.dispatch('dynamicRoutes', menuRouter).then(() => {})
        isLoad = false
      } else {
        // 路由已存在或已缓存路由
        // 判断token是否存在
        if(store.state.token === null || store.state.token === ''){
          router.push('/')
          message.error('请重新登录！')
        }else{
          next()
        }
      }
    } catch (error) {
      console.log('出错了:',error)
      // next(`/?redirect=${to.path}`)
    }

  }
})
