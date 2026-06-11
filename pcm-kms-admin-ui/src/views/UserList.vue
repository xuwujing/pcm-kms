<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>用户管理</span>
          <el-button type="primary" @click="showCreate = true">新增用户</el-button>
        </div>
      </template>
      <el-form :inline="true" style="margin-bottom: 16px;">
        <el-form-item label="用户名">
          <el-input v-model="userQuery" placeholder="模糊搜索" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">查询</el-button>
          <el-button @click="userQuery = ''; loadUsers()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="users" stripe style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="enabled" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.enabled" type="danger" size="small" link @click="handleToggle(row, false)">禁用</el-button>
            <el-button v-else type="success" size="small" link @click="handleToggle(row, true)">启用</el-button>
            <el-button type="danger" size="small" link @click="handleResetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑用户对话框 -->
    <el-dialog v-model="showCreate" :title="editing ? '编辑用户' : '新增用户'" width="450px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="editing" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" required v-if="!editing">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { formatTime } from '../utils/format'

const users = ref([])
const loading = ref(false)
const showCreate = ref(false)
const editing = ref(false)
const userQuery = ref('')
const form = ref({ username: '', nickname: '', password: '' })

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await http.get('/admin/users')
    let list = res.data || res || []
    if (userQuery.value) {
      list = list.filter(u => u.username?.includes(userQuery.value) || u.nickname?.includes(userQuery.value))
    }
    users.value = list
  } catch (e) {
    // 接口可能还不存在，容错处理
    users.value = []
  } finally {
    loading.value = false
  }
}

const handleEdit = (row) => {
  editing.value = true
  form.value = { ...row }
  showCreate.value = true
}

const handleSubmit = async () => {
  if (!form.value.username || (!editing.value && !form.value.password)) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    if (editing.value) {
      await http.put('/admin/users', form.value)
    } else {
      await http.post('/admin/users', form.value)
    }
    ElMessage.success(editing.value ? '编辑成功' : '创建成功')
    showCreate.value = false
    editing.value = false
    form.value = { username: '', nickname: '', password: '' }
    loadUsers()
  } catch (e) {
    // 错误已在拦截器处理
  }
}

const handleToggle = async (row, enabled) => {
  try {
    await http.post(`/admin/users/${row.id}/enable?enabled=${enabled}`)
    ElMessage.success(enabled ? '已启用' : '已禁用')
    loadUsers()
  } catch (e) {}
}

const handleResetPwd = async (row) => {
  await ElMessageBox.confirm(`确认重置用户 "${row.username}" 的密码为默认密码？`, '确认重置')
  try {
    await http.post(`/admin/users/${row.id}/reset-password`)
    ElMessage.success('密码已重置')
  } catch (e) {}
}

onMounted(loadUsers)
</script>
