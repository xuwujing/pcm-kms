<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>审计日志</span>
        <el-button size="small" @click="loadLogs">刷新</el-button>
      </div>
    </template>

    <el-form :inline="true" class="toolbar">
      <el-form-item label="操作类型">
        <el-input v-model="filters.operation" clearable placeholder="create-key" />
      </el-form-item>
      <el-form-item label="操作人">
        <el-input v-model="filters.operator" clearable placeholder="admin" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="logs" stripe>
      <el-table-column type="index" label="序号" width="70" />
      <el-table-column prop="operation" label="操作类型" width="160" />
      <el-table-column prop="operator" label="操作人" width="120" />
      <el-table-column prop="resource" label="资源" width="140" />
      <el-table-column prop="resourceId" label="资源 ID" min-width="160" />
      <el-table-column prop="result" label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="normalizeResult(row.result) ? 'success' : 'danger'">
            {{ row.result || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      class="pagination"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @current-change="loadLogs"
      @size-change="loadLogs"
    />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api/http'
import { formatTime } from '../utils/format'

const logs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filters = ref({
  operation: '',
  operator: '',
})

const normalizeResult = (result) => {
  if (!result) {
    return false
  }
  const value = String(result).toLowerCase()
  return ['success', 'ok', 'true', '1'].includes(value) || result === '成功'
}

const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...(filters.value.operation ? { operation: filters.value.operation } : {}),
      ...(filters.value.operator ? { operator: filters.value.operator } : {}),
    }
    const res = await http.get('/admin/audit', { params })
    const data = res.data || res
    logs.value = data.records || (Array.isArray(data) ? data : [])
    total.value = data.total || logs.value.length
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadLogs()
}

const handleReset = () => {
  filters.value = { operation: '', operator: '' }
  currentPage.value = 1
  loadLogs()
}

onMounted(loadLogs)
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
</style>
