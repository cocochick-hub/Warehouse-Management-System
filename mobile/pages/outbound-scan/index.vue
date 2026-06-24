<template>
  <view class="scan-page">
    <van-nav-bar
      :title="outboundOrderId ? '带单出库' : '扫码出库'"
      left-arrow
      @click-left="goBack"
      fixed
      safe-area-inset-top
    />

    <view class="scan-body">
      <ScanInput ref="scanInputRef" placeholder="请输入或扫码看板号" @scan="onScan" />

      <MaterialCard
        v-if="label"
        :material-code="label.materialCode"
        :material-name="label.materialName"
        :supplier="label.supplierName"
        :qty="label.availableQty"
        :warehouse-area="label.warehouseArea"
        :extra-rows="outboundExtras"
      />

      <view v-if="label && label.labelStatus === '已出库'" class="tips">
        <van-tag type="warning" size="large">该看板已出库</van-tag>
      </view>

      <view v-if="label && label.labelStatus !== '已出库'" class="actions">
        <van-button type="danger" block round :loading="confirming" @click="onTryConfirm">
          确认出库
        </van-button>
        <van-button plain block round style="margin-top: 12px" @click="onReset">取消</van-button>
      </view>
    </view>

    <FifoAlert
      :visible="showFifo"
      :message="label?.fifoMessage"
      :earliest-doc-no="label?.earliestDocNo"
      @confirm="onConfirm"
      @cancel="showFifo = false"
    />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { NavBar, Button, Tag } from 'vant'
import ScanInput from '@/components/ScanInput.vue'
import MaterialCard from '@/components/MaterialCard.vue'
import FifoAlert from '@/components/FifoAlert.vue'
import { getOutboundScanLabel, issueByScan, orderlessIssue } from '@/api/outbound'

const scanInputRef = ref(null)
const label = ref(null)
const confirming = ref(false)
const showFifo = ref(false)
const outboundOrderId = ref(null)

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  if (currentPage.$page?.options?.outboundOrderId) {
    outboundOrderId.value = currentPage.$page.options.outboundOrderId
  }
})

const outboundExtras = computed(() => {
  if (!label.value) return []
  return [
    { label: '看板状态', value: label.value.labelStatus }
  ]
})

async function onScan(kanbanNo) {
  try {
    const res = await getOutboundScanLabel(kanbanNo)
    label.value = res.data
  } catch {
    label.value = null
  }
}

function onTryConfirm() {
  if (label.value?.fifoWarning) {
    showFifo.value = true
  } else {
    onConfirm()
  }
}

async function onConfirm() {
  showFifo.value = false
  confirming.value = true
  try {
    if (outboundOrderId.value) {
      // 带单出库：走 issue 接口
      await issueByScan({
        kanbanNo: label.value.kanbanNo,
        issueQty: label.value.availableQty,
        outboundOrderId: outboundOrderId.value
      })
    } else {
      // 不带单出库：走 orderless-issue 接口
      await orderlessIssue({
        materialCode: label.value.materialCode,
        materialName: label.value.materialName,
        supplierCode: label.value.supplierCode,
        supplierName: label.value.supplierName,
        issueQty: label.value.availableQty,
        warehouseArea: label.value.warehouseArea
      })
    }
    uni.showToast({ title: '出库成功', icon: 'success' })
    onReset()
  } catch {
    // 错误已由请求拦截器处理
  } finally {
    confirming.value = false
  }
}

function onReset() {
  label.value = null
  scanInputRef.value?.clear()
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped>
.scan-page { min-height: 100vh; }
.scan-body { padding: 12px; margin-top: 46px; }
.actions { margin-top: 24px; }
.tips { text-align: center; margin-top: 24px; }
</style>
