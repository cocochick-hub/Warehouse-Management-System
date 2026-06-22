<template>
  <PageContainer title="入库历史">
    <el-form :model="query" inline>
      <el-form-item label="单号">
        <el-input v-model="query.docNo" placeholder="入库单号" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-input v-model="query.supplier" placeholder="供应商名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="物料号">
        <el-input v-model="query.materialCode" placeholder="物料号" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="转包状态">
        <el-select v-model="query.transferStatus" placeholder="全部" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="不转包" value="不转包" />
          <el-option label="转包" value="转包" />
        </el-select>
      </el-form-item>
      <el-form-item label="库区">
        <el-select v-model="query.warehouseArea" placeholder="全部" clearable style="width: 130px">
          <el-option label="全部" value="" />
          <el-option label="默认库区" value="默认库区" />
          <el-option label="库区1" value="库区1" />
          <el-option label="库区2" value="库区2" />
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
      <el-table-column prop="docNo" label="入库单号" width="180" />
      <el-table-column prop="supplier" label="供应商" min-width="160" />
      <el-table-column prop="itemCount" label="零件种类数" width="100" />
      <el-table-column prop="plannedTotalQty" label="计划总数" width="90" />
      <el-table-column prop="actualTotalQty" label="实收总数" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="inboundStatusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="transferStatus" label="转包状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.transferStatus === '转包' ? 'warning' : 'info'" size="small">
            {{ row.transferStatus || '不转包' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="最近入库时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleView(row.id)">查看详情</el-button>
          <el-button type="info" link size="small" @click="handlePrintOrder(row.id)">打印单据</el-button>
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

    <InboundOrderDetailDialog
      v-model:visible="detailVisible"
      :detail="currentDetail"
    />

    <InboundOrderPrintDialog
      v-model:visible="printOrderVisible"
      :detail="printOrderDetail"
    />
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'
import InboundOrderDetailDialog from '@/components/inbound/InboundOrderDetailDialog.vue'
import InboundOrderPrintDialog from '@/components/inbound/InboundOrderPrintDialog.vue'
import { getInboundHistoryApi, getInboundOrderDetailApi } from '@/api/inbound'
import { formatDateTime, inboundStatusType } from '@/utils/inbound'

const query = reactive({
  docNo: '',
  supplier: '',
  materialCode: '',
  transferStatus: '',
  warehouseArea: ''
})

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const printOrderVisible = ref(false)
const currentDetail = ref(null)
const printOrderDetail = ref(null)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  fetchHistory()
})

async function fetchHistory() {
  loading.value = true
  try {
    const { data } = await getInboundHistoryApi({
      docNo: query.docNo || undefined,
      supplier: query.supplier || undefined,
      materialCode: query.materialCode || undefined,
      transferStatus: query.transferStatus || undefined,
      warehouseArea: query.warehouseArea || undefined,
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
  fetchHistory()
}

function handleReset() {
  query.docNo = ''
  query.supplier = ''
  query.materialCode = ''
  query.transferStatus = ''
  query.warehouseArea = ''
  pagination.page = 1
  pagination.size = 10
  fetchHistory()
}

function handlePageChange(page) {
  pagination.page = page
  fetchHistory()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  fetchHistory()
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

async function handlePrintOrder(id) {
  try {
    const { data } = await getInboundOrderDetailApi(id)
    printOrderDetail.value = data
    printOrderVisible.value = true
  } catch (error) {
    printOrderDetail.value = null
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
