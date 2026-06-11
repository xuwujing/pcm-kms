<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>应用列表</span>
          <el-button type="primary" @click="openCreate">创建应用</el-button>
        </div>
      </template>
      <!-- 搜索 -->
      <el-form :inline="true" style="margin-bottom: 16px;">
        <el-form-item label="服务标识">
          <el-input v-model="query.clientId" placeholder="如 kms" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="应用名称">
          <el-input v-model="query.clientName" placeholder="模糊搜索" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.enabled" placeholder="全部" clearable style="width: 100px;">
            <el-option label="已启用" value="true" />
            <el-option label="未启用" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="currentPage = 1; loadApps()">查询</el-button>
          <el-button @click="query = { clientId: '', clientName: '', enabled: '' }; loadApps()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="apps" stripe style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="clientId" label="服务标识" width="160" />
        <el-table-column prop="clientName" label="应用名称" min-width="150" />
        <el-table-column prop="clientGroup" label="应用组" width="100" />
        <el-table-column prop="enabled" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contacts" label="联系人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="240" align="center">
          <template #default="{ row }">
            <el-button v-if="!row.enabled" type="success" size="small" link @click="handleEnable(row)">启用</el-button>
            <el-button v-if="row.enabled" type="warning" size="small" link @click="handleDisable(row)">停用</el-button>
            <el-button type="primary" size="small" link @click="handleView(row)">详情</el-button>
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
        @current-change="loadApps"
        @size-change="loadApps"
      />
    </el-card>

    <!-- 创建应用对话框 -->
    <el-dialog v-model="showCreate" title="创建应用" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="服务标识" required>
          <el-input v-model="form.clientId" placeholder="如 kms、order-service（唯一标识）" />
        </el-form-item>
        <el-form-item label="应用名称" required>
          <el-input v-model="form.clientName" placeholder="如 密钥管理系统" />
        </el-form-item>
        <el-form-item label="应用组">
          <el-input v-model="form.clientGroup" placeholder="默认 default" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contacts" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="工号">
          <el-input v-model="form.jobNo" placeholder="请输入工号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="showDetail" title="应用详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentApp">
        <el-descriptions-item label="服务标识">{{ currentApp.clientId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应用名称">{{ currentApp.clientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="应用组">{{ currentApp.clientGroup || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentApp.enabled ? 'success' : 'info'" size="small">
            {{ currentApp.enabled ? '已启用' : '未启用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Client Secret" :span="2" v-if="currentApp.clientSecret">
          <div style="display: flex; align-items: center; gap: 8px;">
            <code style="flex:1; word-break: break-all; font-size: 12px; padding: 4px 8px; background: #f5f5f5; border-radius: 4px;">{{ currentApp.clientSecret }}</code>
            <el-button size="small" @click="copyText(currentApp.clientSecret)">复制</el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ currentApp.contacts || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentApp.mobile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工号">{{ currentApp.jobNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(currentApp.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(currentApp.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="签名公钥" :span="2" v-if="currentApp.signPublicKey">
          <el-input :model-value="currentApp.signPublicKey" readonly type="textarea" :rows="3" size="small" />
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApps, createApp, getApp } from '../api'
import http from '../api/http'
import { formatTime } from '../utils/format'

const apps = ref([])
const loading = ref(false)
const showCreate = ref(false)
const showDetail = ref(false)
const currentApp = ref(null)
const creating = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const query = ref({ clientId: '', clientName: '', enabled: '' })
const form = ref({ clientId: '', clientName: '', clientGroup: 'default', contacts: '', mobile: '', jobNo: '' })

const loadApps = async () => {
  loading.value = true
  try {
    const res = await listApps(currentPage.value, pageSize.value)
    const data = res.data || res
    let records = data.records || (Array.isArray(data) ? data : [])
    total.value = data.total || records.length
    // 前端过滤
    if (query.value.clientId) {
      records = records.filter(r => r.clientId?.includes(query.value.clientId))
    }
    if (query.value.clientName) {
      records = records.filter(r => r.clientName?.includes(query.value.clientName))
    }
    if (query.value.enabled !== '') {
      const enabled = query.value.enabled === 'true'
      records = records.filter(r => r.enabled === enabled)
    }
    apps.value = records
  } catch (e) {} finally {
    loading.value = false
  }
}

const openCreate = () => {
  form.value = { clientId: '', clientName: '', clientGroup: 'default', contacts: '', mobile: '', jobNo: '' }
  showCreate.value = true
}

const handleCreate = async () => {
  if (!form.value.clientId || !form.value.clientName) {
    ElMessage.warning('请填写服务标识和应用名称')
    return
  }
  creating.value = true
  try {
    await createApp(form.value)
    ElMessage.success('创建成功')
    showCreate.value = false
    loadApps()
  } catch (e) {} finally {
    creating.value = false
  }
}

const handleEnable = async (row) => {
  await ElMessageBox.confirm('启用后将生成 ClientSecret 和默认密钥，确认启用？', '确认启用', { type: 'warning' })
  try {
    await http.post(`/admin/apps/${row.id}/enable`)
    ElMessage.success('启用成功')
    loadApps()
  } catch (e) {}
}

const handleDisable = async (row) => {
  await ElMessageBox.confirm('停用后该应用将无法调用加解密接口，确认停用？', '确认停用', { type: 'warning' })
  try {
    await http.post(`/admin/apps/${row.id}/disable`)
    ElMessage.success('已停用')
    loadApps()
  } catch (e) {}
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除应用 "${row.clientName}"？此操作不可恢复。`, '确认删除', { type: 'error' })
  try {
    await http.delete(`/admin/apps/${row.id}`)
    ElMessage.success('已删除')
    loadApps()
  } catch (e) {}
}

const handleView = async (row) => {
  try {
    const res = await getApp(row.id)
    currentApp.value = res.data || res
    showDetail.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const copyText = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.warning('复制失败，请手动复制')
  })
}

onMounted(loadApps)
</script>
