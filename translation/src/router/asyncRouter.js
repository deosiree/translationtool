// 用于处理动态菜单数据，将其转为 route 形式
// 用于保存普通路由数据
let temp = []
// 用于保存存在子路由的路由数据
let route = []
export function fnAddDynamicMenuRoutes (menuList = [], routes = []) {
    
    // 遍历数据
    for (let i = 0; i < menuList.length; i++) {
      // 存在子路由，则递归遍历，并返回数据作为 children 保存
      if (menuList[i].children && menuList[i].children.length > 0) {
        // 获取路由的基本格式
        route = getRoute(menuList[i])
        // 递归处理子路由数据，并返回，将其作为路由的 children 保存
        fnAddDynamicMenuRoutes(menuList[i].children)
        // 保存存在子路由的路由
        routes.push(route)
      } else {
        let route = getRoute(menuList[i]);
        if(route.path != null && route.path != ''){
           // 保存普通路由
            temp.push(route)
        }
        
      }
    }
    //首页导航
    let item = {
        url: "/translate",
        name: "translate",
        component: "/layout/index.vue"
    }
    let layout = getRoute(item);
    layout.children = temp
    // 返回路由结果
    return layout
  }
   
  // 返回路由的基本格式
  function getRoute (item) {
    // 路由基本格式
    let route = {
      // 路由的路径
      path: item.url,
      // 路由名
      name: item.name,
      // 路由所在组件
      component: loadComponent(item.component),
    //   meta: {
    //     id: item.id,
    //     icon: item.icon
    //   },
      // 路由的子路由
      children: []
    }
    // 返回 route
    return route
  }

//路由的插件
const loadComponent = (view) => {
    return () => import(`@/views${view}`)
    // if (process.env.NODE_ENV === 'development') {
    //   return (resolve) => require([`@/pages/${view}`], resolve)
    // } else {
    //   // 使用 import 实现生产环境的路由懒加载
    //   
    // }
  }