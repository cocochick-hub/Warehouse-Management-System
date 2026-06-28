<template>
  <view class="seal-page">
    <van-nav-bar
      title="封存/解封"
      left-arrow
      @click-left="goBack"
      fixed
      safe-area-inset-top
    />

    <view class="seal-body">
      <van-tabs v-model:active="activeTab">
        <!-- Tab 1: 扫码操作 -->
        <van-tab title="扫码操作">
          <ScanInput ref="scanInputRef" @scan="onScan" />

          <MaterialCard
            v-if="label"
            :material-code="label.materialCode"
            :material-name="label.materialName"
            :supplier="label.supplierName"
            :qty="label.availableQty"
            :warehouse-area="label.warehouseArea"
            :extra-rows="extras"
          />

          <view v-if="label && !label.sealed" class="action">
            <van-button
              type="danger" block round :loading="toggling"
              @click="onToggle('seal')"
            >封存</van-button>
          </view>
          <view v-if="label && label.sealed" class="action">
            <van-button
              type="success" block round :loading="toggling"
              @click="onToggle('unseal')"
            >解封</van-button>
          </view>
        </van-tab>

        <!-- Tab 2: 已封存列表 -->
        <van-tab title="已封存列表">
          <view class="filter">
            <van-field v-model="filterMaterialCode" placeholder="物料号（可选）" />
            <van-field v-model="filterSupplierName" placeholder="供应商（可选）" />
            <van-button type="primary" size="small" block @click="fetchSealedList">
              查询已封存看板
            </van-button>
          </view>

          <van-cell-group v-if="sealedList.length" inset>
            <van-cell
              v-for="item in sealedList" :key="item.kanbanNo"
              :title="item.kanbanNo"
              :label="`${item.materialCode} ${item.materialName}\n${item.supplierName} | ${item.availableQty}件 | ${item.warehouseArea}`"
            >
              <template #right-icon>
                <van-button
                  type="success" size="small" :loading="unsealingId === item.kanbanNo"
                  @click.stop="onUnsealFromList(item)"
                >解封</van-button>
              </template>
            </van-cell>
          </van-cell-group>

          <van-empty v-else description="暂无已封存的看板" />
        </van-tab>
      </van-tabs>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  NavBar, Tabs, Tab, Button, Cell, CellGroup,
  Field, Empty
} from 'vant'
import ScanInput from '@/components/ScanInput.vue'
import MaterialCard from '@/components/MaterialCard.vue'
import { getSealLabel, getSealedLabels, toggleSeal } from '@/api/seal'

const activeTab = ref(0)
const scanInputRef = ref(null)

// ==================== 扫码操作 ====================
const label = ref(null)
const toggling = ref(false)

const extras = computed(() => {
  if (!label.value) return []
  return [
    { label: '看板号', value: label.value.kanbanNo },
    { label: '入库单号', value: label.value.docNo },
    { label: '看板数量', value: `${label.value.labelQty} 件` },
    { label: '入库状态', value: label.value.labelStatus },
    {
      label: '封存状态',
      value: label.value.sealed ? '已封存' : '正常',
      warn: label.value.sealed
    },
    ...(label.value.sealed ? [
      { label: '封存时间', value: label.value.sealedAt?.replace('T', ' ') || '-' },
      { label: '封存人', value: label.value.sealedBy || '-' }
    ] : [])
  ]
})

async function onScan(kanbanNo) {
  try {
    const res = await getSealLabel(kanbanNo)
    label.value = res.data
  } catch {
    label.value = null
  }
}

async function onToggle(action) {
  const actionText = action === 'seal' ? '封存' : '解封'
  try {
    const result = await new Promise((resolve) => {
      uni.showModal({
        title: `确认${actionText}`,
        content: `确定${actionText}看板「${label.value.kanbanNo}」吗？`,
        success: (res) => resolve(res.confirm)
      })
    })
    if (!result) return
  } catch { return }

  toggling.value = true
  try {
    const res = await toggleSeal({ kanbanNo: label.value.kanbanNo, action })
    label.value = res.data
    uni.showToast({ title: `${actionText}成功`, icon: 'success' })
  } catch {
    // 错误由请求拦截器处理
  } finally {
    toggling.value = false
  }
}

// ==================== 已封存列表 ====================
const filterMaterialCode = ref('')
const filterSupplierName = ref('')
const sealedList = ref([])
const unsealingId = ref(null)

async function fetchSealedList() {
  const params = {}
  if (filterMaterialCode.value) params.materialCode = filterMaterialCode.value
  if (filterSupplierName.value) params.supplierName = filterSupplierName.value

  try {
    const res = await getSealedLabels(params)
    sealedList.value = res.data || []
  } catch {
    sealedList.value = []
  }
}

async function onUnsealFromList(item) {
  try {
    const result = await new Promise((resolve) => {
      uni.showModal({
        title: '确认解封',
        content: `确定解封看板「${item.kanbanNo}」吗？`,
        success: (res) => resolve(res.confirm)
      })
    })
    if (!result) return
  } catch { return }

  unsealingId.value = item.kanbanNo
  try {
    await toggleSeal({ kanbanNo: item.kanbanNo, action: 'unseal' })
    uni.showToast({ title: '解封成功', icon: 'success' })
    await fetchSealedList()
  } catch {
    // handled by interceptor
  } finally {
    unsealingId.value = null
  }
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped>
.seal-page { min-height: 100vh; }
.seal-body { margin-top: 46px; }
.action { padding: 16px 16px 0; }
.filter { padding: 12px 16px; display: flex; flex-direction: column; gap: 8px; }
.seal-cell-label { white-space: pre-line; }
</style>
