import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器：自动携带 Token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('kms_token')
  if (token) {
    config.headers['satoken'] = token
  }
  return config
})

// 响应拦截器
http.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      // 未登录或 token 过期，跳转登录
      if (res.code === 401) {
        localStorage.removeItem('kms_token')
        localStorage.removeItem('kms_user')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('kms_token')
      localStorage.removeItem('kms_user')
      window.location.href = '/login'
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default http
