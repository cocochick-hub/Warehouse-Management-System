<template>
  <PageContainer title="扫码入库">
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
          title="扫码枪输入后按回车查询；也可以手动粘贴二维码内容。确认入库后系统会按当前包数量更新库存。"
          type="info"
          :closable="false"
          show-icon
        />
      </section>

      <section v-if="currentLabel" class="result-panel">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="看板号">{{ currentLabel.kanbanNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentLabel.labelStatus === '未入库' ? 'warning' : 'success'">
              {{ currentLabel.labelStatus }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="入库单号">{{ currentLabel.docNo }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ currentLabel.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="物料号">{{ currentLabel.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="物料名称">{{ currentLabel.materialName }}</el-descriptions-item>
          <el-descriptions-item label="本包数量">{{ currentLabel.labelQty }}</el-descriptions-item>
          <el-descriptions-item label="包序号">
            第 {{ currentLabel.packageSeq }}/{{ currentLabel.packageTotal }} 包
          </el-descriptions-item>
          <el-descriptions-item label="器具型号">{{ currentLabel.packageModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="库区">{{ currentLabel.warehouseArea || '-' }}</el-descriptions-item>
          <el-descriptions-item label="转包状态">{{ currentLabel.transferStatus || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="confirm-row">
          <el-button
            type="success"
            size="large"
            :disabled="currentLabel.labelStatus !== '未入库'"
            :loading="submitting"
            @click="handleReceive"
          >
            确认入库
          </el-button>
        </div>
      </section>
    </div>
  </PageContainer>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { getInboundScanLabelApi, receiveInboundScanApi } from '@/api/inbound'

const scanInputRef = ref()
const scanValue = ref('')
const querying = ref(false)
const submitting = ref(false)
const currentLabel = ref(null)

onMounted(() => {
  focusInput()
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
    const { data } = await getInboundScanLabelApi(queryValue)
    currentLabel.value = data
  } finally {
    querying.value = false
    focusInput()
  }
}

async function handleReceive() {
  if (!currentLabel.value?.kanbanNo) {
    return
  }

  submitting.value = true
  try {
    await receiveInboundScanApi({ kanbanNo: currentLabel.value.kanbanNo })
    ElMessage.success('扫码入库成功')
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
