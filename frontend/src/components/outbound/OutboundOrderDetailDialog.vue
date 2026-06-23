<template>
  <el-dialog
    :model-value="visible"
    title="出库单详情"
    width="900px"
    destroy-on-close
    @close="handleClose"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <el-descriptions :column="3" border class="mb-16">
          <el-descriptions-item label="出库单号">{{ detail.order.docNo }}</el-descriptions-item>
          <el-descriptions-item label="需求方">{{ detail.order.supplier }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.order.status)" size="small">
              {{ detail.order.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="明细条数">{{ detail.order.itemCount }}</el-descriptions-item>
          <el-descriptions-item label="计划总数">{{ detail.order.plannedTotalQty }}</el-descriptions-item>
          <el-descriptions-item label="实出总数">{{ detail.order.actualTotalQty }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.order.createdBy }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(detail.order.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(detail.order.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">出库明细</h4>
        <el-table :data="detail.details" border>
          <el-table-column prop="lineNo" label="行号" width="60" />
          <el-table-column prop="supplierName" label="需求方" min-width="120" />
          <el-table-column prop="materialCode" label="物料号" min-width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="120" />
          <el-table-column prop="plannedQty" label="计划数量" width="100" />
          <el-table-column prop="actualQty" label="实出数量" width="100" />
          <el-table-column prop="pendingQty" label="待出库" width="100">
            <template #default="{ row }">
              <span :class="{ 'text-danger': row.pendingQty > 0 }">
                {{ row.pendingQty }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="warehouseArea" label="库区" width="120" />
          <el-table-column prop="remark" label="备注" min-width="120">
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
        </el-table>

        <template v-if="detail.stocks && detail.stocks.length">
          <h4 class="section-title">当前库存</h4>
          <el-table :data="detail.stocks" border>
            <el-table-column prop="materialCode" label="物料号" min-width="120" />
            <el-table-column prop="materialName" label="物料名称" min-width="120" />
            <el-table-column prop="supplier" label="供应商" min-width="120" />
            <el-table-column prop="onHandQty" label="库存数量" width="100" />
            <el-table-column prop="lastInboundDocNo" label="最近入库单号" min-width="140" />
            <el-table-column prop="lastInboundAt" label="最近入库时间" width="180">
              <template #default="{ row }">{{ formatDate(row.lastInboundAt) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </template>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getOrderDetail } from '@/api/outbound'

const props = defineProps({
  visible: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:visible'])

const loading = ref(false)
const detail = ref(null)

watch(
  () => props.visible,
  async (value) => {
    if (value && props.orderId) {
      loading.value = true
      try {
        const { data } = await getOrderDetail(props.orderId)
        detail.value = data
      } catch {
        detail.value = null
      } finally {
        loading.value = false
      }
    }
  }
)

function handleClose() {
  emit('update:visible', false)
}

function statusType(status) {
  if (status === '已完成') return 'success'
  if (status === '部分完成') return ''
  return 'warning'
}

function formatDate(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.section-title {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
