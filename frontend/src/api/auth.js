import request from './request'

/**
 * 认证相关 API
 */

/**
 * 登录
 * @param {string} username 用户名
 * @param {string} password 密码
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
 * 获取当前登录用户信息
 * @returns {Promise} 用户信息
 */
export function getUserInfoApi() {
  return request({
    url: '/auth/userInfo',
    method: 'get'
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
