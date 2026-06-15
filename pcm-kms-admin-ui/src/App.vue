<template>
  <router-view v-if="isLoginPage" />
  <el-container v-else class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="brand">PCM-KMS</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#1f2937"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/apps">
          <el-icon><Monitor /></el-icon>
          <span>应用管理</span>
        </el-menu-item>
        <el-menu-item index="/keys">
          <el-icon><Key /></el-icon>
          <span>密钥管理</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/audit">
          <el-icon><Document /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
        <el-menu-item index="/ratelimit">
          <el-icon><Timer /></el-icon>
          <span>限流配置</span>
        </el-menu-item>
        <el-menu-item index="/crypto">
          <el-icon><Lock /></el-icon>
          <span>加解密测试</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title || 'PCM-KMS' }}</span>
        <div class="header-actions">
          <span class="username">{{ username }}</span>
          <el-button link type="primary" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const isLoginPage = computed(() => route.path === '/login')

const username = computed(() => {
  try {
    const raw = localStorage.getItem('kms_user')
    return raw ? JSON.parse(raw).username || 'admin' : 'admin'
  } catch {
    return 'admin'
  }
})

const handleLogout = () => {
  localStorage.removeItem('kms_token')
  localStorage.removeItem('kms_user')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.sidebar {
  background: #1f2937;
}

.brand {
  padding: 20px 16px;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  color: #6b7280;
  font-size: 13px;
}

.main {
  background: #f3f4f6;
}
</style>
