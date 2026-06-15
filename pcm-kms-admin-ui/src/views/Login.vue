<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="login-title">PCM-KMS 管理台</div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            size="large"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
      <div class="login-hint">默认账号：admin / 123456</div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const res = await login(form.username, form.password)
    const token = res?.token || res?.data?.token
    if (!token) {
      throw new Error('登录响应缺少 token')
    }
    localStorage.setItem('kms_token', token)
    localStorage.setItem('kms_user', JSON.stringify({ username: form.username }))
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top left, rgba(34, 197, 94, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(59, 130, 246, 0.22), transparent 26%),
    linear-gradient(135deg, #f8fafc, #e5e7eb);
}

.login-card {
  width: 420px;
  border-radius: 16px;
}

.login-title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.login-button {
  width: 100%;
}

.login-hint {
  margin-top: 16px;
  color: #6b7280;
  font-size: 12px;
  text-align: center;
}
</style>
