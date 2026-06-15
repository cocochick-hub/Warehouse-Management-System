<template>
  <el-dialog :model-value="visible" :title="detail ? `执行出库 - ${detail.order.docNo}` : '执行出库'" width="960px" destroy-on-close @close="handleClose">
    <template v-if="detail">
      <el-descriptions :column="2" border class="summary">
        <el-descriptions-item label="出库单号">{{ detail.order.docNo }}</el-descriptions-item>
        <el-descriptions-item label="需求方">{{ detail.order.supplier }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.order.status === '已完成' ? 'success' : 'warning'">{{ detail.order.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="form.details" border stripe>
        <el-table-column prop="materialCode" label="物料号" min-width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column prop="supplierName" label="需求方" min-width="140" />
        <el-table-column prop="plannedQty" label="计划数量" width="100" />
        <el-table-column prop="actualQty" label="已出库" width="100" />
        <el-table-column prop="pendingQty" label="待出库" width="100" />
        <el-table-column label="本次出库" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.issueQty" :min="0" :max="row.pendingQty" :disabled="row.pendingQty <= 0" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column prop="warehouseArea" label="库区" width="120" />
      </el-table>

      <template v-if="detail.stocks?.length">
        <div class="stock-title">当前库存</div>
        <el-table :data="detail.stocks" border stripe>
          <el-table-column prop="materialCode" label="物料号" min-width="140" />
          <el-table-column prop="materialName" label="物料名称" min-width="160" />
          <el-table-column prop="supplier" label="供应商" min-width="140" />
          <el-table-column prop="onHandQty" label="库存数量" width="100" />
        </el-table>
      </template>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认出库</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderDetail, issueOrder } from '@/api/outbound'

const props = defineProps({
  visible: { type: Boolean, default: false },
  orderId: { type: Number, default: null }
})

const emit = defineEmits(['update:visible', 'success'])

const detail = ref(null)
const submitting = ref(false)
const form = reactive({ details: [] })

watch([() => props.visible, () => props.orderId], async ([vis, id]) => {
  if (vis && id) {
    submitting.value = false
    try {
      const { data } = await getOrderDetail(id)
      detail.value = data
      form.details = (data.details || []).map(d => ({
        detailId: d.id,
        materialCode: d.materialCode,
        materialName: d.materialName,
        supplierName: d.supplierName,
        plannedQty: d.plannedQty,
        actualQty: d.actualQty,
        pendingQty: d.pendingQty,
        warehouseArea: d.warehouseArea,
        issueQty: 0
      }))
    } catch { detail.value = null }
  }
}, { immediate: true })

function handleSubmit() {
  const issueDetails = form.details
    .filter(d => d.issueQty > 0)
    .map(d => ({ detailId: d.detailId, issueQty: d.issueQty }))

  if (!issueDetails.length) { ElMessage.warning('请至少填写一条出库数量'); return }

  submitting.value = true
  issueOrder(props.orderId, { details: issueDetails }).then(() => {
    ElMessage.success('出库成功')
    emit('success')
    emit('update:visible', false)
  }).catch(() => {}).finally(() => { submitting.value = false })
}

function handleClose() { emit('update:visible', false) }
</script>

<style scoped>
.summary { margin-bottom: 16px; }
.stock-title { font-weight: 600; margin: 16px 0 8px; font-size: 14px; }
</style>
