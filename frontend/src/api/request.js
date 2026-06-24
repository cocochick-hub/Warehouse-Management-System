import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth'
import router from '@/router'

/**
 * Axios 实例 - 统一请求配置
 *
 * 功能：
 * 1. 请求拦截器：自动携带 JWT Token
 * 2. 响应拦截器：统一处理错误码，401 自动跳转登录
 * 3. 基础 URL 配置（开发环境通过 Vite 代理）
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ==================== 请求拦截器 ====================
request.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 Token，添加到请求头
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器 ====================
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 { code, message, data, timestamp }
    if (res.code === 200) {
      return res
    }
    // 业务错误（如参数校验失败）
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          clearAuth()
          ElMessage.error('用户名或密码错误！')
          router.push('/login')
          break
        case 403:
          ElMessage.error('权限不足，无法访问')
          break
        case 400:
          ElMessage.error(data?.message || '请求参数错误')
          break
        case 404:
          ElMessage.error(data?.message || '请求的资源不存在')
          break
        case 500:
          ElMessage.error(data?.message || '服务器内部错误')
          break
        default:
          ElMessage.error(`请求失败 (${status})`)
      }
    } else {
      // 网络错误（如后端未启动）
      ElMessage.error('网络连接异常，请检查服务器是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
