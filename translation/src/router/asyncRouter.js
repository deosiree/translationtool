// 绑定动态路由（仅返回 children，避免覆盖静态 /translate 路由）
export function initRouter(menuList) {
  const children = []

  // 循环数据库的菜单
  for (const menu of menuList) {
    // 没有组件的不绑定
    if (menu.component === '' || menu.component === null) {
      continue
    }

    if (menu.children && menu.children.length > 0) {
      children.push({
        path: menu.url,
        name: menu.name,
        component: loadComponent(menu.component),
        children: initRouter(menu.children),
      })
    } else {
      children.push({
        path: menu.url,
        name: menu.name,
        component: loadComponent(menu.component),
      })
    }
  }

  return children
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