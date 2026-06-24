<template>
  <PageContainer title="封存管理">
    <el-alert title="封存后该看板将不可用于出库，解封后恢复正常出库。" type="info" :closable="false" show-icon style="margin-bottom: 16px" />

    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: 扫码封存/解封 -->
      <el-tab-pane label="扫码操作" name="scan">
        <el-form :model="scanForm" inline @submit.prevent="handleScanSearch">
          <el-form-item label="看板号">
            <el-input
              v-model="scanForm.kanbanNo"
              placeholder="扫码或输入看板号"
              clearable
              style="width: 320px"
              @keyup.enter="handleScanSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleScanSearch">
              <el-icon><Search /></el-icon>查询
            </el-button>
          </el-form-item>
        </el-form>

        <template v-if="scanResult">
          <el-descriptions :column="2" border class="scan-result">
            <el-descriptions-item label="看板号">{{ scanResult.kanbanNo }}</el-descriptions-item>
            <el-descriptions-item label="入库单号">{{ scanResult.docNo }}</el-descriptions-item>
            <el-descriptions-item label="物料号">{{ scanResult.materialCode }}</el-descriptions-item>
            <el-descriptions-item label="物料名称">{{ scanResult.materialName }}</el-descriptions-item>
            <el-descriptions-item label="供应商">{{ scanResult.supplierName }}</el-descriptions-item>
            <el-descriptions-item label="看板数量">{{ scanResult.labelQty }}</el-descriptions-item>
            <el-descriptions-item label="库区">{{ scanResult.warehouseArea }}</el-descriptions-item>
            <el-descriptions-item label="可用数量">{{ scanResult.availableQty }}</el-descriptions-item>
            <el-descriptions-item label="入库状态">{{ scanResult.labelStatus }}</el-descriptions-item>
            <el-descriptions-item label="封存状态">
              <el-tag v-if="scanResult.sealed" type="danger" size="small">已封存</el-tag>
              <el-tag v-else type="success" size="small">正常</el-tag>
            </el-descriptions-item>
            <el-descriptions-item v-if="scanResult.sealed" label="封存时间">{{ scanResult.sealedAt?.replace('T', ' ') }}</el-descriptions-item>
            <el-descriptions-item v-if="scanResult.sealed" label="封存人">{{ scanResult.sealedBy }}</el-descriptions-item>
          </el-descriptions>

          <div class="scan-actions">
            <el-button
              v-if="!scanResult.sealed"
              type="danger"
              :loading="sealing"
              @click="handleToggleSeal('seal')"
            >
              <el-icon><Lock /></el-icon>封存
            </el-button>
            <el-button
              v-if="scanResult.sealed"
              type="success"
              :loading="sealing"
              @click="handleToggleSeal('unseal')"
            >
              <el-icon><Unlock /></el-icon>解封
            </el-button>
          </div>
        </template>

        <el-empty v-else-if="scanned" description="未找到该看板号，请确认输入正确" />
      </el-tab-pane>

      <!-- Tab 2: 批量封存/解封 -->
      <el-tab-pane label="批量操作" name="batch">
        <el-form :model="batchForm">
          <el-form-item label="看板号列表">
            <el-input
              v-model="batchForm.kanbanNosText"
              type="textarea"
              :rows="6"
              placeholder="每行输入一个看板号，例如：&#10;KB20240601001-001&#10;KB20240601001-002&#10;KB20240601001-003"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" :loading="batchSealing" @click="handleBatchSeal">
              <el-icon><Lock /></el-icon>批量封存
            </el-button>
            <el-button type="success" :loading="batchSealing" @click="handleBatchUnseal" style="margin-left: 12px">
              <el-icon><Unlock /></el-icon>批量解封
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <!-- 批量操作结果 -->
        <template v-if="batchResult">
          <el-alert
            :title="`操作完成：成功 ${batchResult.successCount} 个，失败 ${batchResult.failCount} 个`"
            :type="batchResult.failCount > 0 ? 'warning' : 'success'"
            show-icon
            style="margin-bottom: 12px"
          />
          <el-table v-if="batchResult.failReasons?.length" :data="batchResult.failReasons" stripe border>
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column label="失败原因" min-width="400">
              <template #default="{ row }">{{ row }}</template>
            </el-table-column>
          </el-table>
        </template>
      </el-tab-pane>

      <!-- Tab 3: 已封存列表 -->
      <el-tab-pane label="已封存列表" name="list">
        <el-form :model="listQuery" inline>
          <el-form-item label="物料号">
            <el-input v-model="listQuery.materialCode" placeholder="物料号" clearable />
          </el-form-item>
          <el-form-item label="供应商">
            <el-input v-model="listQuery.supplierName" placeholder="供应商名称" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleListSearch">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button @click="handleListReset">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="listLoading" :data="sealedList" stripe border style="width: 100%">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="kanbanNo" label="看板号" min-width="200" />
          <el-table-column prop="materialCode" label="物料号" width="140" />
          <el-table-column prop="materialName" label="物料名称" min-width="160" />
          <el-table-column prop="supplierName" label="供应商" min-width="140" />
          <el-table-column prop="labelQty" label="看板数量" width="100" />
          <el-table-column prop="availableQty" label="可用数量" width="100" />
          <el-table-column prop="warehouseArea" label="库区" width="100" />
          <el-table-column prop="sealedAt" label="封存时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.sealedAt) }}</template>
          </el-table-column>
          <el-table-column prop="sealedBy" label="封存人" width="100" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="success" link size="small" @click="handleUnsealFromList(row)">
                <el-icon><Unlock /></el-icon>解封
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!listLoading && sealedList.length === 0" description="暂无已封存的看板" />
      </el-tab-pane>
    </el-tabs>
  </PageContainer>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { getSealLabelApi, getSealedLabelsApi, toggleSealApi, toggleSealBatchApi } from '@/api/seal'

const activeTab = ref('scan')

// ==================== 扫码操作 ====================
const scanForm = reactive({ kanbanNo: '' })
const scanResult = ref(null)
const scanned = ref(false)
const sealing = ref(false)

async function handleScanSearch() {
  const kanbanNo = scanForm.kanbanNo?.trim()
  if (!kanbanNo) {
    ElMessage.warning('请输入看板号')
    return
  }
  try {
    const { data } = await getSealLabelApi(kanbanNo)
    scanResult.value = data
    scanned.value = true
  } catch {
    scanResult.value = null
    scanned.value = true
  }
}

async function handleToggleSeal(action) {
  if (!scanResult.value) return
  const actionText = action === 'seal' ? '封存' : '解封'
  await ElMessageBox.confirm(
    `确定${actionText}看板「${scanResult.value.kanbanNo}」吗？`,
    `确认${actionText}`,
    { confirmButtonText: actionText, cancelButtonText: '取消', type: 'warning' }
  )

  sealing.value = true
  try {
    const { data } = await toggleSealApi({
      kanbanNo: scanResult.value.kanbanNo,
      action
    })
    scanResult.value = data
    ElMessage.success(`${actionText}成功`)
  } catch {
    // handled by interceptor
  } finally {
    sealing.value = false
  }
}

// ==================== 批量操作 ====================
const batchForm = reactive({ kanbanNosText: '' })
const batchSealing = ref(false)
const batchResult = ref(null)

async function handleBatchSeal() {
  await handleBatchToggle('seal', '封存')
}

async function handleBatchUnseal() {
  await handleBatchToggle('unseal', '解封')
}

async function handleBatchToggle(action, actionText) {
  const lines = batchForm.kanbanNosText
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean)
  if (!lines.length) {
    ElMessage.warning('请至少输入一个看板号')
    return
  }

  await ElMessageBox.confirm(
    `确定${actionText}以下 ${lines.length} 个看板吗？`,
    `确认批量${actionText}`,
    { confirmButtonText: actionText, cancelButtonText: '取消', type: 'warning' }
  )

  batchSealing.value = true
  batchResult.value = null
  try {
    const { data } = await toggleSealBatchApi({
      kanbanNos: lines,
      action
    })
    batchResult.value = data
    ElMessage.success(`批量操作完成：成功 ${data.successCount} 个，失败 ${data.failCount} 个`)
  } catch {
    // handled by interceptor
  } finally {
    batchSealing.value = false
  }
}

// ==================== 已封存列表 ====================
const listQuery = reactive({ materialCode: '', supplierName: '' })
const listLoading = ref(false)
const sealedList = ref([])

async function fetchSealedList() {
  listLoading.value = true
  try {
    const { data } = await getSealedLabelsApi({
      materialCode: listQuery.materialCode || undefined,
      supplierName: listQuery.supplierName || undefined
    })
    sealedList.value = data || []
  } catch {
    sealedList.value = []
  } finally {
    listLoading.value = false
  }
}

function handleListSearch() {
  fetchSealedList()
}

function handleListReset() {
  listQuery.materialCode = ''
  listQuery.supplierName = ''
  fetchSealedList()
}

async function handleUnsealFromList(row) {
  await ElMessageBox.confirm(
    `确定解封看板「${row.kanbanNo}」吗？`,
    '确认解封',
    { confirmButtonText: '解封', cancelButtonText: '取消', type: 'warning' }
  )
  try {
    await toggleSealApi({ kanbanNo: row.kanbanNo, action: 'unseal' })
    ElMessage.success('解封成功')
    await fetchSealedList()
  } catch {
    // handled by interceptor
  }
}

// 切换至已封存列表时自动加载数据
watch(activeTab, (tab) => {
  if (tab === 'list') {
    fetchSealedList()
  }
})

// ==================== 工具函数 ====================
function formatDateTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}
</script>

<style scoped>
.scan-result {
  margin-top: 16px;
}
.scan-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
