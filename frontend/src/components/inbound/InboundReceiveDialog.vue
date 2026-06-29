<template>
  <el-dialog
    :model-value="visible"
    :title="detail ? `手工入库 - ${detail.order.docNo}` : '手工入库'"
    width="1060px"
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

      <div class="section-header">
        <span class="section-title">选择待入库看板</span>
        <div class="section-actions">
          <span class="selected-info" v-if="selectedIds.length > 0">
            已选 {{ selectedIds.length }} 个看板，共 {{ selectedTotalQty }} 件
          </span>
          <el-button size="small" @click="handleSelectAll" v-if="filteredLabels.length > 0">
            {{ allSelected ? '取消全选' : '全选' }}
          </el-button>
        </div>
      </div>

      <!-- 无待入库看板 -->
      <el-empty v-if="filteredLabels.length === 0" description="当前单据没有待入库的看板，请先生成看板" :image-size="60" />

      <el-table
        v-else
        ref="tableRef"
        :data="filteredLabels"
        border stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="kanbanNo" label="看板号" width="200" />
        <el-table-column prop="materialCode" label="物料号" min-width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="160" />
        <el-table-column prop="labelQty" label="数量" width="80" />
        <el-table-column label="包装" width="90">
          <template #default="{ row }">
            {{ row.packageSeq }}/{{ row.packageTotal }}
          </template>
        </el-table-column>
        <el-table-column prop="warehouseArea" label="库区" width="120" />
        <el-table-column prop="labelStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ row.labelStatus }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="selectedIds.length === 0"
        @click="handleSubmit"
      >
        提交入库
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { inboundStatusType } from '@/utils/inbound'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  detail: {
    type: Object,
    default: null
  },
  pendingLabels: {
    type: Array,
    default: () => []
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'submit'])

const tableRef = ref(null)
const selectedIds = ref([])

// 只显示"未入库"状态的看板（同时过滤掉已选择的冗余处理）
const filteredLabels = computed(() => {
  return (props.pendingLabels || []).filter(l => l.labelStatus === '未入库')
})

const allSelected = computed(() => {
  return filteredLabels.value.length > 0 && selectedIds.value.length === filteredLabels.value.length
})

const selectedTotalQty = computed(() => {
  const selectedSet = new Set(selectedIds.value)
  return (props.pendingLabels || [])
    .filter(l => selectedSet.has(l.id))
    .reduce((sum, l) => sum + (l.labelQty || 0), 0)
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

function handleClose() {
  selectedIds.value = []
  emit('update:visible', false)
}

function handleSubmit() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请至少选择一个待入库的看板')
    return
  }
  emit('submit', { labelIds: selectedIds.value })
}
</script>

<style scoped>
.summary {
  margin-bottom: 16px;
}

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
</style>
