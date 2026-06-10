<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>应用列表</span>
          <el-button type="primary" @click="showCreate = true">创建应用</el-button>
        </div>
      </template>
      <el-table :data="apps" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="clientName" label="应用名称" width="180" />
        <el-table-column prop="clientId" label="Client ID" width="220">
          <template #default="{ row }">
            <span v-if="row.clientId">{{ row.clientId }}</span>
            <el-tag v-else type="info" size="small">未启用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clientGroupName" label="应用组" width="120" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contacts" label="联系人" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button v-if="!row.enabled" type="primary" size="small" @click="handleEnable(row)">
              启用
            </el-button>
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建应用对话框 -->
    <el-dialog v-model="showCreate" title="创建应用" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="应用名称">
          <el-input v-model="form.clientName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="应用组">
          <el-input v-model="form.clientGroup" placeholder="默认 default" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contacts" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="showDetail" title="应用详情" width="600px">
      <el-descriptions :column="1" border v-if="currentApp">
        <el-descriptions-item label="ID">{{ currentApp.id }}</el-descriptions-item>
        <el-descriptions-item label="应用名称">{{ currentApp.clientName }}</el-descriptions-item>
        <el-descriptions-item label="Client ID">{{ currentApp.clientId || '未启用' }}</el-descriptions-item>
        <el-descriptions-item label="Client Secret">
          <span v-if="currentApp.clientSecret">{{ currentApp.clientSecret }}</span>
          <span v-else>未启用</span>
        </el-descriptions-item>
        <el-descriptions-item label="应用组">{{ currentApp.clientGroupName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentApp.enabled ? '已启用' : '未启用' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentApp.contacts }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentApp.mobile }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentApp.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApps, createApp, enableApp, getApp } from '../api'

const apps = ref([])
const showCreate = ref(false)
const showDetail = ref(false)
const currentApp = ref(null)
const form = ref({ clientName: '', clientGroup: 'default', contacts: '', mobile: '' })

const loadApps = async () => {
  const res = await listApps()
  apps.value = res.data || []
}

const handleCreate = async () => {
  if (!form.value.clientName) {
    ElMessage.warning('请输入应用名称')
    return
  }
  await createApp(form.value)
  ElMessage.success('创建成功')
  showCreate.value = false
  form.value = { clientName: '', clientGroup: 'default', contacts: '', mobile: '' }
  loadApps()
}

const handleEnable = async (row) => {
  await ElMessageBox.confirm('启用后将生成 clientId/clientSecret 和默认密钥，确认启用？', '确认')
  await enableApp(row.id)
  ElMessage.success('启用成功')
  loadApps()
}

const handleView = async (row) => {
  const res = await getApp(row.id)
  currentApp.value = res.data
  showDetail.value = true
}

onMounted(loadApps)
</script>
