<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>密钥列表</span>
          <el-button type="primary" @click="showCreate = true">创建密钥</el-button>
        </div>
      </template>
      <el-table :data="keys" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="alias" label="别名" width="200" />
        <el-table-column prop="algorithm" label="算法" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.algorithm?.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cryptoType" label="类型" width="120">
          <template #default="{ row }">
            {{ {symmetric:'对称',asymmetric:'非对称',sign:'签名',digester:'摘要'}[row.cryptoType] || row.cryptoType }}
          </template>
        </el-table-column>
        <el-table-column prop="clientGroup" label="应用组" width="120" />
        <el-table-column prop="keyVersion" label="版本" width="80" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button v-if="row.enabled" type="danger" size="small" link @click="handleToggle(row, false)">禁用</el-button>
            <el-button v-else type="success" size="small" link @click="handleToggle(row, true)">启用</el-button>
            <el-button type="warning" size="small" link @click="handleRotate(row)">轮转</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建密钥对话框 -->
    <el-dialog v-model="showCreate" title="创建密钥" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="别名">
          <el-input v-model="form.alias" placeholder="如 my-app-db-pwd" />
        </el-form-item>
        <el-form-item label="算法">
          <el-select v-model="form.algorithm" placeholder="选择算法">
            <el-option label="AES（对称）" value="aes" />
            <el-option label="SM4（国密对称）" value="sm4" />
            <el-option label="RSA（非对称）" value="rsa" />
            <el-option label="SM2（国密非对称）" value="sm2" />
            <el-option label="签名算法" value="sign" />
          </el-select>
        </el-form-item>
        <el-form-item label="应用组">
          <el-input v-model="form.clientGroup" placeholder="默认 default" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listKeys, createKey, enableKey, disableKey, rotateKey } from '../api'

const keys = ref([])
const showCreate = ref(false)
const form = ref({ alias: '', algorithm: 'aes', clientGroup: 'default', description: '' })

const loadKeys = async () => {
  const res = await listKeys()
  keys.value = res.data || []
}

const handleCreate = async () => {
  if (!form.value.alias || !form.value.algorithm) {
    ElMessage.warning('请填写别名和算法')
    return
  }
  await createKey(form.value)
  ElMessage.success('密钥创建成功')
  showCreate.value = false
  form.value = { alias: '', algorithm: 'aes', clientGroup: 'default', description: '' }
  loadKeys()
}

const handleToggle = async (row, enabled) => {
  await (enabled ? enableKey(row.id) : disableKey(row.id))
  ElMessage.success(enabled ? '已启用' : '已禁用')
  loadKeys()
}

const handleRotate = async (row) => {
  await ElMessageBox.confirm(`确认轮转密钥 "${row.alias}"？将生成新版本密钥。`, '确认轮转')
  await rotateKey(row.id)
  ElMessage.success('轮转成功')
  loadKeys()
}

onMounted(loadKeys)
</script>
