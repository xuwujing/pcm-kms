<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>应用总数</template>
          <div style="font-size: 36px; font-weight: bold; color: #409eff;">{{ appCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>密钥总数</template>
          <div style="font-size: 36px; font-weight: bold; color: #67c23a;">{{ keyCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>用户总数</template>
          <div style="font-size: 36px; font-weight: bold; color: #e6a23c;">{{ userCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>系统状态</template>
          <div style="font-size: 24px; font-weight: bold; color: #67c23a;">运行中</div>
          <div style="margin-top: 8px; color: #999;">SQLite 模式</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listApps, listKeys } from '../api'
import http from '../api/http'

const appCount = ref(0)
const keyCount = ref(0)
const userCount = ref(0)

onMounted(async () => {
  try {
    const [appsRes, keysRes, usersRes] = await Promise.all([
      listApps(1, 1),
      listKeys({ page: 1, size: 1 }),
      http.get('/admin/users')
    ])
    const appsData = appsRes.data || appsRes
    appCount.value = appsData.total || 0
    const keysData = keysRes.data || keysRes
    keyCount.value = keysData.total || 0
    const usersData = usersRes.data || usersRes
    userCount.value = Array.isArray(usersData) ? usersData.length : (usersData.total || 0)
  } catch (e) { /* ignore */ }
})
</script>
