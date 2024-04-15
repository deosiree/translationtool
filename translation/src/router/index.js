import { createRouter, createWebHashHistory } from 'vue-router'

import Login from '@/views/login/index.vue'

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

export default router
