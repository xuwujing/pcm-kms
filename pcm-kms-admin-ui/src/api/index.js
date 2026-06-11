import http from './http'

// 登录
export const login = (username, password) => http.post('/auth/login', { username, password }).then(res => res.data || res)

// 应用管理
export const listApps = (page = 1, size = 20) => http.get('/admin/apps', { params: { page, size } })
export const createApp = (data) => http.post('/admin/apps', data)
export const getApp = (id) => http.get(`/admin/apps/${id}`)
export const enableApp = (id) => http.post(`/admin/apps/${id}/enable`)
export const disableApp = (id) => http.post(`/admin/apps/${id}/disable`)
export const deleteApp = (id) => http.delete(`/admin/apps/${id}`)

// 密钥管理
export const listKeys = (params) => http.get('/admin/keys', { params })
export const createKey = (data) => http.post('/admin/keys', data)
export const getKey = (id) => http.get(`/admin/keys/${id}`)
export const enableKey = (id) => http.post(`/admin/keys/${id}/enable`)
export const disableKey = (id) => http.post(`/admin/keys/${id}/disable`)
export const rotateKey = (id) => http.post(`/admin/keys/${id}/rotate`)
export const deleteKey = (id) => http.delete(`/admin/keys/${id}`)

// 密钥授权
export const listKeyPermissions = (keyId) => http.get(`/admin/keys/${keyId}/permissions`)
export const grantKeyPermission = (keyId, clientId) => http.post(`/admin/keys/${keyId}/grant`, { clientId })
export const revokeKeyPermission = (permissionId) => http.delete(`/admin/keys/permission/${permissionId}`)

// 加密服务
export const encrypt = (data) => http.post('/crypto/encrypt', data)
export const decrypt = (data) => http.post('/crypto/decrypt', data)
export const sign = (data) => http.post('/crypto/sign', data)
export const verify = (data) => http.post('/crypto/verify', data)
export const digest = (data) => http.post('/crypto/digest', data)
export const getPublicKey = (alias, clientGroup) => http.get(`/crypto/public-key/${alias}`, { params: { clientGroup } })

// 限流配置
export const getRateLimitConfig = () => http.get('/admin/ratelimit')
export const updateRateLimitConfig = (data) => http.put('/admin/ratelimit', data)
