/**
 * Token 工具模块
 *
 * 统一管理 JWT Token 的存取删操作，
 * 所有模块通过此工具读写 Token，避免直接操作 localStorage。
 */

const TOKEN_KEY = 'wms_token'
const USER_KEY = 'wms_user'

/**
 * 获取存储的 Token
 * @returns {string|null} JWT Token
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 存储 Token
 * @param {string} token JWT Token
 */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 删除 Token（退出登录时调用）
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 获取存储的用户信息
 * @returns {object|null} 用户信息对象
 */
export function getUser() {
  const userStr = localStorage.getItem(USER_KEY)
  return userStr ? JSON.parse(userStr) : null
}

/**
 * 存储用户信息
 * @param {object} user 用户信息对象
 */
export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/**
 * 删除用户信息
 */
export function removeUser() {
  localStorage.removeItem(USER_KEY)
}

/**
 * 清除所有认证信息（退出登录时使用）
 */
export function clearAuth() {
  removeToken()
  removeUser()
}
