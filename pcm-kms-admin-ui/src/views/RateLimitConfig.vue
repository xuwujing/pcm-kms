<template>
  <div>
    <!-- 全局限流配置 -->
    <el-card style="margin-bottom: 20px;">
      <template #header>
        <span>全局限流配置（默认值）</span>
      </template>
      <el-form :model="globalConfig" label-width="160px" style="max-width: 500px;" v-loading="globalLoading">
        <el-form-item label="启用限流">
          <el-switch v-model="globalConfig.enabled" />
        </el-form-item>
        <el-form-item label="每分钟最大请求数">
          <el-input-number v-model="globalConfig.maxPerMinute" :min="1" :max="100000" :step="10" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveGlobalConfig" :loading="globalSaving">保存全局配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 应用级别限流配置 -->
    <el-card>
      <template #header>
        <span>应用级别限流配置</span>
      </template>
      <el-table :data="appConfigs" stripe style="width: 100%" v-loading="appLoading">
        <el-table-column prop="clientId" label="服务标识" width="160" />
        <el-table-column prop="clientName" label="应用名称" min-width="150" />
        <el-table-column label="限流配置" width="200">
          <template #default="{ row }">
            <div v-if="row.editing">
              <el-input-number v-model="row.maxPerMinute" :min="1" :max="100000" :step="10" size="small" style="width: 130px;" />
            </div>
            <span v-else>
              {{ row.maxPerMinute }} 次/分钟
              <el-tag v-if="row.isCustom" type="warning" size="small" style="margin-left: 4px;">自定义</el-tag>
              <el-tag v-else type="info" size="small" style="margin-left: 4px;">默认</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="启用限流" width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" size="small" @change="handleAppSwitch(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <template v-if="row.editing">
              <el-button type="primary" size="small" @click="saveAppConfig(row)">保存</el-button>
              <el-button size="small" @click="row.editing = false; loadAppConfigs()">取消</el-button>
            </template>
            <template v-else>
              <el-button type="primary" size="small" link @click="row.editing = true">修改</el-button>
              <el-button v-if="row.isCustom" type="danger" size="small" link @click="resetAppConfig(row)">恢复默认</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const globalConfig = ref({ enabled: true, maxPerMinute: 60 })
const globalLoading = ref(false)
const globalSaving = ref(false)
const appConfigs = ref([])
const appLoading = ref(false)

const loadGlobalConfig = async () => {
  globalLoading.value = true
  try {
    const res = await http.get('/admin/ratelimit/global')
    globalConfig.value = res.data || res
  } catch (e) {} finally {
    globalLoading.value = false
  }
}

const saveGlobalConfig = async () => {
  globalSaving.value = true
  try {
    await http.put('/admin/ratelimit/global', globalConfig.value)
    ElMessage.success('全局配置已保存')
    loadAppConfigs() // 刷新应用列表（默认值可能变了）
  } catch (e) {} finally {
    globalSaving.value = false
  }
}

const loadAppConfigs = async () => {
  appLoading.value = true
  try {
    const res = await http.get('/admin/ratelimit/apps')
    const list = res.data || res || []
    list.forEach(item => item.editing = false)
    appConfigs.value = list
  } catch (e) {} finally {
    appLoading.value = false
  }
}

const saveAppConfig = async (row) => {
  try {
    await http.post('/admin/ratelimit/apps', {
      clientId: row.clientId,
      maxPerMinute: row.maxPerMinute,
      enabled: row.enabled
    })
    ElMessage.success('应用限流配置已保存')
    row.editing = false
    loadAppConfigs()
  } catch (e) {}
}

const handleAppSwitch = async (row) => {
  await saveAppConfig(row)
}

const resetAppConfig = async (row) => {
  await ElMessageBox.confirm(`确认恢复 "${row.clientName}" 使用全局默认限流配置？`, '确认恢复')
  try {
    await http.delete(`/admin/ratelimit/apps/${row.clientId}`)
    ElMessage.success('已恢复为全局默认配置')
    loadAppConfigs()
  } catch (e) {}
}

onMounted(() => {
  loadGlobalConfig()
  loadAppConfigs()
})
</script>
