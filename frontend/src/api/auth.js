import request from './request'

/**
 * 璁よ瘉鐩稿叧 API
 */

/**
 * 鐧诲綍
 * @param {string} username 鐢ㄦ埛鍚?
 * @param {string} password 瀵嗙爜
 * @returns {Promise} { token, tokenType, userInfo }
 */
export function loginApi(username, password) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: { username, password }
  })
}

/**
 * 鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛淇℃伅
 * @returns {Promise} 鐢ㄦ埛淇℃伅
 */
export function getUserInfoApi() {
  return request({
    url: '/auth/userInfo',
    method: 'get'
  })
}

/**
 * 淇敼瀵嗙爜
 * @param {string} oldPassword 鏃у瘑鐮?
 * @param {string} newPassword 鏂板瘑鐮?
 * @returns {Promise}
 */
export function changePasswordApi(oldPassword, newPassword) {
  return request({
    url: '/auth/changePassword',
    method: 'put',
    data: { oldPassword, newPassword }
  })
}

/**
 * 退出登录
 * @returns {Promise}
 */
export function logoutApi() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 更新用户信息
 * @param {object} data { phone: string }
 * @returns {Promise}
 */
export function updateUserInfoApi(data) {
  return request({
    url: '/auth/userInfo',
    method: 'put',
    data
  })
}
