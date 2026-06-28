import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, getUser, setUser, removeUser } from '@/utils/auth'
import { loginApi, getUserInfoApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: getUser()
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo?.username || ''
  },

  actions: {
    async login(username, password) {
      const res = await loginApi(username, password)
      const { token, userInfo } = res.data
      this.token = token
      this.userInfo = userInfo
      setToken(token)
      setUser(userInfo)
      return userInfo
    },

    checkLogin() {
      if (!this.token) {
        uni.reLaunch({ url: '/pages/login/index' })
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
      removeUser()
      uni.reLaunch({ url: '/pages/login/index' })
    }
  }
})
