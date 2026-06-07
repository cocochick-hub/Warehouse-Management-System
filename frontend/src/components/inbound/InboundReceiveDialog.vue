<template>
  <el-dialog
    :model-value="visible"
    :title="detail ? `手工入库 - ${detail.order.docNo}` : '手工入库'"
    width="960px"
    destroy-on-close
    @close="handleClose"
  >
    <template v-if="detail">
      <el-descriptions :column="2" border class="summary">
        <el-descriptions-item label="入库单号">{{ detail.order.docNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.order.supplier }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="inboundStatusType(detail.order.status)">{{ detail.order.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="计划总数">{{ detail.order.plannedTotalQty }}</el-descriptions-item>
        <el-descriptions-item label="已入库总数">{{ detail.order.actualTotalQty }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="form.details" border stripe>
        <el-table-column prop="lineNo" label="行号" width="70" />
        <el-table-column prop="materialCode" label="物料号" min-width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column prop="plannedQty" label="计划数量" width="100" />
        <el-table-column prop="actualQty" label="累计入库" width="100" />
        <el-table-column prop="pendingQty" label="待入库" width="100" />
        <el-table-column label="本次入库" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.receiveQty"
              :min="0"
              :max="row.pendingQty"
              :disabled="row.pendingQty <= 0"
              controls-position="right"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
      </el-table>

      <template v-if="detail.inventoryStocks?.length">
        <div class="stock-title">当前库存快照</div>
        <el-table :data="detail.inventoryStocks" border stripe>
          <el-table-column prop="materialCode" label="物料号" min-width="140" />
          <el-table-column prop="materialName" label="物料名称" min-width="160" />
          <el-table-column prop="onHandQty" label="当前库存" width="100" />
          <el-table-column prop="lastInboundDocNo" label="最近入库单号" width="190" />
          <el-table-column prop="lastInboundAt" label="最近入库时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.lastInboundAt) }}
            </template>
          </el-table-column>
        </el-table>
      </template>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交入库</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { formatDateTime, inboundStatusType } from '@/utils/inbound'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  detail: {
    type: Object,
    default: null
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'submit'])

const form = reactive({
  details: []
})

watch(
  () => props.detail,
  (value) => {
    form.details = (value?.details || []).map((item) => ({
      ...item,
      receiveQty: 0
    }))
  },
  { immediate: true }
)

function handleClose() {
  emit('update:visible', false)
}

function handleSubmit() {
  const details = []
  for (const item of form.details) {
    const receiveQty = Math.max(0, item.receiveQty ?? 0)
    if (receiveQty > item.pendingQty) {
      ElMessage.warning(`物料 ${item.materialCode} 的本次入库数量不能大于待入库数量`)
      return
    }
    details.push({
      detailId: item.id,
      receiveQty
    })
  }
  const totalReceiveQty = details.reduce((sum, item) => sum + item.receiveQty, 0)
  if (totalReceiveQty <= 0) {
    ElMessage.warning('请至少填写一条大于 0 的入库数量')
    return
  }
  emit('submit', { details })
}
</script>

<style scoped>
.summary {
  margin-bottom: 16px;
}

.stock-title {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
</style>
