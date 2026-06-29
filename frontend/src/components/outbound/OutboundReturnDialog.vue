<template>
  <el-dialog
    :model-value="visible"
    :title="detail ? `退库 - ${detail.order.docNo}` : '退库'"
    width="1000px"
    destroy-on-close
    @close="handleClose"
  >
    <template v-if="detail">
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

      <div class="section-header">
        <span class="section-title">选择已出库看板退库</span>
        <div class="section-actions">
          <span class="selected-info" v-if="selectedIds.length > 0">
            已选 {{ selectedIds.length }} 个看板，共 {{ selectedTotalQty }} 件
          </span>
          <el-button size="small" @click="handleSelectAll" v-if="issuedLabels.length > 0">
            {{ allSelected ? '取消全选' : '全选' }}
          </el-button>
        </div>
      </div>

      <el-empty v-if="issuedLabels.length === 0" description="当前出库单没有已出库的看板可退库" :image-size="60" />

      <el-table
        v-else
        ref="tableRef"
        :data="issuedLabels"
        border stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="kanbanNo" label="看板号" width="220" />
        <el-table-column prop="materialCode" label="物料号" min-width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column prop="supplierName" label="需求方" min-width="120" />
        <el-table-column prop="issueQty" label="出库数量" width="90" />
        <el-table-column prop="warehouseArea" label="库区" width="100" />
        <el-table-column prop="sourceInboundDoc" label="来源入库单" min-width="200" />
        <el-table-column label="出库时间" width="170">
          <template #default="{ row }">
            {{ row.issuedAt ? row.issuedAt.replace('T', ' ') : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="danger"
        :loading="submitting"
        :disabled="selectedIds.length === 0"
        @click="handleSubmit"
      >
        确认退库（{{ selectedTotalQty }}件）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, getIssuedLabels, returnByLabels } from '@/api/outbound'

const props = defineProps({
  visible: { type: Boolean, default: false },
  orderId: { type: Number, default: null }
})

const emit = defineEmits(['update:visible', 'success'])

const detail = ref(null)
const issuedLabels = ref([])
const submitting = ref(false)
const tableRef = ref(null)
const selectedIds = ref([])

watch([() => props.visible, () => props.orderId], async ([vis, id]) => {
  if (vis && id) {
    submitting.value = false
    selectedIds.value = []
    try {
      const [detailRes, labelsRes] = await Promise.all([
        getOrderDetail(id),
        getIssuedLabels(id)
      ])
      detail.value = detailRes.data
      issuedLabels.value = labelsRes.data || []
    } catch {
      detail.value = null
      issuedLabels.value = []
    }
  }
}, { immediate: true })

const allSelected = computed(() => {
  return issuedLabels.value.length > 0 && selectedIds.value.length === issuedLabels.value.length
})

const selectedTotalQty = computed(() => {
  const selectedSet = new Set(selectedIds.value)
  return issuedLabels.value
    .filter(l => selectedSet.has(l.labelId))
    .reduce((sum, l) => sum + (l.issueQty || 0), 0)
})

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.labelId)
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
  try {
    await ElMessageBox.confirm(
      `确认退库 ${selectedIds.value.length} 个看板，共 ${selectedTotalQty.value} 件？`,
      '退库确认',
      { confirmButtonText: '确认退库', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    await returnByLabels(props.orderId, { labelIds: selectedIds.value })
    ElMessage.success('退库成功')
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
  color: #e6a23c;
}
</style>
