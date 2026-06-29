<template>
  <view class="home-page">
    <van-nav-bar title="WMS 仓库管理" fixed safe-area-inset-top>
      <template #right>
        <van-icon name="setting-o" size="20" @click="onSettings" />
      </template>
    </van-nav-bar>

    <view class="home-body">
      <van-grid :column-num="3" :border="false" :gutter="12">
        <van-grid-item
          v-for="item in menuItems"
          :key="item.path"
          use-slot
          @click="navigateTo(item.path)"
        >
          <view class="grid-icon" :style="{ backgroundColor: item.color }">
            <van-icon :name="item.icon" size="28" color="#fff" />
          </view>
          <text class="grid-label">{{ item.label }}</text>
        </van-grid-item>
      </van-grid>
    </view>
  </view>
</template>

<script setup>
import { NavBar, Icon, Grid, GridItem } from 'vant'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const menuItems = [
  { label: '扫码入库', icon: 'add-o', color: '#07c160', path: '/pages/inbound-scan/index' },
  { label: '扫码出库', icon: 'delete-o', color: '#ee0a24', path: '/pages/outbound-scan/index' },
  { label: '带单出库', icon: 'orders-o', color: '#ff976a', path: '/pages/outbound-order/index' },
  { label: '退库', icon: 'revoke', color: '#1989fa', path: '/pages/return/index' },
  { label: '转包', icon: 'exchange', color: '#ff976a', path: '/pages/transfer/index' },
  { label: '封存', icon: 'lock', color: '#9e9e9e', path: '/pages/seal/index' },
  { label: '解封', icon: 'unlock', color: '#9e9e9e', path: '/pages/seal/index' },
  { label: '盘点', icon: 'search', color: '#1989fa', path: '/pages/check/index' },
]

function navigateTo(path) {
  if (!path) {
    uni.showToast({ title: '功能开发中', icon: 'none' })
    return
  }
  uni.navigateTo({ url: path })
}

function onSettings() {
  uni.navigateTo({ url: '/pages/settings/index' })
}
</script>

<style scoped>
.home-page { min-height: 100vh; }
.home-body { padding: 40px 16px 16px; margin-top: 46px; }
.grid-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 8px;
}
.grid-label { font-size: 13px; color: #323233; }
</style>
