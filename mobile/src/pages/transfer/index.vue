<template>
  <view class="transfer-page">
    <van-nav-bar
      title="转包"
      left-arrow
      @click-left="goBack"
      fixed
      safe-area-inset-top
    />

    <view class="transfer-body">
      <view class="section">
        <view class="section-title">源看板</view>
        <ScanInput ref="sourceScanRef" placeholder="扫码或输入源看板号" @scan="onScanSource" />

        <MaterialCard
          v-if="sourceLabel"
          :material-code="sourceLabel.materialCode"
          :material-name="sourceLabel.materialName"
          :supplier="sourceLabel.supplierName"
          :qty="sourceLabel.availableQty"
          :warehouse-area="sourceLabel.warehouseArea"
          :extra-rows="sourceExtras"
        />

        <view v-if="sourceError" class="error-tip">
          <van-tag type="danger" size="large">{{ sourceError }}</van-tag>
        </view>
      </view>

      <template v-if="sourceLabel && !sourceError">
        <view class="section">
          <view class="section-title">转包方式</view>
          <view class="mode-tabs">
            <button
              v-for="item in transferModes"
              :key="item.value"
              class="mode-tab"
              :class="{ active: transferMode === item.value }"
              @click="selectMode(item.value)"
            >
              {{ item.label }}
            </button>
          </view>

          <van-cell-group inset>
            <van-field
              v-model="transferQty"
              label="转移数量"
              type="digit"
              placeholder="请输入转移数量"
            />
            <van-cell title="源看板剩余" :value="`${remainingQty} 件`" />
          </van-cell-group>

          <view v-if="transferMode !== 'auto'" class="target-box">
            <ScanInput
              ref="targetScanRef"
              :autoscan="false"
              :placeholder="targetPlaceholder"
              @scan="onScanTarget"
            />
          </view>

          <van-cell-group v-if="targetLabel" inset title="目标看板">
            <van-cell title="看板号" :value="targetLabel.kanbanNo" />
            <van-cell title="物料号" :value="targetLabel.materialCode" />
            <van-cell title="物料名称" :value="targetLabel.materialName" />
            <van-cell title="当前数量" :value="`${targetLabel.availableQty ?? targetLabel.labelQty ?? 0} 件`" />
            <van-cell title="封存状态" :value="targetLabel.sealed ? '已封存' : '正常'" />
          </van-cell-group>

          <view v-if="targetError" class="error-tip">
            <van-tag type="danger" size="large">{{ targetError }}</van-tag>
          </view>

          <view class="action">
            <van-button
              type="danger"
              block
              round
              :loading="executing"
              :disabled="!canExecute"
              @click="onExecute"
            >
              {{ submitText }}
            </van-button>
          </view>
        </view>

        <view v-if="result" class="section result">
          <van-divider content-position="center">转包完成</van-divider>
          <van-cell-group inset>
            <van-cell title="转包类型" :value="result.transferType || transferModeLabel" />
            <van-cell title="目标看板号" :value="result.targetKanbanNo" />
            <van-cell title="目标数量" :value="`${result.targetQty} 件`" />
            <van-cell title="源看板剩余" :value="`${result.sourceRemainingQty} 件`" />
            <van-cell title="目标入库单号" :value="result.targetInboundDocNo || '-'" />
            <van-cell title="二维码内容" :label="targetQrPayload" />
          </van-cell-group>

          <view class="result-actions">
            <van-button plain type="primary" block round @click="copyText(result.targetKanbanNo)">
              复制目标看板号
            </van-button>
            <van-button
              v-if="result.targetInboundDocNo"
              plain
              type="primary"
              block
              round
              @click="copyText(result.targetInboundDocNo)"
            >
              复制入库单号
            </van-button>
            <van-button type="primary" block round @click="onReset">继续转包</van-button>
          </view>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import ScanInput from '@/components/ScanInput.vue'
import MaterialCard from '@/components/MaterialCard.vue'
import { getTransferLabel, executeTransfer } from '@/api/transfer'

const sourceScanRef = ref(null)
const targetScanRef = ref(null)
const sourceLabel = ref(null)
const targetLabel = ref(null)
const sourceError = ref('')
const targetError = ref('')
const transferQty = ref('')
const targetKanbanNo = ref('')
const transferMode = ref('auto')
const executing = ref(false)
const result = ref(null)

const transferModes = [
  { label: '自动新建', value: 'auto' },
  { label: '指定新建', value: 'custom' },
  { label: '合入已有', value: 'merge' }
]

const sourceExtras = computed(() => {
  if (!sourceLabel.value) return []
  return [
    { label: '看板号', value: sourceLabel.value.kanbanNo },
    { label: '入库单号', value: sourceLabel.value.docNo },
    { label: '看板数量', value: `${sourceLabel.value.labelQty} 件` },
    { label: '入库状态', value: sourceLabel.value.labelStatus },
    { label: '封存状态', value: sourceLabel.value.sealed ? '已封存' : '正常', warn: sourceLabel.value.sealed }
  ]
})

const availableQty = computed(() => Number(sourceLabel.value?.availableQty || 0))
const inputQty = computed(() => Number(transferQty.value || 0))
const remainingQty = computed(() => Math.max(availableQty.value - inputQty.value, 0))

const transferModeLabel = computed(() => {
  const item = transferModes.find(mode => mode.value === transferMode.value)
  return item?.label || '转包'
})

const targetPlaceholder = computed(() => {
  return transferMode.value === 'merge' ? '扫码或输入已有目标看板号' : '输入新目标看板号'
})

const submitText = computed(() => {
  if (transferMode.value === 'merge') return '确认合包'
  return '确认拆包'
})

const targetQrPayload = computed(() => {
  return result.value?.targetKanbanNo ? `WMS-INBOUND|${result.value.targetKanbanNo}` : ''
})

const canExecute = computed(() => {
  if (executing.value || !sourceLabel.value || sourceError.value || targetError.value) return false
  if (inputQty.value <= 0 || inputQty.value > availableQty.value) return false
  if (transferMode.value !== 'auto' && !targetKanbanNo.value.trim()) return false
  if (targetKanbanNo.value.trim() && targetKanbanNo.value.trim() === sourceLabel.value.kanbanNo) return false
  return true
})

async function onScanSource(kanbanNo) {
  sourceError.value = ''
  targetError.value = ''
  result.value = null
  targetLabel.value = null

  try {
    const res = await getTransferLabel(kanbanNo)
    const data = res.data
    sourceLabel.value = data

    if (data.labelStatus !== '已入库') {
      sourceError.value = `看板状态为「${data.labelStatus}」，无法转包`
      return
    }
    if (data.sealed) {
      sourceError.value = '该看板已封存，无法转包'
      return
    }
    if (!Number(data.availableQty || 0)) {
      sourceError.value = '该看板无可转包数量'
      return
    }

    transferQty.value = ''
    targetKanbanNo.value = ''
    transferMode.value = 'auto'
  } catch {
    sourceLabel.value = null
  }
}

async function onScanTarget(kanbanNo) {
  targetKanbanNo.value = kanbanNo
  targetError.value = ''
  targetLabel.value = null

  if (!kanbanNo || transferMode.value !== 'merge') return
  if (sourceLabel.value?.kanbanNo === kanbanNo) {
    targetError.value = '目标看板不能与源看板相同'
    return
  }

  try {
    const res = await getTransferLabel(kanbanNo)
    const data = res.data
    targetLabel.value = data

    if (data.labelStatus !== '已入库') {
      targetError.value = `目标看板状态为「${data.labelStatus}」，无法合包`
    } else if (data.sealed) {
      targetError.value = '目标看板已封存，无法合包'
    } else if (data.materialCode !== sourceLabel.value.materialCode) {
      targetError.value = '目标看板物料与源看板不一致'
    }
  } catch {
    targetError.value = '目标看板不存在，无法合包'
  }
}

function selectMode(mode) {
  transferMode.value = mode
  targetKanbanNo.value = ''
  targetLabel.value = null
  targetError.value = ''
  targetScanRef.value?.clear()
}

async function onExecute() {
  const qty = inputQty.value
  const targetNo = targetKanbanNo.value.trim()

  const targetText = transferMode.value === 'auto'
    ? '自动新建目标看板'
    : `目标看板「${targetNo}」`

  const confirmed = await showConfirm(
    submitText.value,
    `从源看板「${sourceLabel.value.kanbanNo}」转移 ${qty} 件，${targetText}，确认？`
  )
  if (!confirmed) return

  executing.value = true
  try {
    const res = await executeTransfer({
      sourceKanbanNo: sourceLabel.value.kanbanNo,
      targetKanbanNo: targetNo || undefined,
      transferQty: qty
    })
    result.value = res.data
    sourceLabel.value = {
      ...sourceLabel.value,
      labelQty: res.data.sourceRemainingQty,
      availableQty: res.data.sourceRemainingQty
    }
    transferQty.value = ''
    uni.showToast({ title: '转包成功', icon: 'success' })
  } catch {
    // 错误由请求拦截器处理
  } finally {
    executing.value = false
  }
}

function showConfirm(title, content) {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      success: (res) => resolve(res.confirm),
      fail: () => resolve(false)
    })
  })
}

function copyText(text) {
  if (!text) return
  uni.setClipboardData({
    data: text,
    success() {
      uni.showToast({ title: '已复制', icon: 'success' })
    }
  })
}

function onReset() {
  result.value = null
  sourceLabel.value = null
  targetLabel.value = null
  sourceError.value = ''
  targetError.value = ''
  transferQty.value = ''
  targetKanbanNo.value = ''
  transferMode.value = 'auto'
  sourceScanRef.value?.clear()
  targetScanRef.value?.clear()
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped>
.transfer-page { min-height: 100vh; padding-bottom: 32px; background: #f7f8fa; }
.transfer-body { margin-top: 46px; padding: 12px 0 20px; }
.section { margin-bottom: 12px; }
.section-title {
  padding: 0 16px 8px;
  font-size: 13px;
  color: #646566;
  font-weight: 600;
}
.mode-tabs {
  margin: 0 16px 12px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.mode-tab {
  height: 36px;
  border: 1px solid #dcdee0;
  background: #fff;
  border-radius: 6px;
  font-size: 14px;
  color: #323233;
}
.mode-tab.active {
  border-color: #1989fa;
  background: #eaf4ff;
  color: #1989fa;
  font-weight: 600;
}
.target-box { padding: 0 16px; }
.error-tip { text-align: center; margin: 10px 16px 0; }
.action { padding: 16px 16px 0; }
.result { margin-top: 8px; }
.result-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
}
</style>
