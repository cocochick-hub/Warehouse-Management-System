import { getToken, clearAuth } from '@/utils/auth'

const STORED_BASE_URL = uni.getStorageSync('wms_base_url')
const BASE_URL = STORED_BASE_URL || 'http://10.196.86.149:8080'

/**
 * 发起请求，自动注入 JWT、统一处理错误
 * @param {Object} options - { url, method, data }
 * @returns {Promise}
 */
export function request(options) {
  const { url, method = 'GET', data } = options

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': getToken() ? `Bearer ${getToken()}` : ''
      },
      success(res) {
        const { statusCode, data: body } = res
        if (statusCode === 200 && body.code === 200) {
          resolve(body)
        } else if (statusCode === 401) {
          clearAuth()
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          uni.reLaunch({ url: '/pages/login/index' })
          reject(new Error(body?.message || '未授权'))
        } else {
          uni.showToast({ title: body?.message || `请求失败 (${statusCode})`, icon: 'none' })
          reject(new Error(body?.message || '请求失败'))
        }
      },
      fail(err) {
        uni.showToast({ title: '网络连接异常，请检查服务器', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function updateBaseUrl(url) {
  uni.setStorageSync('wms_base_url', url)
}

export default request
