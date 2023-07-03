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
    component: () => import('@/views/empty/notPermission.vue'),
  },
  {
    path: '/translate',
    name: 'translate',
    component: () => import('@/views/layout/layout.vue'),
    children: [
      
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
