<template>
  <view class="login-page">
    <view class="login-header">
      <text class="login-title">WMS 仓库管理</text>
      <text class="login-subtitle">手机端</text>
    </view>

    <van-form>
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
        />
        <van-field
          v-model="form.password"
          name="password"
          label="密码"
          type="password"
          placeholder="请输入密码"
        />
      </van-cell-group>

      <view style="margin: 16px">
        <van-button round block type="primary" :loading="loading" @click="onLogin">
          登录
        </van-button>
      </view>
    </van-form>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Button, Form, Field, CellGroup } from 'vant'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function onLogin() {
  if (!form.username || !form.password) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    uni.reLaunch({ url: '/pages/home/index' })
  } catch {
    // 错误已由 request 拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background-color: #1989fa;
  padding: 0 16px;
}
.login-header {
  text-align: center;
  margin-bottom: 40px;
}
.login-title { font-size: 28px; color: #fff; font-weight: bold; }
.login-subtitle { font-size: 14px; color: rgba(255,255,255,0.8); margin-top: 8px; }
</style>
