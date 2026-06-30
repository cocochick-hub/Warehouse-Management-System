<template>
  <PageContainer title="扫码出库">
    <div class="scan-layout">
      <section class="scan-panel">
        <el-form label-width="90px">
          <el-form-item label="看板码">
            <el-input
              ref="scanInputRef"
              v-model="scanValue"
              placeholder="请扫码或输入 WMS-INBOUND|看板号"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="querying" @click="handleQuery">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button @click="handleClear">清空</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          title="扫码枪输入后按回车查询；也可以手动粘贴二维码内容。确认出库后系统会按包数量扣减库存。"
          type="info"
          :closable="false"
          show-icon
        />
      </section>

      <section v-if="currentLabel" class="result-panel">
        <el-alert
          v-if="currentLabel.sealed"
          :title="currentLabel.sealedMessage || '该看板已封存，无法出库'"
          type="error"
          :closable="false"
          show-icon
        />
        <el-descriptions :column="2" border>
          <el-descriptions-item label="看板号">{{ currentLabel.kanbanNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="labelStatusType">{{ currentLabel.labelStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="入库单号">{{ currentLabel.docNo }}</el-descriptions-item>
          <el-descriptions-item label="需求方">{{ currentLabel.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="物料号">{{ currentLabel.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="物料名称">{{ currentLabel.materialName }}</el-descriptions-item>
          <el-descriptions-item label="本包数量">{{ currentLabel.labelQty }}</el-descriptions-item>
          <el-descriptions-item label="可用数量">{{ currentLabel.availableQty }}</el-descriptions-item>
          <el-descriptions-item label="库区">{{ currentLabel.warehouseArea || '-' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="!currentLabel.sealed">
          <el-form label-width="90px" class="issue-form">
            <el-form-item label="出库单号">
              <el-select v-model="issueForm.docNo" filterable clearable placeholder="请选择出库单" style="width: 100%">
                <el-option
                  v-for="order in pendingOrders"
                  :key="order.id"
                  :label="`${order.docNo} - ${order.supplier}`"
                  :value="order.docNo"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="出库数量">
              <el-input-number
                v-model="issueForm.issueQty"
                :min="1"
                :max="Math.max(1, currentLabel.availableQty || 0)"
                controls-position="right"
              />
            </el-form-item>
          </el-form>

          <div class="confirm-row">
            <el-button
              type="success"
              size="large"
              :disabled="!canIssue"
              :loading="submitting"
              @click="handleIssue"
            >
              确认出库
            </el-button>
          </div>
        </template>
      </section>
    </div>

    <el-dialog v-model="fifoVisible" title="先进先出预警" width="480px" :close-on-click-modal="false">
      <div class="fifo-content">
        <el-alert
          title="注意"
          :description="currentLabel?.fifoMessage || ''"
          type="warning"
          :closable="false"
          show-icon
        />
        <p class="fifo-hint">最早出库的库存批次为入库单号：{{ currentLabel?.earliestDocNo }}</p>
      </div>
      <template #footer>
        <el-button @click="handleFifoCancel">取消</el-button>
        <el-button type="primary" @click="handleFifoConfirm">继续出库</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { getOutboundScanLabel, issueByScan, listOrders } from '@/api/outbound'

const scanInputRef = ref()
const scanValue = ref('')
const querying = ref(false)
const submitting = ref(false)
const currentLabel = ref(null)
const fifoVisible = ref(false)

const pendingOrders = ref([])

const issueForm = reactive({
  docNo: '',
  issueQty: 1
})

const labelStatusType = computed(() => {
  return currentLabel.value?.labelStatus === '已入库' ? 'success' : 'warning'
})

const canIssue = computed(() => {
  return (
    currentLabel.value?.labelStatus === '已入库' &&
    !currentLabel.value?.sealed &&
    currentLabel.value?.availableQty > 0 &&
    issueForm.docNo &&
    issueForm.issueQty > 0
  )
})

onMounted(() => {
  focusInput()
  fetchPendingOrders()
})

async function handleQuery() {
  const value = scanValue.value.trim()
  if (!value) {
    ElMessage.warning('请先扫码或输入看板号')
    focusInput()
    return
  }

  querying.value = true
  try {
    const queryValue = parseScanValue(value)
    const { data } = await getOutboundScanLabel(queryValue)
    currentLabel.value = data
    issueForm.docNo = ''
    issueForm.issueQty = data.availableQty ?? (data.labelQty || 1)

    if (data.sealed) {
      ElMessage.error(data.sealedMessage || '该看板已封存，无法出库')
    } else if (data.fifoWarning) {
      fifoVisible.value = true
    }
  } finally {
    querying.value = false
    focusInput()
  }
}

async function handleIssue() {
  if (!currentLabel.value?.kanbanNo) return
  if (!canIssue.value) return

  submitting.value = true
  try {
    await issueByScan({
      kanbanNo: currentLabel.value.kanbanNo,
      issueQty: issueForm.issueQty,
      outboundDocNo: issueForm.docNo,
      warehouseArea: currentLabel.value.warehouseArea || undefined
    })
    ElMessage.success('扫码出库成功')
    scanValue.value = ''
    currentLabel.value = null
    issueForm.docNo = ''
    issueForm.issueQty = 1
  } finally {
    submitting.value = false
    focusInput()
  }
}

async function fetchPendingOrders() {
  try {
    const { data } = await listOrders({
      status: '待出库',
      size: 200
    })
    const pending = data.records || []

    const { data: partialData } = await listOrders({
      status: '部分完成',
      size: 200
    })
    const partial = partialData.records || []

    pendingOrders.value = [...pending, ...partial]
  } catch {
    pendingOrders.value = []
  }
}

function handleClear() {
  scanValue.value = ''
  currentLabel.value = null
  issueForm.docNo = ''
  issueForm.issueQty = 1
  focusInput()
}

function handleFifoConfirm() {
  fifoVisible.value = false
}

function handleFifoCancel() {
  fifoVisible.value = false
  currentLabel.value = null
  scanValue.value = ''
  issueForm.docNo = ''
  issueForm.issueQty = 1
  focusInput()
}

function parseScanValue(value) {
  const prefix = 'WMS-INBOUND|'
  return value.startsWith(prefix) ? value.slice(prefix.length) : value
}

async function focusInput() {
  await nextTick()
  scanInputRef.value?.focus?.()
}
</script>

<style scoped>
.scan-layout {
  display: grid;
  gap: 18px;
}

.scan-panel,
.result-panel {
  max-width: 960px;
}

.issue-form {
  margin-top: 16px;
}

.confirm-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.fifo-content {
  display: grid;
  gap: 16px;
}

.fifo-hint {
  margin: 0;
  font-size: 14px;
  color: #555;
}
</style>
