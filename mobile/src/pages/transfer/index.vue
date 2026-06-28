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
      <!-- 步骤1: 扫描源看板 -->
      <ScanInput ref="scanInputRef" placeholder="扫码或输入源看板号" @scan="onScanSource" />

      <!-- 源看板信息 -->
      <MaterialCard
        v-if="sourceLabel"
        :material-code="sourceLabel.materialCode"
        :material-name="sourceLabel.materialName"
        :supplier="sourceLabel.supplierName"
        :qty="sourceLabel.availableQty"
        :warehouse-area="sourceLabel.warehouseArea"
        :extra-rows="sourceExtras"
      />

      <!-- 源看板校验失败提示 -->
      <view v-if="sourceError" class="error-tip">
        <van-tag type="danger" size="large">{{ sourceError }}</van-tag>
      </view>

      <!-- 步骤2: 输入转移数量和目标看板号（源看板校验通过后才显示） -->
      <template v-if="sourceLabel && !sourceError">
        <van-cell-group inset title="转包设置">
          <van-field
            v-model="transferQty"
            label="转移数量"
            type="digit"
            placeholder="请输入转移数量"
            :rules="[{ required: true, message: '请填写转移数量' }]"
          />
          <van-field
            v-model="targetKanbanNo"
            label="目标看板号"
            placeholder="留空自动生成（拆包）"
            clearable
          />
        </van-cell-group>

        <view class="transfer-hint">
          <text>源看板剩余：</text>
          <text class="remain-qty">{{ sourceLabel.availableQty - (Number(transferQty) || 0) }}</text>
          <text> 件</text>
        </view>

        <!-- 确认按钮 -->
        <view class="action">
          <van-button
            type="danger"
            block
            round
            :loading="executing"
            :disabled="!canExecute"
            @click="onExecute"
          >确认转包</van-button>
        </view>

        <!-- 转包结果 -->
        <view v-if="result" class="result">
          <van-divider content-position="center">转包完成</van-divider>
          <van-cell-group inset>
            <van-cell title="目标看板号" :value="result.targetKanbanNo" />
            <van-cell title="目标数量" :value="`${result.targetQty} 件`" />
            <van-cell title="源看板剩余" :value="`${result.sourceRemainingQty} 件`" />
          </van-cell-group>
          <view class="action">
            <van-button type="primary" block round @click="onReset">继续转包</van-button>
          </view>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  NavBar, Tabs, Tab, Button, Cell, CellGroup, Field,
  Divider, Tag, Empty
} from 'vant'
import ScanInput from '@/components/ScanInput.vue'
import MaterialCard from '@/components/MaterialCard.vue'
import { getTransferLabel, executeTransfer } from '@/api/transfer'

const scanInputRef = ref(null)
const sourceLabel = ref(null)
const sourceError = ref('')
const transferQty = ref('')
const targetKanbanNo = ref('')
const executing = ref(false)
const result = ref(null)

// 源看板附加信息行
const sourceExtras = computed(() => {
  if (!sourceLabel.value) return []
  return [
    { label: '看板号', value: sourceLabel.value.kanbanNo },
    { label: '入库单号', value: sourceLabel.value.docNo },
    { label: '看板数量', value: `${sourceLabel.value.labelQty} 件` },
    { label: '入库状态', value: sourceLabel.value.labelStatus },
    { label: '转包状态', value: sourceLabel.value.transferStatus || '-' },
    {
      label: '封存状态',
      value: sourceLabel.value.sealed ? '已封存' : '正常',
      warn: sourceLabel.value.sealed
    }
  ]
})

// 是否可以执行转包
const canExecute = computed(() => {
  const qty = Number(transferQty.value)
  return qty > 0 && qty <= (sourceLabel.value?.availableQty || 0) && !executing.value
})

// 扫描源看板
async function onScanSource(kanbanNo) {
  sourceError.value = ''
  result.value = null
  try {
    const res = await getTransferLabel(kanbanNo)
    const data = res.data

    // 校验源看板
    if (data.labelStatus !== '已入库') {
      sourceLabel.value = data
      sourceError.value = `看板状态为「${data.labelStatus}」，无法转包`
      return
    }
    if (data.sealed) {
      sourceLabel.value = data
      sourceError.value = '该看板已封存，无法转包'
      return
    }

    sourceLabel.value = data
    transferQty.value = '' // 清空上次输入
    targetKanbanNo.value = ''
  } catch {
    sourceLabel.value = null
  }
}

// 执行转包
async function onExecute() {
  const actionText = '确认转包'
  const qty = Number(transferQty.value)

  try {
    const confirmed = await new Promise((resolve) => {
      uni.showModal({
        title: actionText,
        content: `从看板「${sourceLabel.value.kanbanNo}」转移 ${qty} 件到${targetKanbanNo.value ? '「' + targetKanbanNo.value + '」' : '新看板'}，确认？`,
        success: (res) => resolve(res.confirm)
      })
    })
    if (!confirmed) return
  } catch { return }

  executing.value = true
  try {
    const res = await executeTransfer({
      sourceKanbanNo: sourceLabel.value.kanbanNo,
      targetKanbanNo: targetKanbanNo.value.trim() || undefined,
      transferQty: qty
    })
    result.value = res.data
    uni.showToast({ title: '转包成功', icon: 'success' })
  } catch {
    // 错误由请求拦截器处理
  } finally {
    executing.value = false
  }
}

// 重置页面，准备下一次转包
function onReset() {
  result.value = null
  sourceLabel.value = null
  sourceError.value = ''
  transferQty.value = ''
  targetKanbanNo.value = ''
  scanInputRef.value?.clear()
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped>
.transfer-page { min-height: 100vh; padding-bottom: 32px; }
.transfer-body { margin-top: 46px; }
.error-tip { text-align: center; margin-top: 12px; }
.transfer-hint {
  padding: 12px 16px; font-size: 14px; color: #646566;
  text-align: center;
}
.remain-qty { color: #1989fa; font-weight: bold; }
.action { padding: 16px 16px 0; }
.result { margin-top: 8px; }
</style>
