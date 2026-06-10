import http from './http'

// 应用管理
export const listApps = () => http.get('/admin/apps')
export const createApp = (data) => http.post('/admin/apps', data)
export const getApp = (id) => http.get(`/admin/apps/${id}`)
export const enableApp = (id) => http.post(`/admin/apps/${id}/enable`)

// 密钥管理
export const listKeys = (clientGroup) => http.get('/admin/keys', { params: { clientGroup } })
export const createKey = (data) => http.post('/admin/keys', data)
export const getKey = (id) => http.get(`/admin/keys/${id}`)
export const enableKey = (id) => http.post(`/admin/keys/${id}/enable`)
export const disableKey = (id) => http.post(`/admin/keys/${id}/disable`)
export const rotateKey = (id) => http.post(`/admin/keys/${id}/rotate`)

// 加密服务
export const encrypt = (data) => http.post('/crypto/encrypt', data)
export const decrypt = (data) => http.post('/crypto/decrypt', data)
export const sign = (data) => http.post('/crypto/sign', data)
export const verify = (data) => http.post('/crypto/verify', data)
export const digest = (data) => http.post('/crypto/digest', data)
export const getPublicKey = (alias, clientGroup) => http.get(`/crypto/public-key/${alias}`, { params: { clientGroup } })
