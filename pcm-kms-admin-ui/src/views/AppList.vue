<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>应用管理</span>
        <el-button type="primary" @click="openCreate">创建应用</el-button>
      </div>
    </template>

    <el-form :inline="true" class="toolbar">
      <el-form-item label="应用标识">
        <el-input v-model="filters.clientId" clearable placeholder="kms_demo" />
      </el-form-item>
      <el-form-item label="应用名称">
        <el-input v-model="filters.clientName" clearable placeholder="Order Service" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.enabled" clearable placeholder="全部" style="width: 120px">
          <el-option label="启用" value="true" />
          <el-option label="禁用" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="apps" stripe>
      <el-table-column type="index" label="序号" width="70" />
      <el-table-column prop="clientId" label="应用标识" width="180" />
      <el-table-column prop="clientName" label="应用名称" min-width="160" />
      <el-table-column prop="clientGroup" label="分组" width="120" />
      <el-table-column prop="contacts" label="联系人" width="120" />
      <el-table-column prop="enabled" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">详情</el-button>
          <el-button v-if="!row.enabled" link type="success" @click="handleEnable(row)">启用</el-button>
          <el-button v-else link type="warning" @click="handleDisable(row)">禁用</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      class="pagination"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @current-change="loadApps"
      @size-change="loadApps"
    />
  </el-card>

  <el-dialog v-model="showCreate" title="创建应用" width="560px" destroy-on-close>
    <el-form :model="form" label-width="110px">
      <el-form-item label="应用标识" required>
        <el-input v-model="form.clientId" placeholder="kms_demo" />
      </el-form-item>
      <el-form-item label="应用名称" required>
        <el-input v-model="form.clientName" placeholder="Demo Service" />
      </el-form-item>
      <el-form-item label="分组">
        <el-input v-model="form.clientGroup" placeholder="default" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="form.contacts" placeholder="owner" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.mobile" placeholder="13800000000" />
      </el-form-item>
      <el-form-item label="工号">
        <el-input v-model="form.jobNo" placeholder="A001" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showCreate = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showDetail" title="应用详情" width="720px" destroy-on-close>
    <el-descriptions v-if="currentApp" :column="2" border>
      <el-descriptions-item label="应用标识">{{ currentApp.clientId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="应用名称">{{ currentApp.clientName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="分组">{{ currentApp.clientGroup || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="currentApp.enabled ? 'success' : 'info'">
          {{ currentApp.enabled ? '启用' : '禁用' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="联系人">{{ currentApp.contacts || '-' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ currentApp.mobile || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工号">{{ currentApp.jobNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatTime(currentApp.createdAt) }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ formatTime(currentApp.updatedAt) }}</el-descriptions-item>
      <el-descriptions-item label="密钥凭证" :span="2">
        <div class="secret-row">
          <code>{{ currentApp.clientSecret || '-' }}</code>
          <el-button
            v-if="currentApp.clientSecret"
            size="small"
            @click="copyText(currentApp.clientSecret)"
          >
            复制
          </el-button>
        </div>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createApp, getApp, listApps } from '../api'
import http from '../api/http'
import { formatTime } from '../utils/format'

const apps = ref([])
const loading = ref(false)
const showCreate = ref(false)
const showDetail = ref(false)
const creating = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const currentApp = ref(null)
const filters = ref({
  clientId: '',
  clientName: '',
  enabled: '',
})
const form = ref({
  clientId: '',
  clientName: '',
  clientGroup: 'default',
  contacts: '',
  mobile: '',
  jobNo: '',
})

const applyFilters = (records) => {
  return records.filter((item) => {
    if (filters.value.clientId && !String(item.clientId || '').includes(filters.value.clientId)) {
      return false
    }
    if (filters.value.clientName && !String(item.clientName || '').includes(filters.value.clientName)) {
      return false
    }
    if (filters.value.enabled !== '') {
      const expected = filters.value.enabled === 'true'
      if (Boolean(item.enabled) !== expected) {
        return false
      }
    }
    return true
  })
}

const loadApps = async () => {
  loading.value = true
  try {
    const res = await listApps(currentPage.value, pageSize.value)
    const data = res.data || res
    const records = data.records || (Array.isArray(data) ? data : [])
    apps.value = applyFilters(records)
    total.value = data.total || apps.value.length
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  form.value = {
    clientId: '',
    clientName: '',
    clientGroup: 'default',
    contacts: '',
    mobile: '',
    jobNo: '',
  }
  showCreate.value = true
}

const handleCreate = async () => {
  if (!form.value.clientId || !form.value.clientName) {
    ElMessage.warning('请填写应用标识和应用名称')
    return
  }
  creating.value = true
  try {
    await createApp(form.value)
    ElMessage.success('应用创建成功')
    showCreate.value = false
    await loadApps()
  } finally {
    creating.value = false
  }
}

const handleEnable = async (row) => {
  await ElMessageBox.confirm('确认启用该应用并生成接入凭证吗？', '提示', { type: 'warning' })
  await http.post(`/admin/apps/${row.id}/enable`)
  ElMessage.success('应用已启用')
  await loadApps()
}

const handleDisable = async (row) => {
  await ElMessageBox.confirm('确认禁用该应用吗？', '提示', { type: 'warning' })
  await http.post(`/admin/apps/${row.id}/disable`)
  ElMessage.success('应用已禁用')
  await loadApps()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除应用 ${row.clientName} 吗？`, '提示', { type: 'error' })
  await http.delete(`/admin/apps/${row.id}`)
  ElMessage.success('应用已删除')
  await loadApps()
}

const handleView = async (row) => {
  const res = await getApp(row.id)
  currentApp.value = res.data || res
  showDetail.value = true
}

const handleSearch = () => {
  currentPage.value = 1
  loadApps()
}

const handleReset = () => {
  filters.value = { clientId: '', clientName: '', enabled: '' }
  currentPage.value = 1
  loadApps()
}

const copyText = async (text) => {
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

onMounted(loadApps)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.secret-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.secret-row code {
  flex: 1;
  padding: 8px 10px;
  background: #f3f4f6;
  border-radius: 8px;
  word-break: break-all;
}
</style>
