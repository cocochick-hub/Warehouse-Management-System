<template>
  <el-dialog
    :model-value="visible"
    title="打印入库单"
    width="900px"
    destroy-on-close
    @close="handleClose"
  >
    <div class="print-actions">
      <el-button type="primary" :disabled="!detail" @click="handlePrint">
        <el-icon><Printer /></el-icon>打印
      </el-button>
    </div>

    <div v-if="detail" ref="printRef" class="print-sheet">
      <div class="print-header">
        <h2 class="print-title">入 库 单</h2>
        <div class="print-header-info">
          <div class="header-row">
            <span class="header-label">入库单号：</span>
            <span class="header-value">{{ detail.order.docNo }}</span>
            <span class="header-label">供应商：</span>
            <span class="header-value">{{ detail.order.supplier }}</span>
          </div>
          <div class="header-row">
            <span class="header-label">转包状态：</span>
            <span class="header-value">{{ detail.order.transferStatus || '不转包' }}</span>
            <span class="header-label">单据状态：</span>
            <span class="header-value">{{ detail.order.status }}</span>
          </div>
          <div class="header-row">
            <span class="header-label">创建人：</span>
            <span class="header-value">{{ detail.order.createdBy || '-' }}</span>
            <span class="header-label">创建时间：</span>
            <span class="header-value">{{ formatDateTime(detail.order.createdAt) }}</span>
          </div>
          <div class="header-row" v-if="detail.order.remark">
            <span class="header-label">备注：</span>
            <span class="header-value" style="grid-column: span 3">{{ detail.order.remark }}</span>
          </div>
        </div>
      </div>

      <table class="print-table">
        <thead>
          <tr>
            <th>序号</th>
            <th>供应商代码</th>
            <th>供应商名称</th>
            <th>物料号</th>
            <th>物料名称</th>
            <th>包装型号</th>
            <th>包装容量</th>
            <th>库区</th>
            <th>计划数量</th>
            <th>实收数量</th>
            <th>箱数</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in detail.details" :key="item.id || index">
            <td>{{ item.lineNo || index + 1 }}</td>
            <td>{{ item.supplierCode }}</td>
            <td>{{ item.supplierName }}</td>
            <td>{{ item.materialCode }}</td>
            <td>{{ item.materialName }}</td>
            <td>{{ item.packageModel || '-' }}</td>
            <td>{{ item.packagingCapacity || '-' }}</td>
            <td>{{ item.warehouseArea || '默认库区' }}</td>
            <td>{{ item.plannedQty }}</td>
            <td>{{ item.actualQty }}</td>
            <td>{{ item.boxCount ?? '-' }}</td>
            <td>{{ item.remark || '-' }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="8" class="text-right"><strong>合计</strong></td>
            <td><strong>{{ detail.order.plannedTotalQty }}</strong></td>
            <td><strong>{{ detail.order.actualTotalQty }}</strong></td>
            <td colspan="2"></td>
          </tr>
        </tfoot>
      </table>

      <div class="print-footer">
        <div class="footer-item">
          <span class="footer-label">制单人：</span>
          <span class="footer-line">{{ detail.order.createdBy || '' }}</span>
        </div>
        <div class="footer-item">
          <span class="footer-label">收货人：</span>
          <span class="footer-line"></span>
        </div>
        <div class="footer-item">
          <span class="footer-label">日期：</span>
          <span class="footer-line">{{ printDate }}</span>
        </div>
      </div>
    </div>

    <div v-else class="print-empty">
      <p>加载中...</p>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { formatDateTime } from '@/utils/inbound'

const props = defineProps({
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
  margin-bottom: 16px;
}

.print-sheet {
  background: #fff;
  padding: 24px;
  font-size: 13px;
}

.print-header {
  margin-bottom: 20px;
}

.print-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 8px;
  margin: 0 0 16px 0;
}

.print-header-info {
  border: 1px solid #333;
  padding: 12px;
}

.header-row {
  display: grid;
  grid-template-columns: 90px 1fr 90px 1fr;
  gap: 4px 12px;
  margin-bottom: 6px;
}

.header-row:last-child {
  margin-bottom: 0;
}

.header-label {
  font-weight: 600;
  text-align: right;
  white-space: nowrap;
}

.header-value {
  text-align: left;
}

.print-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}

.print-table th,
.print-table td {
  border: 1px solid #333;
  padding: 5px 4px;
  text-align: center;
  font-size: 11px;
}

.print-table thead th {
  background: #f0f0f0;
  font-weight: 600;
}

.print-table tfoot td {
  background: #f8f8f8;
}

.text-right {
  text-align: right;
  padding-right: 8px;
}

.print-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 30px;
  padding: 0 40px;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.footer-label {
  font-weight: 600;
  white-space: nowrap;
}

.footer-line {
  display: inline-block;
  width: 100px;
  border-bottom: 1px solid #333;
}

.print-empty {
  text-align: center;
  padding: 40px;
  color: #909399;
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

  :global(.print-actions) {
    display: none !important;
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
