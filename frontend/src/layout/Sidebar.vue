<template>
  <div class="sidebar">
    <div class="sidebar-logo">
      <el-icon :size="28" color="#409eff"><Box /></el-icon>
      <span v-show="!isCollapse" class="sidebar-title">WMS 管理系统</span>
    </div>
    <el-menu
      :default-active="route.path"
      :collapse="isCollapse"
      :collapse-transition="false"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><Odometer /></el-icon>
        <span>仪表盘</span>
      </el-menu-item>

      <el-sub-menu v-if="hasRole('admin', 'manager')" index="basic">
        <template #title>
          <el-icon><FolderOpened /></el-icon>
          <span>基础信息</span>
        </template>
        <el-menu-item index="/basic/material">
          <el-icon><Tickets /></el-icon>
          <span>物料管理</span>
        </el-menu-item>
        <el-menu-item index="/basic/packaging">
          <el-icon><Tickets /></el-icon>
          <span>包装管理</span>
        </el-menu-item>
        <el-menu-item index="/basic/supplier">
          <el-icon><Tickets /></el-icon>
          <span>供应商管理</span>
        </el-menu-item>
        <el-menu-item index="/basic/warehouse-area">
          <el-icon><Tickets /></el-icon>
          <span>库区管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="inbound">
        <template #title>
          <el-icon><Download /></el-icon>
          <span>入库管理</span>
        </template>
        <el-menu-item index="/inbound/order">
          <el-icon><Document /></el-icon>
          <span>入库单管理</span>
        </el-menu-item>
        <el-menu-item index="/inbound/manual">
          <el-icon><EditPen /></el-icon>
          <span>手工入库</span>
        </el-menu-item>
        <el-menu-item index="/inbound/scan">
          <el-icon><Aim /></el-icon>
          <span>扫码入库</span>
        </el-menu-item>
        <el-menu-item index="/inbound/history">
          <el-icon><Clock /></el-icon>
          <span>入库历史</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="outbound">
        <template #title>
          <el-icon><Upload /></el-icon>
          <span>出库管理</span>
        </template>
        <el-menu-item index="/outbound/order">
          <el-icon><Document /></el-icon>
          <span>出库单管理</span>
        </el-menu-item>
        <el-menu-item index="/outbound/scan">
          <el-icon><Aim /></el-icon>
          <span>扫码出库</span>
        </el-menu-item>
        <el-menu-item index="/outbound/return-scan">
          <el-icon><RefreshLeft /></el-icon>
          <span>扫码退库</span>
        </el-menu-item>
        <el-menu-item index="/outbound/history">
          <el-icon><Clock /></el-icon>
          <span>出库历史</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="inventory">
        <template #title>
          <el-icon><DataBoard /></el-icon>
          <span>库存管理</span>
        </template>
        <el-menu-item index="/inventory/report">
          <el-icon><DataAnalysis /></el-icon>
          <span>库存报表</span>
        </el-menu-item>
        <el-menu-item v-if="hasRole('admin', 'manager')" index="/inventory/import">
          <el-icon><UploadFilled /></el-icon>
          <span>需求导入</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/demand/list">
        <el-icon><List /></el-icon>
        <span>物料需求</span>
      </el-menu-item>

      <el-menu-item index="/alert/threshold">
        <el-icon><WarningFilled /></el-icon>
        <span>高低储预警</span>
      </el-menu-item>

      <el-menu-item index="/seal">
        <el-icon><Lock /></el-icon>
        <span>封存管理</span>
      </el-menu-item>

      <el-menu-item index="/audit">
        <el-icon><Document /></el-icon>
        <span>操作日志</span>
      </el-menu-item>

      <el-menu-item index="/ai/chat">
        <el-icon><ChatDotRound /></el-icon>
        <span>AI 助手</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

defineProps({
  isCollapse: Boolean
})

const route = useRoute()
const userStore = useUserStore()

function hasRole(...roles) {
  return roles.includes(userStore.role)
}
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
}

.sidebar :deep(.el-menu) {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar :deep(.el-menu-item),
.sidebar :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
}

.sidebar :deep(.el-menu-item:hover) {
  background-color: #263445 !important;
}

.sidebar :deep(.el-menu-item.is-active) {
  background-color: #263445 !important;
}

.sidebar :deep(.el-sub-menu .el-menu) {
  background-color: #1f2d3d !important;
}

.sidebar :deep(.el-sub-menu .el-menu .el-menu-item) {
  background-color: #1f2d3d !important;
  padding-left: 56px !important;
}

.sidebar :deep(.el-sub-menu .el-menu .el-menu-item:hover) {
  background-color: #263445 !important;
}

.sidebar :deep(.el-sub-menu .el-menu .el-menu-item.is-active) {
  background-color: #263445 !important;
}
</style>
