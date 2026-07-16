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
        const menuRoutes = initRouter(store.state.menu)
        // 动态路由作为 /translate 的 children 追加，避免覆盖静态路由（用于开发测试的原型页等）
        for (const r of menuRoutes) {
          router.addRoute('translate', r)
        }
        next({...to, replace: true})
        //将路由存入vuex
        store.dispatch('dynamicRoutes', menuRoutes).then(() => {})
        isLoad = false
      } else {
        // 路由已存在或已缓存路由
        // 判断token是否存在
        if(store.state.token === null || store.state.token === ''){
          next({
            path: '/',
            query: { redirect: to.fullPath },
            replace: true,
          })
          message.error('请重新登录！')
        }else{
          next()
        }
      }
    } catch (error) {
      console.error('[router/permission] dynamic route failed:', error)
      // 必须调用 next，否则导航挂死（空白登录壳）
      next({
        path: '/',
        query: { redirect: to.fullPath },
        replace: true,
      })
      message.error('路由加载失败，请重新登录！')
    }

  }
})
