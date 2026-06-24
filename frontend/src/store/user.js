import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, getUser, setUser, removeUser } from '@/utils/auth'
import { loginApi, getUserInfoApi } from '@/api/auth'

/**
 * 用户状态管理（Pinia）
 *
 * 集中管理登录状态、用户信息，整个应用共享
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    /** 用户 Token */
    token: getToken(),
    /** 用户信息 */
    userInfo: getUser()
  }),

  getters: {
    /** 是否已登录（有 Token 即为已登录） */
    isLoggedIn: (state) => !!state.token,

    /** 当前用户名 */
    username: (state) => state.userInfo?.username || '',

    /** 当前用户角色 */
    role: (state) => state.userInfo?.role || ''
  },

  actions: {
    /**
     * 登录
     * @param {string} username 用户名
     * @param {string} password 密码
     */
    async login(username, password) {
      const res = await loginApi(username, password)
      const { token, userInfo } = res.data

      // 保存到 Pinia 状态
      this.token = token
      this.userInfo = userInfo

      // 持久化到 localStorage
      setToken(token)
      setUser(userInfo)

      return userInfo
    },

    /**
     * 获取用户信息（刷新页面时调用，用 Token 重新获取）
     */
    async fetchUserInfo() {
      if (!this.token) return null

      try {
        const res = await getUserInfoApi()
        this.userInfo = res.data
        setUser(res.data)
        return res.data
      } catch {
        // Token 无效，清除状态
        this.logout()
        return null
      }
    },

    /**
     * 直接设置用户信息（更新个人信息后使用）
     */
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      setUser(userInfo)
    },

    /**
     * 退出登录
     */
    logout() {
      this.token = null
      this.userInfo = null
      removeToken()
      removeUser()
    }
  }
})
