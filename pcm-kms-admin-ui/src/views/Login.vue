<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div style="text-align: center; font-size: 22px; font-weight: bold; color: #304156;">
          PCM-KMS 登录
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="loginForm" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large"
                    show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" size="large" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div style="text-align: center; color: #999; font-size: 12px;">
        默认账号：admin / 123456
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter()
const loginForm = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await loginForm.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form.username, form.password)
    if (res.token) {
      localStorage.setItem('kms_token', res.token)
      localStorage.setItem('kms_user', JSON.stringify({ username: form.username }))
      router.push('/dashboard')
    }
  } catch (e) {
    // 错误已在 http 拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  border-radius: 8px;
}

.login-card :deep(.el-card__header) {
  padding: 20px 20px 10px;
  border-bottom: none;
}
</style>
