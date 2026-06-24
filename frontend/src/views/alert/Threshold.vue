<template>
  <PageContainer title="高低储预警">
    <el-alert
      title="为每种物料设定低储数量和高储数量。当库存低于低储量时显示红色预警，高于高储量时显示黄色预警。可编辑的阈值仅管理员可见。"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
      <div>
        <el-button type="primary" @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>刷新数据
        </el-button>
        <el-button v-if="isAdmin" type="success" @click="handleSave" :loading="saving" style="margin-left: 8px">
          <el-icon><Check /></el-icon>保存全部
        </el-button>
      </div>
      <el-tag v-if="alertCount > 0" type="danger" effect="dark" size="medium">
        共 {{ alertCount }} 项异常预警
      </el-tag>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="tableData"
      stripe
      border
      style="width: 100%"
      max-height="calc(100vh - 260px)"
    >
      <el-table-column type="index" label="序号" width="60" fixed />
      <el-table-column prop="materialCode" label="物料号" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="supplier" label="供应商" min-width="150" />
      <el-table-column prop="currentStock" label="当前库存" width="100">
        <template #default="{ row }">
          <span :class="{
            'alert-low': row.alertStatus === 'low',
            'alert-high': row.alertStatus === 'high'
          }">{{ row.currentStock }}</span>
        </template>
      </el-table-column>

      <!-- 低储阈值（仅 admin 可编辑） -->
      <el-table-column label="低储数量(低于此值预警)" width="155">
        <template #default="{ row }">
          <el-input-number
            v-if="isAdmin"
            v-model="row.lowStockQty"
            :min="0"
            size="small"
            controls-position="right"
            style="width: 130px"
          />
          <span v-else>{{ row.lowStockQty }}</span>
        </template>
      </el-table-column>

      <!-- 高储阈值（仅 admin 可编辑） -->
      <el-table-column label="高储数量(高于此值预警)" width="160">
        <template #default="{ row }">
          <el-input-number
            v-if="isAdmin"
            v-model="row.highStockQty"
            :min="0"
            size="small"
            controls-position="right"
            style="width: 130px"
          />
          <span v-else>{{ row.highStockQty }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="alertStatus" label="预警状态" width="110" fixed="right">
        <template #default="{ row }">
          <el-tag v-if="row.alertStatus === 'low'" type="danger" size="large" effect="dark">
            低储预警
          </el-tag>
          <el-tag v-else-if="row.alertStatus === 'high'" type="warning" size="large" effect="dark">
            高储预警
          </el-tag>
          <el-tag v-else-if="row.alertStatus === 'normal'" type="success" size="large">
            正常
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import PageContainer from '@/components/PageContainer.vue'
import { getAlertThresholdsApi, saveAlertThresholdsApi } from '@/api/alert'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'admin')
const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const alertCount = ref(0)

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getAlertThresholdsApi()
    tableData.value = data || []
    alertCount.value = (data || []).filter(r => r.alertStatus === 'low' || r.alertStatus === 'high').length
  } catch {
    tableData.value = []
    alertCount.value = 0
  } finally {
    loading.value = false
  }
}

function handleRefresh() { fetchData() }

async function handleSave() {
  saving.value = true
  try {
    await saveAlertThresholdsApi(tableData.value.map(item => ({
      materialCode: item.materialCode,
      materialName: item.materialName,
      supplier: item.supplier,
      lowStockQty: item.lowStockQty ?? 0,
      highStockQty: item.highStockQty ?? 0
    })))
    ElMessage.success('预警阈值保存成功')
    fetchData()
  } catch { /* 拦截器已处理 */ }
  finally { saving.value = false }
}
</script>

<style scoped>
.alert-low { color: #f56c6c; font-weight: 700; }
.alert-high { color: #e6a23c; font-weight: 700; }
</style>
