import { getToken, clearAuth } from '@/utils/auth'

const DEFAULT_BASE_URL = 'http://10.196.86.149:8080'

/** 动态获取 baseUrl，每次请求实时从 storage 读取，修改后无需重启即可生效 */
function getBaseUrl() {
  return uni.getStorageSync('wms_base_url') || DEFAULT_BASE_URL
}

/**
 * 发起请求，自动注入 JWT、统一处理错误
 * @param {Object} options - { url, method, data }
 * @returns {Promise}
 */
export function request(options) {
  const { url, method = 'GET', data } = options

  return new Promise((resolve, reject) => {
    uni.request({
      url: getBaseUrl() + url,
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

/** 获取当前生效的服务器地址（供页面初始化显示） */
export function getCurrentBaseUrl() {
  return getBaseUrl()
}

export default request
