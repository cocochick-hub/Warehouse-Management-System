<template>
  <el-dialog
    :model-value="visible"
    title="打印入库看板"
    width="1100px"
    destroy-on-close
    @close="handleClose"
  >
    <div class="print-actions">
      <el-button type="primary" :disabled="!labels.length" @click="handlePrint">
        <el-icon><Printer /></el-icon>打印
      </el-button>
    </div>

    <div ref="printRef" class="print-sheet">
      <div v-for="label in labels" :key="label.id" class="kanban-card">
        <div class="kanban-info">
          <div class="cell label">零件号</div>
          <div class="cell value strong">{{ label.materialCode }}</div>
          <div class="cell label">供应商代码</div>
          <div class="cell value">{{ label.supplierName }}</div>
          <div class="cell label">器具型号</div>
          <div class="cell value">{{ label.packageModel || '-' }}</div>
          <div class="cell label">库区</div>
          <div class="cell value">{{ label.warehouseArea || '默认库区' }}</div>
          <div class="cell label">日期</div>
          <div class="cell value">{{ printDate }}</div>
          <div class="cell label">数量</div>
          <div class="cell value">{{ label.labelQty }}（第 {{ label.packageSeq }}/{{ label.packageTotal }} 包）</div>
          <div class="cell label">转包状态</div>
          <div class="cell value">{{ label.transferStatus || '不转包' }}</div>
          <div class="cell label bottom-label">看板号</div>
          <div class="cell value bottom-value">{{ label.kanbanNo }}</div>
        </div>
        <div class="kanban-qr">
          <img v-if="qrMap[label.kanbanNo]" :src="qrMap[label.kanbanNo]" alt="二维码" />
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  labels: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:visible'])

const printRef = ref()
const qrMap = ref({})
const printDate = computed(() => new Date().toISOString().slice(0, 10))

watch(
  () => props.labels,
  async (value) => {
    const nextMap = {}
    for (const label of value || []) {
      nextMap[label.kanbanNo] = await QRCode.toDataURL(label.qrPayload || label.kanbanNo, {
        width: 180,
        margin: 1,
        errorCorrectionLevel: 'M'
      })
    }
    qrMap.value = nextMap
  },
  { immediate: true, deep: true }
)

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
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 8px;
  background: #fff;
}

.kanban-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  border: 1px solid #9a9a9a;
  min-height: 210px;
  break-inside: avoid;
  page-break-inside: avoid;
}

.kanban-info {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  border-right: 1px solid #9a9a9a;
}

.cell {
  min-height: 26px;
  padding: 5px 8px;
  border-right: 1px solid #b8b8b8;
  border-bottom: 1px solid #b8b8b8;
  color: #111;
  font-size: 14px;
  line-height: 1.2;
}

.label {
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.value {
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  word-break: break-all;
}

.strong {
  font-size: 20px;
  font-weight: 800;
}

.bottom-label,
.bottom-value {
  border-bottom: none;
}

.bottom-value {
  font-size: 12px;
}

.kanban-qr {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
}

.kanban-qr img {
  width: 160px;
  height: 160px;
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
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10mm;
    padding: 8mm;
  }

  :global(.kanban-card) {
    min-height: 58mm;
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
