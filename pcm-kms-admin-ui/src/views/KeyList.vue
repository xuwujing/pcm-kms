<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>密钥列表</span>
          <el-button type="primary" @click="openCreate">创建密钥</el-button>
        </div>
      </template>
      <el-form :inline="true" style="margin-bottom: 16px;">
        <el-form-item label="别名">
          <el-input v-model="keyQuery.alias" placeholder="模糊搜索" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="算法">
          <el-select v-model="keyQuery.algorithm" placeholder="全部" clearable style="width: 120px;">
            <el-option label="AES" value="aes" />
            <el-option label="SM4" value="sm4" />
            <el-option label="RSA" value="rsa" />
            <el-option label="SM2" value="sm2" />
            <el-option label="签名" value="sign" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="currentPage = 1; loadKeys()">查询</el-button>
          <el-button @click="keyQuery = { alias: '', algorithm: '' }; loadKeys()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="keys" stripe style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="alias" label="别名" min-width="160" />
        <el-table-column prop="algorithm" label="算法" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.algorithm?.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cryptoType" label="类型" width="90" align="center">
          <template #default="{ row }">
            {{ {symmetric:'对称',asymmetric:'非对称',sign:'签名',digester:'摘要'}[row.cryptoType] || row.cryptoType }}
          </template>
        </el-table-column>
        <el-table-column prop="clientGroup" label="应用组" width="100" />
        <el-table-column prop="keyVersion" label="版本" width="70" align="center" />
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="260" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleViewAuth(row)">授权</el-button>
            <el-button v-if="row.enabled" type="warning" size="small" link @click="handleToggle(row, false)">禁用</el-button>
            <el-button v-else type="success" size="small" link @click="handleToggle(row, true)">启用</el-button>
            <el-button type="primary" size="small" link @click="handleRotate(row)">轮转</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right;"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadKeys"
        @size-change="loadKeys"
      />
    </el-card>

    <!-- 创建密钥对话框 -->
    <el-dialog v-model="showCreate" title="创建密钥" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="绑定应用" required>
          <el-select v-model="form.clientId" placeholder="选择应用" style="width: 100%" filterable>
            <el-option
              v-for="app in enabledApps"
              :key="app.clientId"
              :label="`${app.clientName} (${app.clientId})`"
              :value="app.clientId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="别名" required>
          <el-input v-model="form.alias" placeholder="如 user-phone-aes" />
        </el-form-item>
        <el-form-item label="算法" required>
          <el-select v-model="form.algorithm" placeholder="选择算法" style="width: 100%">
            <el-option label="AES（对称加密）" value="aes" />
            <el-option label="SM4（国密对称）" value="sm4" />
            <el-option label="RSA（非对称加密）" value="rsa" />
            <el-option label="SM2（国密非对称）" value="sm2" />
            <el-option label="签名算法（SHA256withRSA）" value="sign" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="密钥用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">确定</el-button>
      </template>
    </el-dialog>

    <!-- 授权管理对话框 -->
    <el-dialog v-model="showAuth" title="密钥授权管理" width="600px" destroy-on-close>
      <div style="margin-bottom: 16px;">
        <span style="font-weight: bold;">密钥：</span>{{ currentKey?.alias }}
        <span style="margin-left: 12px; color: #909399;">{{ currentKey?.algorithm?.toUpperCase() }}</span>
      </div>
      <el-table :data="permissions" stripe size="small">
        <el-table-column prop="clientId" label="应用 Client ID" min-width="200" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? '有效' : '已撤销' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleRevoke(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; gap: 8px;">
        <el-select v-model="grantClientId" placeholder="选择应用" size="small" filterable style="flex: 1">
          <el-option
            v-for="app in enabledApps"
            :key="app.clientId"
            :label="`${app.clientName} (${app.clientId})`"
            :value="app.clientId"
          />
        </el-select>
        <el-button type="primary" size="small" @click="handleGrant">授权</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listKeys, createKey, enableKey, disableKey, rotateKey, listApps, listKeyPermissions, grantKeyPermission, revokeKeyPermission } from '../api'
import http from '../api/http'
import { formatTime } from '../utils/format'

const keys = ref([])
const loading = ref(false)
const showCreate = ref(false)
const showAuth = ref(false)
const creating = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const keyQuery = ref({ alias: '', algorithm: '' })
const form = ref({ alias: '', algorithm: 'aes', clientId: '', description: '' })
const enabledApps = ref([])
const currentKey = ref(null)
const permissions = ref([])
const grantClientId = ref('')

const loadKeys = async () => {
  loading.value = true
  try {
    const res = await listKeys({ page: currentPage.value, size: pageSize.value })
    const data = res.data || res
    let records = data.records || (Array.isArray(data) ? data : [])
    total.value = data.total || records.length
    if (keyQuery.value.alias) {
      records = records.filter(r => r.alias?.includes(keyQuery.value.alias))
    }
    if (keyQuery.value.algorithm) {
      records = records.filter(r => r.algorithm === keyQuery.value.algorithm)
    }
    keys.value = records
  } catch (e) {} finally {
    loading.value = false
  }
}

const loadEnabledApps = async () => {
  try {
    const res = await listApps(1, 1000)
    const data = res.data || res
    const appList = data.records || (Array.isArray(data) ? data : [])
    enabledApps.value = appList.filter(a => a.enabled && a.clientId)
  } catch (e) {}
}

const openCreate = async () => {
  form.value = { alias: '', algorithm: 'aes', clientId: '', description: '' }
  await loadEnabledApps()
  showCreate.value = true
}

const handleCreate = async () => {
  if (!form.value.alias || !form.value.algorithm || !form.value.clientId) {
    ElMessage.warning('请填写必填项')
    return
  }
  creating.value = true
  try {
    await createKey(form.value)
    ElMessage.success('密钥创建成功')
    showCreate.value = false
    loadKeys()
  } catch (e) {} finally {
    creating.value = false
  }
}

const handleToggle = async (row, enabled) => {
  try {
    await (enabled ? enableKey(row.id) : disableKey(row.id))
    ElMessage.success(enabled ? '已启用' : '已禁用')
    loadKeys()
  } catch (e) {}
}

const handleRotate = async (row) => {
  await ElMessageBox.confirm(`确认轮转密钥 "${row.alias}"（v${row.keyVersion}）？将生成新版本密钥。`, '确认轮转', { type: 'warning' })
  try {
    await rotateKey(row.id)
    ElMessage.success('轮转成功')
    loadKeys()
  } catch (e) {}
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除密钥 "${row.alias}"？此操作不可恢复。`, '确认删除', { type: 'error' })
  try {
    await http.delete(`/admin/keys/${row.id}`)
    ElMessage.success('删除成功')
    loadKeys()
  } catch (e) {}
}

const handleViewAuth = async (row) => {
  currentKey.value = row
  await loadEnabledApps()
  grantClientId.value = ''
  try {
    const res = await listKeyPermissions(row.id)
    permissions.value = res.data || res || []
  } catch (e) {
    permissions.value = []
  }
  showAuth.value = true
}

const handleGrant = async () => {
  if (!grantClientId.value) {
    ElMessage.warning('请选择应用')
    return
  }
  try {
    await grantKeyPermission(currentKey.value.id, grantClientId.value)
    ElMessage.success('授权成功')
    grantClientId.value = ''
    // 刷新授权列表
    const res = await listKeyPermissions(currentKey.value.id)
    permissions.value = res.data || res || []
  } catch (e) {}
}

const handleRevoke = async (row) => {
  await ElMessageBox.confirm('确认撤销此授权？', '确认撤销', { type: 'warning' })
  try {
    await revokeKeyPermission(row.id)
    ElMessage.success('已撤销')
    const res = await listKeyPermissions(currentKey.value.id)
    permissions.value = res.data || res || []
  } catch (e) {}
}

onMounted(() => {
  loadKeys()
  loadEnabledApps()
})
</script>
