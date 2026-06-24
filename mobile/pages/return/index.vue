<template>
  <view class="scan-page">
    <van-nav-bar title="退库" left-arrow @click-left="goBack" fixed safe-area-inset-top />

    <view class="scan-body">
      <ScanInput ref="scanInputRef" placeholder="请输入或扫码看板号" @scan="onScan" />

      <MaterialCard
        v-if="data"
        :material-code="data.materialCode"
        :material-name="data.materialName"
        :supplier="data.supplierName"
        :qty="data.issueQty"
        :warehouse-area="data.warehouseArea"
        :extra-rows="returnExtras"
      />

      <view v-if="data && data.canReturn" class="actions">
        <van-button type="primary" block round :loading="confirming" @click="onConfirm">
          确认退库
        </van-button>
        <van-button plain block round style="margin-top: 12px" @click="onReset">取消</van-button>
      </view>

      <view v-if="data && !data.canReturn" class="tips">
        <van-tag type="danger" size="large">{{ data.reason || '无法退库' }}</van-tag>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import ScanInput from '@/components/ScanInput.vue'
import MaterialCard from '@/components/MaterialCard.vue'
import { getReturnLabel, doReturn } from '@/api/outbound'

const scanInputRef = ref(null)
const data = ref(null)
const confirming = ref(false)

const returnExtras = computed(() => {
  if (!data.value) return []
  return [
    { label: '原出库单号', value: data.value.outboundDocNo || '-' },
    { label: '出库时间', value: data.value.issuedAt || '-' }
  ]
})

async function onScan(kanbanNo) {
  try {
    const res = await getReturnLabel(kanbanNo)
    data.value = res.data
  } catch {
    data.value = null
  }
}

async function onConfirm() {
  confirming.value = true
  try {
    await doReturn(data.value.kanbanNo)
    uni.showToast({ title: '退库成功', icon: 'success' })
    onReset()
  } catch {
    // 错误已由拦截器处理
  } finally {
    confirming.value = false
  }
}

function onReset() {
  data.value = null
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
