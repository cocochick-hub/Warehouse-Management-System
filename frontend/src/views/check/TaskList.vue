<template>
  <PageContainer title="盘点任务管理">
    <template #actions>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>创建盘点任务
      </el-button>
    </template>

    <!-- 任务列表 -->
    <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="taskNo" label="盘点单号" width="180" />
      <el-table-column prop="taskName" label="盘点名称" min-width="150" />
      <el-table-column prop="checkType" label="盘点模式" width="100">
        <template #default="{ row }">
          <el-tag :type="row.checkType === '明盘' ? 'success' : 'warning'" size="small">
            {{ row.checkType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseArea" label="盘点库区" width="120">
        <template #default="{ row }">{{ row.warehouseArea || '全库' }}</template>
      </el-table-column>
      <el-table-column prop="materialCode" label="盘点物料" width="140">
        <template #default="{ row }">{{ row.materialCode || '全部' }}</template>
      </el-table-column>
      <el-table-column label="盘点进度" width="140">
        <template #default="{ row }">
          <div class="progress-cell">
            <el-progress :percentage="row.progressPercent" :stroke-width="8" />
            <span class="progress-text">{{ row.checkedCount }}/{{ row.detailCount }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="创建人" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewDetails(row)">查看明细</el-button>
          <el-button
            v-if="row.status === '进行中'"
            type="success"
            link
            size="small"
            @click="handleComplete(row)"
          >
            完成
          </el-button>
          <el-button
            v-if="row.status === '已完成'"
            type="danger"
            link
            size="small"
            @click="viewReport(row)"
          >
            差异报告
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建盘点任务弹窗 -->
    <el-dialog v-model="createVisible" title="创建盘点任务" width="600px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" label-width="100px">
        <el-form-item label="盘点名称" prop="taskName" :rules="[{ required: true, message: '必填' }]">
          <el-input v-model="createForm.taskName" placeholder="如：A区2026年6月盘点" />
        </el-form-item>
        <el-form-item label="盘点模式" prop="checkType">
          <el-radio-group v-model="createForm.checkType">
            <el-radio label="明盘">明盘（扫码显示系统库存）</el-radio>
            <el-radio label="盲盘">盲盘（扫码不显示系统库存）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="盘点库区">
          <el-select v-model="createForm.warehouseArea" placeholder="全库（不限定）" clearable style="width: 100%">
            <el-option v-for="area in warehouseAreaOptions" :key="area.areaCode"
              :label="area.areaName" :value="area.areaName" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点物料">
          <el-input v-model="createForm.materialCode" placeholder="不填则盘点全部物料" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 明细列表弹窗 -->
    <el-dialog v-model="detailVisible" :title="`盘点明细 - ${currentTask?.taskName}`" width="900px" destroy-on-close>
      <el-table v-loading="detailLoading" :data="detailList" stripe border max-height="500">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="materialCode" label="物料编码" width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="130" />
        <el-table-column prop="supplier" label="供应商" min-width="130" />
        <el-table-column prop="warehouseArea" label="库区" width="100" />
        <el-table-column prop="systemQty" label="系统库存" width="90" align="right" />
        <el-table-column prop="actualQty" label="实盘数量" width="90" align="right">
          <template #default="{ row }">{{ row.actualQty ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="diffQty" label="差异" width="80" align="right">
          <template #default="{ row }">
            <span v-if="row.diffQty != null" :class="getDiffClass(row.diffQty)">
              {{ formatDiff(row.diffQty) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getDetailStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button
              v-if="row.status === '已盘' && row.diffQty !== 0"
              type="primary"
              link
              size="small"
              @click="openAdjustDialog(row)"
            >
              差异调整
            </el-button>
            <span v-else-if="row.status === '已调整'" style="color: #67c23a; font-size: 12px">已调整</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 差异调整弹窗 -->
    <el-dialog v-model="adjustVisible" title="差异调整" width="400px" destroy-on-close>
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="物料">
          <div>{{ currentDetail?.materialName }}</div>
          <div style="font-size: 12px; color: #909399">{{ currentDetail?.materialCode }}</div>
        </el-form-item>
        <el-form-item label="系统库存">
          <span>{{ currentDetail?.systemQty }} 件</span>
        </el-form-item>
        <el-form-item label="实盘数量">
          <span>{{ currentDetail?.actualQty }} 件</span>
        </el-form-item>
        <el-form-item label="差异">
          <span :class="getDiffClass(currentDetail?.diffQty)">{{ formatDiff(currentDetail?.diffQty) }}</span>
        </el-form-item>
        <el-form-item label="调整后库存" :rules="[{ required: true, message: '必填' }]">
          <el-input-number v-model="adjustForm.adjustQty" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustSubmitting" @click="handleAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'
import { listTasksApi, createTaskApi, getTaskDetailsApi, completeTaskApi, adjustDetailApi } from '@/api/check'
import { getWarehouseAreasApi } from '@/api/basic'
import { formatDateTime } from '@/utils/inbound'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const createVisible = ref(false)
const createFormRef = ref(null)
const submitting = ref(false)
const warehouseAreaOptions = ref([])
const createForm = reactive({
  taskName: '',
  checkType: '明盘',
  warehouseArea: '',
  materialCode: ''
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentTask = ref(null)
const detailList = ref([])

const adjustVisible = ref(false)
const adjustSubmitting = ref(false)
const currentDetail = ref(null)
const adjustForm = reactive({ adjustQty: 0 })

onMounted(() => {
  fetchData()
  loadWarehouseAreas()
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await listTasksApi()
    tableData.value = data || []
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

async function loadWarehouseAreas() {
  try {
    const { data } = await getWarehouseAreasApi()
    warehouseAreaOptions.value = data || []
  } catch { warehouseAreaOptions.value = [] }
}

async function openCreateDialog() {
  createForm.taskName = ''
  createForm.checkType = '明盘'
  createForm.warehouseArea = ''
  createForm.materialCode = ''
  createVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createTaskApi({
      taskName: createForm.taskName.trim(),
      checkType: createForm.checkType,
      warehouseArea: createForm.warehouseArea || null,
      materialCode: createForm.materialCode.trim() || null
    })
    ElMessage.success('盘点任务创建成功')
    createVisible.value = false
    fetchData()
  } catch { /* 拦截器已处理 */ }
  finally { submitting.value = false }
}

async function viewDetails(row) {
  currentTask.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    const { data } = await getTaskDetailsApi(row.id)
    detailList.value = data || []
  } catch { detailList.value = [] }
  finally { detailLoading.value = false }
}

async function handleComplete(row) {
  try {
    await ElMessageBox.confirm('完成任务后，将锁定差异数据，确定要完成吗？', '提示', { type: 'warning' })
    await completeTaskApi(row.id)
    ElMessage.success('任务已完成')
    fetchData()
  } catch { /* 取消或错误 */ }
}

function openAdjustDialog(row) {
  currentDetail.value = row
  adjustForm.adjustQty = row.systemQty + row.diffQty
  adjustVisible.value = true
}

async function handleAdjust() {
  if (adjustForm.adjustQty == null || adjustForm.adjustQty < 0) {
    ElMessage.warning('请输入有效的调整数量')
    return
  }
  adjustSubmitting.value = true
  try {
    await adjustDetailApi(currentDetail.value.id, adjustForm.adjustQty, 'admin')
    ElMessage.success('差异调整成功')
    adjustVisible.value = false
    viewDetails(currentTask.value)
    fetchData()
  } catch { /* 拦截器已处理 */ }
  finally { adjustSubmitting.value = false }
}

function viewReport(row) {
  viewDetails(row)
}

function getStatusType(status) {
  if (status === '进行中') return 'primary'
  if (status === '已完成') return 'success'
  if (status === '已取消') return 'info'
  return 'default'
}

function getDetailStatusType(status) {
  if (status === '已盘') return 'success'
  if (status === '已调整') return 'primary'
  return 'info'
}

function getDiffClass(diff) {
  if (diff == null || diff === 0) return ''
  return diff > 0 ? 'diff-profit' : 'diff-loss'
}

function formatDiff(diff) {
  if (diff == null) return '0'
  return diff > 0 ? `+${diff}` : String(diff)
}
</script>

<style scoped>
.progress-cell { display: flex; align-items: center; gap: 8px; }
.progress-text { font-size: 12px; color: #909399; white-space: nowrap; }
.diff-profit { color: #67c23a; font-weight: 600; }
.diff-loss { color: #f56c6c; font-weight: 600; }
</style>