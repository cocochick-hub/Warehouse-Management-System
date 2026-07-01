<template>
  <PageContainer title="转包管理">
    <template #actions>
      <el-button type="primary" @click="openCreateTransfer">
        <el-icon><Plus /></el-icon>新建转包
      </el-button>
      <el-button @click="openInboundCreate">
        <el-icon><Plus /></el-icon>新建入库单
      </el-button>
    </template>
    <el-tabs v-model="activeTab" class="transfer-tabs">
      <!-- 创建转包 Tab -->
      <el-tab-pane label="创建转包" name="create">
        <!-- 搜索筛选 -->
        <el-alert
          class="workflow-alert"
          type="info"
          show-icon
          :closable="false"
          title="转包操作流程"
          description="先选择或扫码源看板，再填写转包数量。目标看板号留空表示拆包并自动创建新看板；填写已存在的同物料看板号表示合包。"
        />

        <el-form inline class="quick-create-form">
          <el-form-item label="源看板号">
            <el-input
              v-model="quickKanbanNo"
              placeholder="扫码或输入源看板号"
              clearable
              style="width: 260px"
              @keyup.enter="handleQuickSelect"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="quickLoading" @click="handleQuickSelect">
              <el-icon><Aim /></el-icon>选择源看板
            </el-button>
          </el-form-item>
        </el-form>

        <el-form :model="kanbanQuery" inline class="search-form">
          <el-form-item label="物料编码">
            <el-input v-model="kanbanQuery.materialCode" placeholder="物料编码" clearable style="width: 140px" />
          </el-form-item>
          <el-form-item label="供应商">
            <el-input v-model="kanbanQuery.supplierName" placeholder="供应商名称" clearable style="width: 140px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleKanbanSearch">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button @click="handleKanbanReset">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 看板列表 -->
        <el-table
          v-loading="kanbanLoading"
          :data="kanbanTableData"
          stripe
          border
          highlight-current-row
          @row-click="handleKanbanRowClick"
          style="width: 100%"
        >
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="kanbanNo" label="看板号" width="200" />
          <el-table-column prop="materialCode" label="物料编码" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="140" />
          <el-table-column prop="supplierName" label="供应商" width="140" />
          <el-table-column prop="warehouseArea" label="库区" width="100" />
          <el-table-column prop="labelQty" label="看板数量" width="90" />
          <el-table-column prop="availableQty" label="可用数量" width="90" />
          <el-table-column prop="labelStatus" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusType(row.labelStatus)" size="small">{{ row.labelStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="transferStatus" label="转包状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.transferStatus" :type="transferStatusType(row.transferStatus)" size="small">
                {{ row.transferStatus }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 看板分页 -->
        <div class="pager-wrap">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="kanbanPagination.total"
            :current-page="kanbanPagination.page"
            :page-size="kanbanPagination.size"
            :page-sizes="[5, 10, 20, 50]"
            @current-change="handleKanbanPageChange"
            @size-change="handleKanbanSizeChange"
          />
        </div>

        <!-- 转包操作区 -->
        <el-card v-if="selectedKanban" class="transfer-card">
          <template #header>
            <span>已选择看板</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="看板号">{{ selectedKanban.kanbanNo }}</el-descriptions-item>
            <el-descriptions-item label="物料编码">{{ selectedKanban.materialCode }}</el-descriptions-item>
            <el-descriptions-item label="物料名称">{{ selectedKanban.materialName }}</el-descriptions-item>
            <el-descriptions-item label="供应商">{{ selectedKanban.supplierName }}</el-descriptions-item>
            <el-descriptions-item label="库区">{{ selectedKanban.warehouseArea }}</el-descriptions-item>
            <el-descriptions-item label="可用数量">
              <el-tag type="success">{{ selectedKanban.availableQty }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-form :model="transferForm" label-width="100px" class="transfer-form">
            <el-form-item label="转包数量">
              <el-input-number
                v-model="transferForm.transferQty"
                :min="1"
                :max="selectedKanban.availableQty"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item label="目标看板号">
              <el-input
                v-model="transferForm.targetKanbanNo"
                placeholder="留空则自动生成"
                clearable
                style="width: 300px"
              />
              <span class="form-tip">（可选，不填则自动生成 T- 前缀看板号）</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleExecuteTransfer">
                执行转包
              </el-button>
              <el-button @click="handleCancelTransfer">取消选择</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 转包历史 Tab -->
      <el-tab-pane label="转包历史" name="history">
        <!-- 历史搜索筛选 -->
        <el-form :model="historyQuery" inline class="search-form">
          <el-form-item label="源看板号">
            <el-input v-model="historyQuery.sourceKanbanNo" placeholder="源看板号" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="目标看板号">
            <el-input v-model="historyQuery.targetKanbanNo" placeholder="目标看板号" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleHistorySearch">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button @click="handleHistoryReset">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 历史列表 -->
        <el-table v-loading="historyLoading" :data="historyTableData" stripe border style="width: 100%">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="createdAt" label="操作时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="sourceKanbanNo" label="源看板号" width="200" />
          <el-table-column prop="targetKanbanNo" label="目标看板号" width="200" />
          <el-table-column prop="transferQty" label="转移数量" width="90" />
          <el-table-column prop="sourceQtyBefore" label="转前数量" width="90" />
          <el-table-column prop="sourceQtyAfter" label="转后数量" width="90" />
          <el-table-column prop="materialCode" label="物料编码" width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="140" />
          <el-table-column prop="supplierName" label="供应商" width="120" />
          <el-table-column prop="operator" label="操作人" width="100" />
          <el-table-column prop="sourceOutboundDocNo" label="源出库单号" width="200">
            <template #default="{ row }">
              <span v-if="row.sourceOutboundDocNo">{{ row.sourceOutboundDocNo }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="targetInboundDocNo" label="目标入库单号" width="200">
            <template #default="{ row }">
              <span v-if="row.targetInboundDocNo">{{ row.targetInboundDocNo }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 历史分页 -->
        <div class="pager-wrap">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="historyPagination.total"
            :current-page="historyPagination.page"
            :page-size="historyPagination.size"
            :page-sizes="[5, 10, 20, 50]"
            @current-change="handleHistoryPageChange"
            @size-change="handleHistorySizeChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 转包结果对话框 -->
    <el-dialog v-model="resultVisible" title="转包成功" width="500px" :close-on-click-modal="false">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="源看板号">{{ transferResult.sourceKanbanNo }}</el-descriptions-item>
        <el-descriptions-item label="源看板剩余数量">{{ transferResult.sourceRemainingQty }}</el-descriptions-item>
        <el-descriptions-item label="目标看板号">{{ transferResult.targetKanbanNo }}</el-descriptions-item>
        <el-descriptions-item label="目标看板数量">{{ transferResult.targetQty }}</el-descriptions-item>
        <el-descriptions-item v-if="transferResult.targetInboundDocNo" label="目标入库单号">
          {{ transferResult.targetInboundDocNo }}
        </el-descriptions-item>
        <el-descriptions-item label="物料编码">{{ transferResult.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ transferResult.materialName }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ transferResult.supplierName }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button v-if="transferResult.targetInboundDocNo" @click="goTargetInboundOrder">
          查看目标入库单
        </el-button>
        <el-button type="primary" @click="handleResultConfirm">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { listAvailableKanbans, listTransferHistory, executeTransfer, getTransferLabel } from '@/api/transfer'

const activeTab = ref('create')
const router = useRouter()

// ========== 看板列表相关 ==========
const kanbanQuery = reactive({
  materialCode: '',
  supplierName: ''
})
const kanbanLoading = ref(false)
const kanbanTableData = ref([])
const kanbanPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const selectedKanban = ref(null)
const quickKanbanNo = ref('')
const quickLoading = ref(false)
const transferForm = reactive({
  transferQty: 1,
  targetKanbanNo: ''
})
const submitting = ref(false)

// ========== 历史记录相关 ==========
const historyQuery = reactive({
  sourceKanbanNo: '',
  targetKanbanNo: ''
})
const historyLoading = ref(false)
const historyTableData = ref([])
const historyPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// ========== 结果对话框 ==========
const resultVisible = ref(false)
const transferResult = ref({})

onMounted(() => {
  fetchKanbans()
})

// 切换 tab 时自动加载对应数据
watch(activeTab, (tab) => {
  if (tab === 'history') {
    historyPagination.page = 1
    fetchHistory()
  } else if (tab === 'create') {
    fetchKanbans()
  }
})

// ========== 看板列表操作 ==========
function openCreateTransfer() {
  activeTab.value = 'create'
  // 清空已选看板和表单，让用户可以重新扫码选择
  selectedKanban.value = null
  transferForm.transferQty = 1
  transferForm.targetKanbanNo = ''
  // 如果在历史tab则刷新列表
  fetchKanbans()
}

function openInboundCreate() {
  router.push({
    path: '/inbound/order',
    query: { create: '1', transferStatus: '转包' }
  })
}

async function handleQuickSelect() {
  const kanbanNo = quickKanbanNo.value.trim()
  if (!kanbanNo) {
    ElMessage.warning('请输入源看板号')
    return
  }

  quickLoading.value = true
  try {
    const { data } = await getTransferLabel(kanbanNo)
    handleKanbanRowClick({
      ...data,
      availableQty: data.availableQty ?? data.labelQty
    })
  } finally {
    quickLoading.value = false
  }
}

async function fetchKanbans() {
  kanbanLoading.value = true
  try {
    const { data } = await listAvailableKanbans({
      materialCode: kanbanQuery.materialCode || undefined,
      supplierName: kanbanQuery.supplierName || undefined,
      page: kanbanPagination.page,
      size: kanbanPagination.size
    })
    kanbanTableData.value = data.records || []
    kanbanPagination.total = data.total || 0
  } catch {
    kanbanTableData.value = []
    kanbanPagination.total = 0
  } finally {
    kanbanLoading.value = false
  }
}

function handleKanbanSearch() {
  kanbanPagination.page = 1
  fetchKanbans()
}

function handleKanbanReset() {
  kanbanQuery.materialCode = ''
  kanbanQuery.supplierName = ''
  kanbanPagination.page = 1
  kanbanPagination.size = 10
  fetchKanbans()
}

function handleKanbanPageChange(page) {
  kanbanPagination.page = page
  fetchKanbans()
}

function handleKanbanSizeChange(size) {
  kanbanPagination.size = size
  kanbanPagination.page = 1
  fetchKanbans()
}

function handleKanbanRowClick(row) {
  // 检查看板是否可转包
  if (row.labelStatus !== '已入库') {
    ElMessage.warning('只有已入库的看板才能转包')
    return
  }
  if (row.sealed) {
    ElMessage.warning('已封存的看板不能转包，请先解封')
    return
  }
  selectedKanban.value = row
  transferForm.transferQty = 1
  transferForm.targetKanbanNo = ''
}

function handleCancelTransfer() {
  selectedKanban.value = null
  transferForm.transferQty = 1
  transferForm.targetKanbanNo = ''
}

async function handleExecuteTransfer() {
  if (!selectedKanban.value) {
    ElMessage.warning('请先选择看板')
    return
  }
  if (!transferForm.transferQty || transferForm.transferQty < 1) {
    ElMessage.warning('转包数量必须大于0')
    return
  }
  if (transferForm.transferQty > selectedKanban.value.availableQty) {
    ElMessage.warning('转包数量不能超过可用数量')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认将 ${selectedKanban.value.kanbanNo} 转出 ${transferForm.transferQty} 件物料？`,
      '转包确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const { data } = await executeTransfer({
      sourceKanbanNo: selectedKanban.value.kanbanNo,
      targetKanbanNo: transferForm.targetKanbanNo || undefined,
      transferQty: transferForm.transferQty
    })
    transferResult.value = data
    resultVisible.value = true
    // 刷新看板列表
    fetchKanbans()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

function handleResultConfirm() {
  resultVisible.value = false
  handleCancelTransfer()
  // 切换到历史Tab
  activeTab.value = 'history'
  fetchHistory()
}

function goTargetInboundOrder() {
  const docNo = transferResult.value.targetInboundDocNo
  resultVisible.value = false
  router.push({
    path: '/inbound/order',
    query: docNo ? { docNo } : undefined
  })
}

// ========== 历史记录操作 ==========
async function fetchHistory() {
  historyLoading.value = true
  try {
    const { data } = await listTransferHistory({
      sourceKanbanNo: historyQuery.sourceKanbanNo || undefined,
      targetKanbanNo: historyQuery.targetKanbanNo || undefined,
      page: historyPagination.page,
      size: historyPagination.size
    })
    historyTableData.value = data.records || []
    historyPagination.total = data.total || 0
  } catch {
    historyTableData.value = []
    historyPagination.total = 0
  } finally {
    historyLoading.value = false
  }
}

function handleHistorySearch() {
  historyPagination.page = 1
  fetchHistory()
}

function handleHistoryReset() {
  historyQuery.sourceKanbanNo = ''
  historyQuery.targetKanbanNo = ''
  historyPagination.page = 1
  historyPagination.size = 10
  fetchHistory()
}

function handleHistoryPageChange(page) {
  historyPagination.page = page
  fetchHistory()
}

function handleHistorySizeChange(size) {
  historyPagination.size = size
  historyPagination.page = 1
  fetchHistory()
}

// ========== 工具方法 ==========
function formatDateTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

function statusType(status) {
  if (status === '已入库') return 'success'
  if (status === '已出库') return 'info'
  return 'warning'
}

function transferStatusType(status) {
  if (status === '已转包') return 'danger'
  if (status === '转包' || status === '部分转包') return 'warning'
  if (status === '转入') return 'success'
  return 'info'
}
</script>

<style scoped>
.transfer-tabs {
  margin-top: 0;
}

.search-form {
  margin-bottom: 16px;
}

.workflow-alert {
  margin-bottom: 16px;
}

.quick-create-form {
  margin-bottom: 8px;
}

.transfer-card {
  margin-top: 20px;
}

.transfer-form {
  margin-top: 20px;
  max-width: 500px;
}

.form-tip {
  margin-left: 10px;
  color: #999;
  font-size: 12px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
