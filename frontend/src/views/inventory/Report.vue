<template>
  <PageContainer title="库存报表">
    <el-form :model="query" inline>
      <el-form-item label="物料号">
        <el-input v-model="query.materialCode" placeholder="物料号" clearable />
      </el-form-item>
      <el-form-item label="物料名称">
        <el-input v-model="query.materialName" placeholder="物料名称" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-input v-model="query.supplier" placeholder="供应商名称" clearable style="width: 180px" />
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
      <el-table-column prop="materialCode" label="物料号" width="150" />
      <el-table-column prop="materialName" label="物料名称" min-width="160" />
      <el-table-column prop="supplier" label="供应商" min-width="170" />
      <el-table-column prop="onHandQty" label="当前库存" width="110" />
      <el-table-column prop="lastInboundDocNo" label="最近入库单号" width="190" />
      <el-table-column prop="lastInboundAt" label="最近入库时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.lastInboundAt) }}
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
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'
import { getInventoryStocksApi } from '@/api/inventory'
import { formatDateTime } from '@/utils/inbound'

const query = reactive({
  materialCode: '',
  materialName: '',
  supplier: ''
})

const loading = ref(false)
const tableData = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  fetchStocks()
})

async function fetchStocks() {
  loading.value = true
  try {
    const { data } = await getInventoryStocksApi({
      materialCode: query.materialCode || undefined,
      materialName: query.materialName || undefined,
      supplier: query.supplier || undefined,
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
  fetchStocks()
}

function handleReset() {
  query.materialCode = ''
  query.materialName = ''
  query.supplier = ''
  pagination.page = 1
  pagination.size = 10
  fetchStocks()
}

function handlePageChange(page) {
  pagination.page = page
  fetchStocks()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  fetchStocks()
}
</script>

<style scoped>
.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
