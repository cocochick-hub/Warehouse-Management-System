<template>
  <view class="login-page">
    <view class="login-header">
      <text class="login-title">WMS 仓库管理</text>
      <text class="login-subtitle">手机端</text>
    </view>

    <view class="login-form">
      <view class="input-group">
        <text class="input-label">服务器地址</text>
        <input
          v-model="serverUrl"
          class="input-field"
          placeholder="http://192.168.1.100:8080"
          @blur="onServerUrlBlur"
        />
      </view>
      <view class="input-group">
        <text class="input-label">用户名</text>
        <input
          v-model="form.username"
          class="input-field"
          placeholder="请输入用户名"
        />
      </view>
      <view class="input-group">
        <text class="input-label">密码</text>
        <input
          v-model="form.password"
          class="input-field"
          type="password"
          placeholder="请输入密码"
        />
      </view>
      <button class="login-btn" :disabled="loading" @tap="onLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/store/user'
import { updateBaseUrl, getCurrentBaseUrl } from '@/api/request'

const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const serverUrl = ref(getCurrentBaseUrl())

function onServerUrlBlur() {
  const url = serverUrl.value.trim()
  if (url) {
    updateBaseUrl(url)
  }
}

async function onLogin() {
  if (!form.username || !form.password) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  // 登录前先保存服务器地址（用户可能只改了地址但未触发 blur）
  onServerUrlBlur()
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
.login-form {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.input-group {
  padding: 12px 0;
  border-bottom: 1px solid #ebedf0;
}
.input-label {
  font-size: 14px;
  color: #323233;
  display: block;
  margin-bottom: 6px;
}
.input-field {
  width: 100%;
  font-size: 16px;
  padding: 8px 0;
  border: none;
  outline: none;
}
.login-btn {
  width: 100%;
  margin-top: 16px;
  padding: 12px;
  background: #1989fa;
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 16px;
}
.login-btn[disabled] {
  opacity: 0.6;
}
</style>
