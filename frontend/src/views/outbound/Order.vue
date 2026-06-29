<template>
  <PageContainer title="出库单管理">
    <template #actions>
      <el-button type="warning" @click="createVisible = true">
        <el-icon><Plus /></el-icon>创建出库单
      </el-button>
    </template>

    <el-form :model="query" inline>
      <el-form-item label="单号">
        <el-input v-model="query.docNo" placeholder="出库单号" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-input v-model="query.supplier" placeholder="供应商名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="单据状态" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="待出库" value="待出库" />
          <el-option label="部分完成" value="部分完成" />
          <el-option label="已完成" value="已完成" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="docNo" label="单号" width="180" />
      <el-table-column prop="supplier" label="供应商" min-width="160" />
      <el-table-column prop="itemCount" label="零件种类数" width="100" />
      <el-table-column prop="plannedTotalQty" label="计划总数" width="90" />
      <el-table-column prop="actualTotalQty" label="实发总数" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="outboundStatusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="outboundType" label="出库方式" width="110">
        <template #default="{ row }">
          <el-tag :type="outboundTypeTagType(row.outboundType)" size="small">{{ row.outboundType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleView(row.id)">详情</el-button>
          <el-button
            v-if="row.status !== '已完成'"
            type="success"
            link
            size="small"
            @click="handleIssue(row.id)"
          >出库</el-button>
          <el-button
            v-if="row.actualTotalQty > 0"
            type="danger"
            link
            size="small"
            @click="handleReturn(row.id)"
          >退库</el-button>
          <el-button type="warning" link size="small" @click="handlePrint(row.id)">打印</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :total="pagination.total"
        :current-page="pagination.page"
        :page-size="pagination.size"
        :page-sizes="[5, 10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <OutboundOrderForm
      v-model:visible="createVisible"
      :submitting="submitting"
      @submit="handleSubmitCreate"
    />

    <OutboundOrderDetailDialog
      v-model:visible="detailVisible"
      :order-id="currentOrderId"
    />

    <OutboundIssueDialog
      v-model:visible="issueVisible"
      :order-id="currentOrderId"
      @success="handleIssueSuccess"
    />

    <OutboundPrintDialog
      v-model:visible="printVisible"
      :order="currentPrintOrder"
      :details="currentPrintDetails"
    />

    <OutboundReturnDialog
      v-model:visible="returnVisible"
      :order-id="currentOrderId"
      @success="handleReturnSuccess"
    />
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import PageContainer from '@/components/PageContainer.vue'
import OutboundOrderForm from '@/components/outbound/OutboundOrderForm.vue'
import OutboundOrderDetailDialog from '@/components/outbound/OutboundOrderDetailDialog.vue'
import OutboundIssueDialog from '@/components/outbound/OutboundIssueDialog.vue'
import OutboundPrintDialog from '@/components/outbound/OutboundPrintDialog.vue'
import OutboundReturnDialog from '@/components/outbound/OutboundReturnDialog.vue'
import { listOrders, createOrder, getOrderDetail } from '@/api/outbound'

const route = useRoute()

const query = reactive({
  docNo: route.query.docNo || '',
  supplier: '',
  status: ''
})

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const issueVisible = ref(false)
const returnVisible = ref(false)
const printVisible = ref(false)
const currentOrderId = ref(null)
const currentPrintOrder = ref(null)
const currentPrintDetails = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  fetchOrders()
})

async function fetchOrders() {
  loading.value = true
  try {
    const { data } = await listOrders({
      docNo: query.docNo || undefined,
      supplier: query.supplier || undefined,
      status: query.status || undefined,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchOrders()
}

function handleReset() {
  query.docNo = ''
  query.supplier = ''
  query.status = ''
  pagination.page = 1
  pagination.size = 10
  fetchOrders()
}

function handlePageChange(page) {
  pagination.page = page
  fetchOrders()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  fetchOrders()
}

function handleView(id) {
  currentOrderId.value = id
  detailVisible.value = true
}

function handleIssue(id) {
  currentOrderId.value = id
  issueVisible.value = true
}

async function handlePrint(id) {
  try {
    const { data } = await getOrderDetail(id)
    currentPrintOrder.value = data.order
    currentPrintDetails.value = data.details || []
    printVisible.value = true
  } catch {
    // error handled by interceptor
  }
}

async function handleSubmitCreate(payload) {
  submitting.value = true
  try {
    await createOrder(payload)
    ElMessage.success('出库单创建成功')
    createVisible.value = false
    pagination.page = 1
    await fetchOrders()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

function handleIssueSuccess() {
  pagination.page = 1
  fetchOrders()
}

function handleReturn(id) {
  currentOrderId.value = id
  returnVisible.value = true
}

function handleReturnSuccess() {
  pagination.page = 1
  fetchOrders()
}

function formatDateTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

function outboundStatusType(status) {
  if (status === '已完成') return 'success'
  if (status === '部分完成') return 'info'
  return 'warning'
}

function outboundTypeTagType(type) {
  return type === '带单出库' ? 'success' : 'info'
}
</script>

<style scoped>
.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
