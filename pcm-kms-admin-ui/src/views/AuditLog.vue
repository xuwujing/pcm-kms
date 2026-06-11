<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>审计日志</span>
          <el-button @click="loadLogs" size="small">刷新</el-button>
        </div>
      </template>
      <!-- 搜索条件 -->
      <el-form :inline="true" style="margin-bottom: 16px;">
        <el-form-item label="操作类型">
          <el-input v-model="query.operation" placeholder="如 创建应用" clearable />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operator" placeholder="如 admin" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="currentPage = 1; loadLogs()">查询</el-button>
          <el-button @click="query = { operation: '', operator: '' }; loadLogs()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="logs" stripe style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="operation" label="操作类型" width="120" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="resource" label="资源类型" width="100" />
        <el-table-column prop="resourceId" label="资源ID" width="180" show-overflow-tooltip />
        <el-table-column prop="result" label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === '成功' ? 'success' : 'danger'" size="small">
              {{ row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="操作时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right;"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadLogs"
        @size-change="loadLogs"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'
import { formatTime } from '../utils/format' = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const query = ref({ operation: '', operator: '' })

const loadLogs = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (query.value.operation) params.operation = query.value.operation
    if (query.value.operator) params.operator = query.value.operator
    const res = await http.get('/admin/audit', { params })
    const data = res.data || res
    if (data.records) {
      logs.value = data.records
      total.value = data.total
    } else {
      logs.value = Array.isArray(data) ? data : []
      total.value = logs.value.length
    }
  } catch (e) {} finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>
