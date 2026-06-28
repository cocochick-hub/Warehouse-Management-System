<template>
  <view class="check-page">
    <van-nav-bar title="盘点" left-arrow @click-left="goBack" fixed safe-area-inset-top />

    <view class="check-body">
      <view v-if="loading" class="loading-wrap">
        <van-loading size="24px">加载中...</van-loading>
      </view>

      <view v-else-if="tasks.length === 0" class="empty-wrap">
        <van-empty description="暂无进行中的盘点任务" />
      </view>

      <view v-else>
        <view
          v-for="task in tasks"
          :key="task.taskId"
          class="task-card"
          @click="enterCheck(task)"
        >
          <view class="task-header">
            <text class="task-name">{{ task.taskName }}</text>
            <van-tag :type="task.checkType === '明盘' ? 'success' : 'warning'" size="small">
              {{ task.checkType }}
            </van-tag>
          </view>
          <view class="task-meta">
            <text class="task-no">{{ task.taskNo }}</text>
            <text class="task-progress">已盘 {{ task.checked }}/{{ task.total }}</text>
          </view>
          <view class="progress-bar">
            <view class="progress-fill" :style="{ width: task.progressPercent + '%' }" />
          </view>
          <view v-if="task.diffCount > 0" class="diff-tip">
            <van-icon name="warning-o" size="14" color="#ff976a" />
            <text>有 {{ task.diffCount }} 条差异待处理</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NavBar, Tag, Loading, Empty, Icon } from 'vant'
import { getActiveTasks } from '@/api/check'

const loading = ref(true)
const tasks = ref([])

onMounted(() => loadTasks())

async function loadTasks() {
  loading.value = true
  try {
    const res = await getActiveTasks()
    tasks.value = res.data || []
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
  }
}

function enterCheck(task) {
  uni.navigateTo({
    url: `/pages/check/detail?taskId=${task.taskId}&taskName=${encodeURIComponent(task.taskName)}&checkType=${encodeURIComponent(task.checkType)}`
  })
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped>
.check-page { min-height: 100vh; background: #f7f8fa; }
.check-body { padding: 12px; margin-top: 46px; }
.loading-wrap { display: flex; justify-content: center; padding: 40px 0; }
.empty-wrap { padding: 40px 0; }
.task-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.task-name { font-size: 15px; font-weight: 600; color: #323233; }
.task-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.task-no { font-size: 12px; color: #969799; }
.task-progress { font-size: 12px; color: #646566; }
.progress-bar {
  height: 6px;
  background: #ebedf0;
  border-radius: 3px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #1989fa;
  border-radius: 3px;
  transition: width 0.3s;
}
.diff-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: #ff976a;
}
</style>