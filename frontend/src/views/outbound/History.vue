<template>
  <PageContainer title="出库历史">
    <el-form :model="searchForm" inline>
      <el-form-item label="出库单号">
        <el-input v-model="searchForm.docNo" placeholder="输入出库单号" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="已出库" value="已出库" />
          <el-option label="已退库" value="已退库" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="createdAt" label="出库时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="docNo" label="出库单号" min-width="170" />
      <el-table-column prop="materialCode" label="物料号" min-width="120" />
      <el-table-column prop="materialName" label="物料名称" min-width="120" />
      <el-table-column prop="supplierName" label="需求方" min-width="120" />
      <el-table-column prop="issueQty" label="出库数量" width="100" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '已退库' ? 'warning' : 'success'" size="small">
            {{ row.status || '已出库' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceInboundDoc" label="来源入库单号" min-width="160">
        <template #default="{ row }">{{ row.sourceInboundDoc || '-' }}</template>
      </el-table-column>
      <el-table-column prop="warehouseArea" label="库区" width="100" />
      <el-table-column prop="issuedBy" label="操作人" width="100" />
    </el-table>

    <div class="pager-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :total="pagination.total"
        :current-page="pagination.page"
        :page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listHistory } from '@/api/outbound'
import PageContainer from '@/components/PageContainer.vue'

const loading = ref(false)
const tableData = ref([])
const searchForm = reactive({ docNo: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

onMounted(() => fetchHistory())

async function fetchHistory() {
  loading.value = true
  try {
    const { data } = await listHistory({
      docNo: searchForm.docNo || undefined,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = data.content || []
    pagination.total = data.totalElements || 0
  } catch {
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
  searchForm.docNo = ''
  searchForm.status = ''
  pagination.page = 1
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

function formatDate(value) {
  if (!value) return '-'
  return value.replace('T', ' ')
}
</script>

<style scoped>
.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
