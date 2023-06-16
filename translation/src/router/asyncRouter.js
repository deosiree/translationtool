let root={ path: '/translate', name: 'translate', component: () => import('@/views/layout/layout.vue'),
  children: []
};
//绑定动态路由
export function initRouter(menuList){
  //循环数据库的菜单
  for(var menu of menuList){
    //没有数据的不绑定
    if(menu.component == "" || menu.component == null){
      continue;
    }
    if(menu.children.length > 0){
      let temp = {
        path: menu.url,
        name: menu.name,
        component: loadComponent(menu.component),
        children: []
      }
      root.children.push(temp)
      initRouter(menu.children)
    }else{
      root.children.push({
        path: menu.url,
        name: menu.name,
        component: loadComponent(menu.component),
      });
    }
  }
  return root
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