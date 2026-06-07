<template>
  <PageContainer title="入库单管理">
    <template #actions>
      <el-button type="primary" @click="createVisible = true">
        <el-icon><Plus /></el-icon>创建入库单
      </el-button>
    </template>

    <el-form :model="query" inline>
      <el-form-item label="单号">
        <el-input v-model="query.docNo" placeholder="入库单号" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-input v-model="query.supplier" placeholder="供应商名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="单据状态" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="未入库" value="未入库" />
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
      <el-table-column prop="actualTotalQty" label="实收总数" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleView(row.id)">查看</el-button>
          <el-button type="success" link size="small" @click="goManualInbound">入库</el-button>
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

    <InboundOrderForm
      v-model:visible="createVisible"
      :submitting="submitting"
      @submit="handleSubmitCreate"
    />

    <InboundOrderDetailDialog
      v-model:visible="detailVisible"
      :detail="currentDetail"
    />
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer.vue'
import InboundOrderForm from '@/components/inbound/InboundOrderForm.vue'
import InboundOrderDetailDialog from '@/components/inbound/InboundOrderDetailDialog.vue'
import { createInboundOrderApi, getInboundOrderDetailApi, getInboundOrdersApi } from '@/api/inbound'

const router = useRouter()

const query = reactive({
  docNo: '',
  supplier: '',
  status: ''
})

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const currentDetail = ref(null)
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
    const { data } = await getInboundOrdersApi({
      docNo: query.docNo || undefined,
      supplier: query.supplier || undefined,
      status: query.status || undefined,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (error) {
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

function statusType(status) {
  if (status === '已完成') return 'success'
  if (status === '部分完成') return 'warning'
  return 'info'
}

function formatDateTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}

async function handleView(id) {
  try {
    const { data } = await getInboundOrderDetailApi(id)
    currentDetail.value = data
    detailVisible.value = true
  } catch (error) {
    currentDetail.value = null
  }
}

async function handleSubmitCreate(payload) {
  submitting.value = true
  try {
    await createInboundOrderApi(payload)
    ElMessage.success('入库单创建成功')
    createVisible.value = false
    await fetchOrders()
  } finally {
    submitting.value = false
  }
}

function goManualInbound() {
  router.push('/inbound/manual')
}
</script>

<style scoped>
.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
