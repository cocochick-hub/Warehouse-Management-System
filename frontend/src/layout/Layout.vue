<template>
  <div class="layout-container">
    <el-container style="height: 100vh">
      <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
        <Sidebar :is-collapse="isCollapse" />
      </el-aside>
      <el-container>
        <el-header class="layout-header">
          <Navbar @toggle-collapse="isCollapse = !isCollapse" :is-collapse="isCollapse" />
        </el-header>
        <el-main class="layout-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <!-- AI 悬浮按钮（全局） -->
    <AiFloatingButton />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
import AiFloatingButton from '@/components/ai/AiFloatingButton.vue'

const isCollapse = ref(false)
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.layout-aside {
  background: #304156;
  transition: width 0.3s ease;
  overflow: hidden;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 16px;
  height: 56px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.layout-main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
  height: calc(100vh - 56px);
}
</style>
