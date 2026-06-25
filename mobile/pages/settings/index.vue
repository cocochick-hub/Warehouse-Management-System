<template>
  <view class="settings-page">
    <van-nav-bar title="设置" left-arrow @click-left="goBack" fixed safe-area-inset-top />

    <view class="settings-body">
      <van-cell-group title="服务器配置">
        <van-field
          v-model="serverUrl"
          label="服务器地址"
          placeholder="http://192.168.1.100:8080"
        />
      </van-cell-group>

      <view style="margin: 16px">
        <van-button type="primary" block round @click="onSave">保存</van-button>
        <van-button plain block round style="margin-top: 12px" @click="onLogout">
          退出登录
        </van-button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { NavBar, Field, CellGroup, Button } from 'vant'
import { useUserStore } from '@/store/user'
import { updateBaseUrl, getCurrentBaseUrl } from '@/api/request'

const userStore = useUserStore()
const serverUrl = ref(getCurrentBaseUrl())

function onSave() {
  updateBaseUrl(serverUrl.value.trim())
  uni.showToast({ title: '保存成功', icon: 'success' })
}

function onLogout() {
  userStore.logout()
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped>
.settings-page { min-height: 100vh; }
.settings-body { margin-top: 46px; }
</style>
