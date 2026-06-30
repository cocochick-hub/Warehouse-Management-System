import { getToken, setToken, setUser, clearAuth } from '@/utils/auth'

const DEFAULT_BASE_URL = 'http://localhost:8080'

let isRefreshing = false
let refreshPromise = null

/** 动态获取 baseUrl，每次请求实时从 storage 读取，修改后无需重启即可生效 */
function getBaseUrl() {
  return uni.getStorageSync('wms_base_url') || DEFAULT_BASE_URL
}

/** 尝试刷新 token，返回新的 token 或 null */
function tryRefreshToken() {
  const oldToken = getToken()
  if (!oldToken) return Promise.resolve(null)

  // 防止并发刷新
  if (isRefreshing && refreshPromise) return refreshPromise

  isRefreshing = true
  refreshPromise = new Promise((resolve) => {
    uni.request({
      url: getBaseUrl() + '/api/auth/refresh',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${oldToken}`
      },
      success(res) {
        const { statusCode, data: body } = res
        if (statusCode === 200 && body.code === 200) {
          const { token, userInfo } = body.data
          setToken(token)
          setUser(userInfo)
          resolve(token)
        } else {
          resolve(null)
        }
      },
      fail() {
        resolve(null)
      },
      complete() {
        isRefreshing = false
        refreshPromise = null
      }
    })
  })
  return refreshPromise
}

/**
 * 发起请求，自动注入 JWT、401 自动刷新 token
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
        } else if (statusCode === 401 && url !== '/api/auth/login' && url !== '/api/auth/refresh') {
          // 尝试刷新 token 后重试
          tryRefreshToken().then((newToken) => {
            if (newToken) {
              // 用新 token 重试原请求
              uni.request({
                url: getBaseUrl() + url,
                method,
                data,
                header: {
                  'Content-Type': 'application/json',
                  'Authorization': `Bearer ${newToken}`
                },
                success(retryRes) {
                  const { statusCode: sc, data: b } = retryRes
                  if (sc === 200 && b.code === 200) {
                    resolve(b)
                  } else {
                    reject(new Error(b?.message || '请求失败'))
                  }
                },
                fail(err) {
                  reject(err)
                }
              })
            } else {
              // 刷新失败，跳转登录
              clearAuth()
              uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
              uni.reLaunch({ url: '/pages/login/index' })
              reject(new Error('Token 已过期'))
            }
          })
        } else if (statusCode === 401) {
          // 登录/刷新接口本身返回 401（如密码错误），显示实际错误信息
          const msg = body?.message || '未授权'
          if (url === '/api/auth/login') {
            uni.showToast({ title: msg, icon: 'none' })
          }
          reject(new Error(msg))
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
