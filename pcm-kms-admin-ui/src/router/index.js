import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '首页' } },
  { path: '/apps', component: () => import('../views/AppList.vue'), meta: { title: '应用管理' } },
  { path: '/keys', component: () => import('../views/KeyList.vue'), meta: { title: '密钥管理' } },
  { path: '/crypto', component: () => import('../views/CryptoTest.vue'), meta: { title: '加解密测试' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
