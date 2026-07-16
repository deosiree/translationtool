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
  const normalized = normalizeViewPath(view)
  return () => import(`@/views${normalized}`)
}

/** 菜单 component 字段统一为 `/path/index.vue`（兼容库里漏写 .vue） */
function normalizeViewPath(view) {
  let path = String(view || '').trim()
  if (!path.startsWith('/')) path = `/${path}`
  if (!path.endsWith('.vue')) path = `${path}.vue`
  return path
}
