<template>
  <view class="scan-page">
    <van-nav-bar title="扫码入库" left-arrow @click-left="goBack" fixed safe-area-inset-top />

    <view class="scan-body">
      <ScanInput ref="scanInputRef" placeholder="请输入或扫码看板号" @scan="onScan" />

      <MaterialCard
        v-if="material"
        :material-code="material.materialCode"
        :material-name="material.materialName"
        :supplier="material.supplierName"
        :qty="material.labelQty"
        :warehouse-area="material.warehouseArea"
        :extra-rows="statusExtra"
      />

      <view v-if="material" class="actions">
        <van-button
          type="primary"
          block
          round
          :loading="confirming"
          :disabled="disabled"
          @click="onConfirm"
        >
          {{ btnText }}
        </van-button>
        <van-button plain block round style="margin-top: 12px" @click="onReset">
          取消
        </van-button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { NavBar, Button } from 'vant'
import ScanInput from '@/components/ScanInput.vue'
import MaterialCard from '@/components/MaterialCard.vue'
import { getScanLabel, receiveByScan } from '@/api/inbound'

const scanInputRef = ref(null)
const material = ref(null)
const confirming = ref(false)

const disabled = computed(() => material.value?.labelStatus === '已入库')
const btnText = computed(() => disabled.value ? '该看板已入库' : '确认入库')

const statusExtra = computed(() => {
  if (!material.value) return []
  const statusText = material.value.labelStatus === '已入库'
    ? '已入库（不可重复入库）'
    : '未入库'
  return [{ label: '看板状态', value: statusText }]
})

async function onScan(kanbanNo) {
  try {
    const res = await getScanLabel(kanbanNo)
    material.value = res.data
  } catch {
    material.value = null
  }
}

async function onConfirm() {
  if (disabled.value) return
  confirming.value = true
  try {
    await receiveByScan(material.value.kanbanNo)
    uni.showToast({ title: '入库成功', icon: 'success' })
    onReset()
  } catch {
    // 错误已由拦截器处理
  } finally {
    confirming.value = false
  }
}

function onReset() {
  material.value = null
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
</style>
