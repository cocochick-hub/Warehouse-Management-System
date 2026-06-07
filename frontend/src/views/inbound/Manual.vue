<template>
  <PageContainer title="手工入库">
    <el-alert
      title="选择待入库单据后录入本次实际入库数量，系统会自动更新明细累计入库数量和单据状态。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <el-form :model="query" inline>
      <el-form-item label="入库单号">
        <el-input v-model="query.docNo" placeholder="搜索入库单号" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="query.supplier" placeholder="选择供应商" clearable style="width: 180px">
          <el-option label="全部" value="" />
          <el-option
            v-for="supplier in supplierOptions"
            :key="supplier"
            :label="supplier"
            :value="supplier"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="选择状态" clearable style="width: 140px">
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
      <el-table-column prop="docNo" label="入库单号" width="190" />
      <el-table-column prop="supplier" label="供应商" min-width="160" />
      <el-table-column prop="itemCount" label="种类数" width="90" />
      <el-table-column prop="plannedTotalQty" label="计划总数" width="100" />
      <el-table-column prop="actualTotalQty" label="已入库总数" width="110" />
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
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            size="small"
            :disabled="row.status === '已完成'"
            @click="handleOpenReceive(row.id)"
          >
            开始入库
          </el-button>
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

    <InboundReceiveDialog
      v-model:visible="receiveVisible"
      :detail="currentDetail"
      :submitting="submitting"
      @submit="handleSubmitReceive"
    />
  </PageContainer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import InboundReceiveDialog from '@/components/inbound/InboundReceiveDialog.vue'
import { getInboundOrderDetailApi, getInboundOrdersApi, receiveInboundOrderApi } from '@/api/inbound'

const route = useRoute()
const router = useRouter()

const query = reactive({
  docNo: '',
  supplier: '',
  status: ''
})

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const allRecords = ref([])
const currentDetail = ref(null)
const receiveVisible = ref(false)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const supplierOptions = computed(() => {
  return Array.from(new Set(allRecords.value.map((item) => item.supplier).filter(Boolean)))
})

onMounted(async () => {
  await fetchOrders()
  const routeOrderId = Number(route.query.orderId)
  if (routeOrderId) {
    await handleOpenReceive(routeOrderId)
  }
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

    const allRes = await getInboundOrdersApi({
      docNo: query.docNo || undefined,
      supplier: query.supplier || undefined,
      status: query.status || undefined,
      page: 1,
      size: 200
    })
    allRecords.value = allRes.data.records || []
  } catch (error) {
    tableData.value = []
    allRecords.value = []
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

async function handleOpenReceive(id) {
  try {
    const { data } = await getInboundOrderDetailApi(id)
    currentDetail.value = data
    receiveVisible.value = true
    if (route.query.orderId) {
      router.replace({ path: '/inbound/manual' })
    }
  } catch (error) {
    currentDetail.value = null
  }
}

async function handleSubmitReceive(payload) {
  if (!currentDetail.value?.order?.id) {
    return
  }

  submitting.value = true
  try {
    const { data } = await receiveInboundOrderApi(currentDetail.value.order.id, payload)
    currentDetail.value = data
    ElMessage.success(`入库提交成功，当前状态：${data.order.status}`)
    receiveVisible.value = false
    await fetchOrders()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
