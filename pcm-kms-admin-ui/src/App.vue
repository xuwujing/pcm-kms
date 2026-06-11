<template>
  <!-- 登录页单独渲染 -->
  <router-view v-if="$route.path === '/login'" />
  <!-- 其他页面带侧边栏 -->
  <el-container v-else style="min-height: 100vh">
    <el-aside width="200px" style="background: #304156">
      <div style="padding: 20px; text-align: center; color: #fff; font-size: 18px; font-weight: bold;">
        PCM-KMS
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
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
      <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between;">
        <span style="font-size: 16px; font-weight: 500;">{{ $route.meta.title || 'PCM-KMS' }}</span>
        <div style="display: flex; align-items: center; gap: 12px;">
          <span style="font-size: 13px; color: #666;">{{ username }}</span>
          <el-button type="text" size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const username = computed(() => {
  try {
    const user = JSON.parse(localStorage.getItem('kms_user') || '{}')
    return user.username || ''
  } catch {
    return ''
  }
})

const handleLogout = () => {
  localStorage.removeItem('kms_token')
  localStorage.removeItem('kms_user')
  router.push('/login')
}
</script>
