<template>
  <el-row :gutter="16" style="margin-top: 16px;">
    <!-- 缺货预警卡片 -->
    <el-col :span="12">
      <el-card shadow="hover" class="ai-alert-card">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon color="#f56c6c" :size="20"><WarningFilled /></el-icon>
              <span>AI 缺货预警</span>
              <el-tag v-if="alerts.shortageCount > 0" type="danger" size="small" effect="dark">
                {{ alerts.shortageCount }}
              </el-tag>
            </div>
            <div class="header-actions">
              <el-button text type="primary" @click="$router.push('/ai/chat')">
                <el-icon><ChatDotRound /></el-icon>AI 分析
              </el-button>
              <el-button text type="primary" @click="refreshAlerts" :loading="refreshing">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="loading" class="loading-wrap">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <span>AI 分析中...</span>
        </div>

        <div v-else-if="alerts.shortages.length === 0" class="empty-wrap">
          <el-empty description="暂无缺货风险" :image-size="60" />
        </div>

        <div v-else class="alert-list">
          <div
            v-for="item in alerts.shortages.slice(0, 5)"
            :key="item.id"
            class="alert-item"
            @click="goChat(item.materialCode)"
          >
            <div class="alert-left">
              <el-tag :type="riskTagType(item.riskLevel)" size="small" effect="dark">
                {{ riskLabel(item.riskLevel) }}
              </el-tag>
              <span class="material-name">{{ item.materialName }}</span>
            </div>
            <div class="alert-right">
              <span class="alert-meta">
                库存 {{ item.currentStock }} · 支撑约 {{ item.estimatedDays }} 天
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </el-col>

    <!-- 呆滞预警卡片 -->
    <el-col :span="12">
      <el-card shadow="hover" class="ai-alert-card">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <el-icon color="#e6a23c" :size="20"><WarningFilled /></el-icon>
              <span>AI 呆滞预警</span>
              <el-tag v-if="alerts.deadStockCount > 0" type="warning" size="small" effect="dark">
                {{ alerts.deadStockCount }}
              </el-tag>
            </div>
            <div class="header-actions">
              <el-button text type="primary" @click="$router.push('/ai/chat')">
                <el-icon><ChatDotRound /></el-icon>AI 分析
              </el-button>
              <el-button text type="primary" @click="refreshAlerts" :loading="refreshing">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="loading" class="loading-wrap">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <span>AI 分析中...</span>
        </div>

        <div v-else-if="alerts.deadStocks.length === 0" class="empty-wrap">
          <el-empty description="暂无呆滞风险" :image-size="60" />
        </div>

        <div v-else class="alert-list">
          <div
            v-for="item in alerts.deadStocks.slice(0, 5)"
            :key="item.id"
            class="alert-item"
            @click="goChat(item.materialCode)"
          >
            <div class="alert-left">
              <el-tag :type="riskTagType(item.riskLevel)" size="small" effect="dark">
                {{ riskLabel(item.riskLevel) }}
              </el-tag>
              <span class="material-name">{{ item.materialName }}</span>
            </div>
            <div class="alert-right">
              <span class="alert-meta">
                库存 {{ item.currentStock }} · 呆滞 {{ item.idleDays }} 天
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getLatestAlertsApi, refreshAlertsApi } from '@/api/ai'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(true)
const refreshing = ref(false)
const alerts = reactive({
  shortages: [],
  deadStocks: [],
  shortageCount: 0,
  deadStockCount: 0
})

function riskTagType(level) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

function riskLabel(level) {
  if (level === 'HIGH') return '高风险'
  if (level === 'MEDIUM') return '中风险'
  return '低风险'
}

function goChat(materialCode) {
  router.push({ path: '/ai/chat', query: { material: materialCode } })
}

function loadAlerts() {
  loading.value = true
  getLatestAlertsApi().then(res => {
    const data = res.data
    alerts.shortages = data.shortages || []
    alerts.deadStocks = data.deadStocks || []
    alerts.shortageCount = data.shortageCount || 0
    alerts.deadStockCount = data.deadStockCount || 0
  }).catch(() => {
    // silent fail
  }).finally(() => {
    loading.value = false
  })
}

async function refreshAlerts() {
  refreshing.value = true
  try {
    await refreshAlertsApi()
    ElMessage.success('预警分析已刷新')
    await loadAlerts()
  } catch {
    ElMessage.error('刷新失败，请稍后重试')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  loadAlerts()
})
</script>

<style scoped>
.ai-alert-card {
  border-radius: 8px;
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: #909399;
  font-size: 14px;
}

.empty-wrap {
  padding: 8px 0;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  background: #fafafa;
}

.alert-item:hover {
  background: #f0f2f5;
}

.alert-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.material-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.alert-meta {
  font-size: 12px;
  color: #909399;
}
</style>
