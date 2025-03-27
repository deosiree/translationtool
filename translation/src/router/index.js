import { createRouter, createWebHashHistory } from 'vue-router'

import Login from '@/views/login/index.vue'

import LoadingService from '@/http/loading';
import request, { cancelAllRequests, handleHideLoading } from '../http/request';

const routes = [
  {
    path: '/',
    name: 'login',
    component: Login
  },
  {
    path: '/notPermission',
    name: 'notPermission',
    component: () => import('@/views/errorPage/403.vue'),
  },
  {
    path: '/translate',
    name: 'translate',
    component: () => import('@/views/layout/layout.vue'),
    children: [

    ]
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/errorPage/404.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  // 取消所有未完成的请求
  cancelAllRequests();

  // 启动显示 loading 的定时器
  request.showLoadingTimer = setTimeout(() => {
    LoadingService.show();
    request.showLoadingTimer = null;
  }, request.DELAY_TIME);
  next();
});

router.afterEach(() => {
  // 清除显示 loading 的定时器
  if (request.showLoadingTimer) {
    clearTimeout(request.showLoadingTimer);
    request.showLoadingTimer = null;
  }
  // 处理隐藏 loading 的逻辑
  handleHideLoading();
});

export default router
