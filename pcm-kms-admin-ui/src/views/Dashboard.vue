<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>应用总数</template>
          <div style="font-size: 36px; font-weight: bold; color: #409eff;">{{ appCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>密钥总数</template>
          <div style="font-size: 36px; font-weight: bold; color: #67c23a;">{{ keyCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>系统状态</template>
          <div style="font-size: 24px; font-weight: bold; color: #e6a23c;">运行中</div>
          <div style="margin-top: 8px; color: #999;">SQLite 模式</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listApps, listKeys } from '../api'

const appCount = ref(0)
const keyCount = ref(0)

onMounted(async () => {
  try {
    const [apps, keys] = await Promise.all([listApps(), listKeys()])
    appCount.value = apps.data ? apps.data.length : 0
    keyCount.value = keys.data ? keys.data.length : 0
  } catch (e) { /* ignore */ }
})
</script>
