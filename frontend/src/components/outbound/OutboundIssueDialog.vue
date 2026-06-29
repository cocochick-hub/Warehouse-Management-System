<template>
  <el-dialog
    :model-value="visible"
    :title="detail ? `执行出库 - ${detail.order.docNo}` : '执行出库'"
    width="1100px"
    destroy-on-close
    @close="handleClose"
  >
    <template v-if="detail">
      <!-- 出库单摘要 -->
      <el-descriptions :column="2" border class="summary">
        <el-descriptions-item label="出库单号">{{ detail.order.docNo }}</el-descriptions-item>
        <el-descriptions-item label="需求方">{{ detail.order.supplier }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.order.status === '已完成' ? 'success' : 'warning'">{{ detail.order.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.order.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划总数">{{ detail.order.plannedTotalQty }}</el-descriptions-item>
        <el-descriptions-item label="实发总数">{{ detail.order.actualTotalQty }}</el-descriptions-item>
      </el-descriptions>

      <!-- 明细行摘要：显示每个物料的待出库/已选情况 -->
      <div class="section-header">
        <span class="section-title">物料明细</span>
      </div>
      <el-table :data="detailSummaries" border stripe size="small">
        <el-table-column prop="materialCode" label="物料号" width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="supplierName" label="需求方" width="120" />
        <el-table-column prop="plannedQty" label="计划数" width="80" />
        <el-table-column prop="actualQty" label="已出库" width="80" />
        <el-table-column prop="pendingQty" label="待出库" width="80" />
        <el-table-column label="本次已选" width="160">
          <template #default="{ row }">
            <span :class="{ 'over-limit': row.selectedQty > row.pendingQty }">
              {{ row.selectedQty }}
            </span>
            <span v-if="row.kbnCount > 0"> 件（{{ row.kbnCount }}个看板）</span>
            <span v-else class="zero-hint">0</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span v-if="row.selectedQty > row.pendingQty" class="over-limit-tag">超出 {{ row.selectedQty - row.pendingQty }} 件</span>
            <span v-else-if="row.selectedQty > 0 && row.selectedQty <= row.pendingQty" class="ok-tag">正常</span>
            <span v-else class="zero-hint">-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 看板选择区 -->
      <div class="section-header">
        <span class="section-title">选择看板号出库</span>
        <div class="section-actions">
          <el-select v-model="filterMaterial" placeholder="按物料筛选" clearable size="small" style="width: 200px">
            <el-option v-for="m in materials" :key="m" :label="m" :value="m" />
          </el-select>
          <span class="selected-info" v-if="selectedIds.length > 0">
            已选 {{ selectedIds.length }} 个看板，共 {{ selectedTotalQty }} 件
          </span>
          <el-button size="small" @click="handleSelectAll" v-if="filteredLabels.length > 0">
            {{ allSelected ? '取消全选' : '全选' }}
          </el-button>
        </div>
      </div>

      <!-- 无可用看板 -->
      <el-empty v-if="filteredLabels.length === 0" description="当前没有可出库的看板，请确认物料已入库且看板未被封存或已出库" :image-size="60" />

      <!-- 看板选择表格 -->
      <el-table
        v-else
        ref="tableRef"
        :data="filteredLabels"
        border stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="kanbanNo" label="看板号" width="220" />
        <el-table-column prop="materialCode" label="物料号" min-width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column prop="supplierName" label="需求方" min-width="120" />
        <el-table-column prop="labelQty" label="数量" width="80" />
        <el-table-column label="包装" width="90">
          <template #default="{ row }">
            {{ row.packageSeq }}/{{ row.packageTotal }}
          </template>
        </el-table-column>
        <el-table-column prop="warehouseArea" label="库区" width="100" />
        <el-table-column prop="docNo" label="来源入库单" min-width="200" />
      </el-table>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="selectedIds.length === 0 || overLimit"
        @click="handleSubmit"
      >
        <span v-if="overLimit">看板数量超出待出库数量</span>
        <span v-else>确认出库（{{ selectedTotalQty }}件）</span>
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderDetail, getAvailableKanbanLabels, issueByLabels } from '@/api/outbound'

const props = defineProps({
  visible: { type: Boolean, default: false },
  orderId: { type: Number, default: null }
})

const emit = defineEmits(['update:visible', 'success'])

const detail = ref(null)
const kanbanLabels = ref([])
const submitting = ref(false)
const tableRef = ref(null)
const selectedIds = ref([])
const filterMaterial = ref('')

watch([() => props.visible, () => props.orderId], async ([vis, id]) => {
  if (vis && id) {
    submitting.value = false
    selectedIds.value = []
    filterMaterial.value = ''
    try {
      const [detailRes, labelsRes] = await Promise.all([
        getOrderDetail(id),
        getAvailableKanbanLabels(id)
      ])
      detail.value = detailRes.data
      kanbanLabels.value = labelsRes.data || []
    } catch {
      detail.value = null
      kanbanLabels.value = []
    }
  }
}, { immediate: true })

// 物料列表（供筛选下拉框）
const materials = computed(() => {
  const codes = new Set()
  kanbanLabels.value.forEach(l => codes.add(l.materialCode))
  return Array.from(codes).sort()
})

// 按物料筛选后的看板
const filteredLabels = computed(() => {
  if (!filterMaterial.value) return kanbanLabels.value
  return kanbanLabels.value.filter(l => l.materialCode === filterMaterial.value)
})

// 全选状态
const allSelected = computed(() => {
  return filteredLabels.value.length > 0 && selectedIds.value.length === filteredLabels.value.length
})

// 选中看板的总件数
const selectedTotalQty = computed(() => {
  const selectedSet = new Set(selectedIds.value)
  return kanbanLabels.value
    .filter(l => selectedSet.has(l.id))
    .reduce((sum, l) => sum + (l.labelQty || 0), 0)
})

// 按出库明细行汇总：含待出库和已选情况
const detailSummaries = computed(() => {
  if (!detail.value) return []
  const selectedSet = new Set(selectedIds.value)
  return (detail.value.details || []).map(d => {
    const matched = kanbanLabels.value.filter(
      l => l.matchedOutboundDetailId === d.id && selectedSet.has(l.id)
    )
    const selectedQty = matched.reduce((sum, l) => sum + (l.labelQty || 0), 0)
    return {
      detailId: d.id,
      materialCode: d.materialCode,
      materialName: d.materialName,
      supplierName: d.supplierName,
      plannedQty: d.plannedQty,
      actualQty: d.actualQty,
      pendingQty: d.pendingQty,
      selectedQty,
      kbnCount: matched.length
    }
  })
})

// 是否超量：任一明细行选中看板总数量超过待出库数
const overLimit = computed(() => {
  return detailSummaries.value.some(d => d.selectedQty > d.pendingQty)
})

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.id)
}

function handleSelectAll() {
  if (!tableRef.value) return
  if (allSelected.value) {
    tableRef.value.clearSelection()
  } else {
    tableRef.value.toggleAllSelection()
  }
}

async function handleSubmit() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请至少选择一个看板')
    return
  }
  if (overLimit.value) {
    ElMessage.warning('看板数量超出待出库数量，请调整选择')
    return
  }
  submitting.value = true
  try {
    await issueByLabels(props.orderId, { labelIds: selectedIds.value })
    ElMessage.success('出库成功')
    emit('success')
    emit('update:visible', false)
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  selectedIds.value = []
  filterMaterial.value = ''
  emit('update:visible', false)
}
</script>

<style scoped>
.summary { margin-bottom: 16px; }

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 0 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-info {
  font-size: 13px;
  color: #409eff;
}

.over-limit {
  color: #f56c6c;
  font-weight: 700;
}

.over-limit-tag {
  color: #f56c6c;
  font-weight: 600;
  font-size: 12px;
}

.ok-tag {
  color: #67c23a;
  font-weight: 600;
  font-size: 12px;
}

.zero-hint {
  color: #c0c4cc;
}
</style>
