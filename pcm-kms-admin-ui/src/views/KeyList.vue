<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>密钥管理</span>
        <el-button type="primary" @click="openCreate">创建密钥</el-button>
      </div>
    </template>

    <el-form :inline="true" class="toolbar">
      <el-form-item label="别名">
        <el-input v-model="filters.alias" clearable placeholder="user-phone-aes" />
      </el-form-item>
      <el-form-item label="算法">
        <el-select v-model="filters.algorithm" clearable placeholder="全部" style="width: 140px">
          <el-option label="AES" value="aes" />
          <el-option label="SM4" value="sm4" />
          <el-option label="RSA" value="rsa" />
          <el-option label="SM2" value="sm2" />
          <el-option label="SIGN" value="sign" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="keys" stripe>
      <el-table-column type="index" label="序号" width="70" />
      <el-table-column prop="alias" label="别名" min-width="180" />
      <el-table-column prop="algorithm" label="算法" width="120">
        <template #default="{ row }">
          <el-tag>{{ String(row.algorithm || '').toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="cryptoType" label="类型" width="120" />
      <el-table-column prop="clientGroup" label="分组" width="120" />
      <el-table-column prop="keyVersion" label="版本" width="90" />
      <el-table-column prop="enabled" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handlePermissions(row)">权限</el-button>
          <el-button v-if="row.enabled" link type="warning" @click="handleToggle(row, false)">禁用</el-button>
          <el-button v-else link type="success" @click="handleToggle(row, true)">启用</el-button>
          <el-button link type="primary" @click="handleRotate(row)">轮转</el-button>
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
      @current-change="loadKeys"
      @size-change="loadKeys"
    />
  </el-card>

  <el-dialog v-model="showCreate" title="创建密钥" width="560px" destroy-on-close>
    <el-form :model="form" label-width="120px">
      <el-form-item label="所属应用" required>
        <el-select v-model="form.clientId" filterable placeholder="请选择应用" style="width: 100%">
          <el-option
            v-for="item in availableApps"
            :key="item.clientId"
            :label="`${item.clientName} (${item.clientId})`"
            :value="item.clientId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="别名" required>
        <el-input v-model="form.alias" placeholder="user-phone-aes" />
      </el-form-item>
      <el-form-item label="算法" required>
        <el-select v-model="form.algorithm" style="width: 100%">
          <el-option label="AES" value="aes" />
          <el-option label="SM4" value="sm4" />
          <el-option label="RSA" value="rsa" />
          <el-option label="SM2" value="sm2" />
          <el-option label="SIGN" value="sign" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showCreate = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showPermissions" title="密钥权限" width="640px" destroy-on-close>
    <div class="permission-bar">
      <div>
        <strong>{{ currentKey?.alias }}</strong>
        <span class="subtle">{{ String(currentKey?.algorithm || '').toUpperCase() }}</span>
      </div>
      <div class="grant-box">
        <el-select v-model="grantClientId" filterable placeholder="请选择应用" size="small" style="width: 240px">
          <el-option
            v-for="item in availableApps"
            :key="item.clientId"
            :label="`${item.clientName} (${item.clientId})`"
            :value="item.clientId"
          />
        </el-select>
        <el-button type="primary" size="small" @click="handleGrant">授权</el-button>
      </div>
    </div>

    <el-table :data="permissions" stripe size="small">
      <el-table-column prop="clientId" label="应用标识" min-width="180" />
      <el-table-column prop="enabled" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '生效中' : '已撤销' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="授权时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleRevoke(row)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createKey,
  disableKey,
  enableKey,
  grantKeyPermission,
  listApps,
  listKeyPermissions,
  listKeys,
  revokeKeyPermission,
  rotateKey,
} from '../api'
import http from '../api/http'
import { formatTime } from '../utils/format'

const keys = ref([])
const availableApps = ref([])
const loading = ref(false)
const creating = ref(false)
const showCreate = ref(false)
const showPermissions = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const currentKey = ref(null)
const permissions = ref([])
const grantClientId = ref('')
const filters = ref({
  alias: '',
  algorithm: '',
})
const form = ref({
  clientId: '',
  alias: '',
  algorithm: 'aes',
  description: '',
})

const applyFilters = (records) =>
  records.filter((item) => {
    if (filters.value.alias && !String(item.alias || '').includes(filters.value.alias)) {
      return false
    }
    if (filters.value.algorithm && item.algorithm !== filters.value.algorithm) {
      return false
    }
    return true
  })

const loadKeys = async () => {
  loading.value = true
  try {
    const res = await listKeys({ page: currentPage.value, size: pageSize.value })
    const data = res.data || res
    const records = data.records || (Array.isArray(data) ? data : [])
    keys.value = applyFilters(records)
    total.value = data.total || keys.value.length
  } finally {
    loading.value = false
  }
}

const loadAvailableApps = async () => {
  const res = await listApps(1, 1000)
  const data = res.data || res
  const records = data.records || (Array.isArray(data) ? data : [])
  availableApps.value = records.filter((item) => item.clientId)
}

const openCreate = async () => {
  await loadAvailableApps()
  form.value = {
    clientId: '',
    alias: '',
    algorithm: 'aes',
    description: '',
  }
  showCreate.value = true
}

const handleCreate = async () => {
  if (!form.value.clientId || !form.value.alias || !form.value.algorithm) {
    ElMessage.warning('请选择应用并填写别名、算法')
    return
  }
  creating.value = true
  try {
    await createKey(form.value)
    ElMessage.success('密钥创建成功')
    showCreate.value = false
    await loadKeys()
  } finally {
    creating.value = false
  }
}

const handleToggle = async (row, enabled) => {
  await (enabled ? enableKey(row.id) : disableKey(row.id))
  ElMessage.success(enabled ? '密钥已启用' : '密钥已禁用')
  await loadKeys()
}

const handleRotate = async (row) => {
  await ElMessageBox.confirm(`确认轮转密钥 ${row.alias} 吗？`, '提示', { type: 'warning' })
  await rotateKey(row.id)
  ElMessage.success('密钥轮转成功')
  await loadKeys()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除密钥 ${row.alias} 吗？`, '提示', { type: 'error' })
  await http.delete(`/admin/keys/${row.id}`)
  ElMessage.success('密钥已删除')
  await loadKeys()
}

const handlePermissions = async (row) => {
  currentKey.value = row
  grantClientId.value = ''
  await loadAvailableApps()
  const res = await listKeyPermissions(row.id)
  permissions.value = res.data || res || []
  showPermissions.value = true
}

const handleGrant = async () => {
  if (!grantClientId.value || !currentKey.value) {
    ElMessage.warning('请选择应用')
    return
  }
  await grantKeyPermission(currentKey.value.id, grantClientId.value)
  ElMessage.success('授权成功')
  const res = await listKeyPermissions(currentKey.value.id)
  permissions.value = res.data || res || []
  grantClientId.value = ''
}

const handleRevoke = async (row) => {
  await ElMessageBox.confirm('确认撤销该授权吗？', '提示', { type: 'warning' })
  await revokeKeyPermission(row.id)
  ElMessage.success('授权已撤销')
  const res = await listKeyPermissions(currentKey.value.id)
  permissions.value = res.data || res || []
}

const handleSearch = () => {
  currentPage.value = 1
  loadKeys()
}

const handleReset = () => {
  filters.value = { alias: '', algorithm: '' }
  currentPage.value = 1
  loadKeys()
}

onMounted(async () => {
  await loadAvailableApps()
  await loadKeys()
})
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

.permission-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.grant-box {
  display: flex;
  gap: 8px;
}

.subtle {
  margin-left: 8px;
  color: #6b7280;
  font-size: 12px;
}
</style>
