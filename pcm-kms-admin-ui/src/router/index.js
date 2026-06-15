import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { title: '登录', noAuth: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '仪表盘' } },
  { path: '/apps', component: () => import('../views/AppList.vue'), meta: { title: '应用管理' } },
  { path: '/keys', component: () => import('../views/KeyList.vue'), meta: { title: '密钥管理' } },
  { path: '/users', component: () => import('../views/UserList.vue'), meta: { title: '用户管理' } },
  { path: '/audit', component: () => import('../views/AuditLog.vue'), meta: { title: '审计日志' } },
  { path: '/ratelimit', component: () => import('../views/RateLimitConfig.vue'), meta: { title: '限流配置' } },
  { path: '/crypto', component: () => import('../views/CryptoTest.vue'), meta: { title: '加解密测试' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('kms_token')
  if (to.meta.noAuth || token) {
    next()
    return
  }
  next('/login')
})

export default router
