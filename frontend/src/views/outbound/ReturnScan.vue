<template>
  <PageContainer title="扫码退库">
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
          title="扫码枪输入后按回车查询，或手动粘贴二维码内容。确认退库后库存会回增，出库历史标记为已退库。"
          type="info"
          :closable="false"
          show-icon
        />
      </section>

      <section v-if="currentLabel" class="result-panel">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="看板号">{{ currentLabel.kanbanNo }}</el-descriptions-item>
          <el-descriptions-item label="可否退库">
            <el-tag :type="currentLabel.canReturn ? 'success' : 'danger'">
              {{ currentLabel.canReturn ? '可退库' : '不可退库' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="物料号">{{ currentLabel.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="物料名称">{{ currentLabel.materialName }}</el-descriptions-item>
          <el-descriptions-item label="需求方">{{ currentLabel.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="退库数量">{{ currentLabel.issueQty }}</el-descriptions-item>
          <el-descriptions-item label="原出库单号">{{ currentLabel.outboundDocNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="出库时间">{{ currentLabel.issuedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="库区">{{ currentLabel.warehouseArea || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="!currentLabel.canReturn" label="原因">
            <span style="color: #f56c6c">{{ currentLabel.reason }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="confirm-row">
          <el-button
            type="warning"
            size="large"
            :disabled="!currentLabel.canReturn"
            :loading="submitting"
            @click="handleReturn"
          >
            确认退库
          </el-button>
        </div>
      </section>
    </div>
  </PageContainer>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { getReturnLabel, doReturn } from '@/api/outbound'

const scanInputRef = ref()
const scanValue = ref('')
const querying = ref(false)
const submitting = ref(false)
const currentLabel = ref(null)

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
    const { data } = await getReturnLabel(queryValue)
    currentLabel.value = data
  } finally {
    querying.value = false
    focusInput()
  }
}

async function handleReturn() {
  if (!currentLabel.value?.kanbanNo) return
  if (!currentLabel.value.canReturn) return

  submitting.value = true
  try {
    await doReturn({ kanbanNo: currentLabel.value.kanbanNo })
    ElMessage.success('退库成功')
    scanValue.value = ''
    currentLabel.value = null
  } finally {
    submitting.value = false
    focusInput()
  }
}

function handleClear() {
  scanValue.value = ''
  currentLabel.value = null
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

.confirm-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
