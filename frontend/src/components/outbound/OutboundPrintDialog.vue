<template>
  <el-dialog
    :model-value="visible"
    title="打印出库单"
    width="1100px"
    destroy-on-close
    @close="handleClose"
  >
    <div class="print-actions">
      <el-button type="primary" :disabled="!order" @click="handlePrint">
        <el-icon><Printer /></el-icon>打印
      </el-button>
    </div>

    <div ref="printRef" class="print-sheet" v-if="order">
      <div class="order-header">
        <h2>出库单</h2>
        <div class="order-meta">
          <span>单号：{{ order.docNo }}</span>
          <span>需求方：{{ order.supplier }}</span>
          <span>出库方式：{{ order.outboundType }}</span>
          <span>日期：{{ printDate }}</span>
        </div>
      </div>

      <table class="detail-table">
        <thead>
          <tr>
            <th>序号</th>
            <th>物料号</th>
            <th>物料名称</th>
            <th>需求方</th>
            <th>计划数量</th>
            <th>实发数量</th>
            <th>待出库</th>
            <th>库区</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in details" :key="item.id">
            <td>{{ idx + 1 }}</td>
            <td>{{ item.materialCode }}</td>
            <td>{{ item.materialName }}</td>
            <td>{{ item.supplierName }}</td>
            <td>{{ item.plannedQty }}</td>
            <td>{{ item.actualQty }}</td>
            <td>{{ item.pendingQty }}</td>
            <td>{{ item.warehouseArea || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  order: {
    type: Object,
    default: null
  },
  details: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:visible'])

const printRef = ref()
const printDate = computed(() => new Date().toISOString().slice(0, 10))

function handleClose() {
  emit('update:visible', false)
}

async function handlePrint() {
  await nextTick()
  window.print()
}
</script>

<style scoped>
.print-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.print-sheet {
  padding: 16px;
  background: #fff;
}

.order-header {
  text-align: center;
  margin-bottom: 24px;
}

.order-header h2 {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 700;
}

.order-meta {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 14px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
}

.detail-table th,
.detail-table td {
  border: 1px solid #999;
  padding: 8px 6px;
  text-align: center;
  font-size: 13px;
}

.detail-table th {
  background: #f5f7fa;
  font-weight: 600;
}

@media print {
  :global(body *) {
    visibility: hidden !important;
  }

  :global(.print-sheet),
  :global(.print-sheet *) {
    visibility: visible !important;
  }

  :global(.print-sheet) {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    padding: 12mm;
  }

  :global(.el-overlay),
  :global(.el-dialog),
  :global(.el-dialog__body) {
    position: static !important;
    box-shadow: none !important;
    padding: 0 !important;
    margin: 0 !important;
  }
}
</style>
