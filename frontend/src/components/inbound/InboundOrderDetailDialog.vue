<template>
  <el-dialog
    :model-value="visible"
    title="入库单详情"
    width="900px"
    destroy-on-close
    @close="emit('update:visible', false)"
  >
    <template v-if="detail">
      <el-descriptions :column="2" border class="summary">
        <el-descriptions-item label="入库单号">{{ detail.order.docNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.order.supplier }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail.order.status)">{{ detail.order.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="零件种类数">{{ detail.order.itemCount }}</el-descriptions-item>
        <el-descriptions-item label="计划总数">{{ detail.order.plannedTotalQty }}</el-descriptions-item>
        <el-descriptions-item label="实收总数">{{ detail.order.actualTotalQty }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail.order.createdBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.order.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="detail.details" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="materialCode" label="物料号" min-width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column prop="packagingCapacity" label="包装容量" width="100" />
        <el-table-column prop="plannedQty" label="计划数量" width="100" />
        <el-table-column prop="actualQty" label="实收数量" width="100" />
        <el-table-column prop="pendingQty" label="待入库数量" width="110" />
        <el-table-column prop="remark" label="备注" min-width="140" />
      </el-table>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  detail: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible'])

function statusType(status) {
  if (status === '已完成') return 'success'
  if (status === '部分完成') return 'warning'
  return 'info'
}

function formatDateTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}
</script>

<style scoped>
.summary {
  margin-bottom: 16px;
}
</style>
