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
          <el-option
            v-for="area in warehouseAreaOptions"
            :key="area.areaCode"
            :label="area.areaName"
            :value="area.areaName"
          />
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
      <el-table-column prop="materialCode" label="物料号" width="150" />
      <el-table-column prop="materialName" label="物料名称" min-width="160" />
      <el-table-column prop="supplier" label="供应商" min-width="170" />
      <el-table-column prop="onHandQty" label="当前库存" width="110" />
      <el-table-column prop="availableQty" label="可用库存" width="110">
        <template #default="{ row }">
          <span :class="row.availableQty <= 0 ? 'no-qty' : ''">{{ row.availableQty ?? row.onHandQty }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="transferStatus" label="转包状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.transferStatus === '转包' ? 'warning' : 'info'" size="small">
            {{ row.transferStatus || '不转包' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseArea" label="库区" width="110" />
      <el-table-column prop="lastInboundDocNo" label="最近入库单号" width="190" />
      <el-table-column prop="lastInboundAt" label="最近入库时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.lastInboundAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewLabels(row)">
            <el-icon><Document /></el-icon>查看板
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

    <!-- 看板详情弹窗 -->
    <el-dialog
      v-model="labelDialogVisible"
      :title="`看板详情 - ${labelMaterialCode}`"
      width="1100px"
      destroy-on-close
    >
      <el-table v-loading="labelLoading" :data="labelData" stripe border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="kanbanNo" label="看板号" min-width="200" />
        <el-table-column label="二维码" width="100" align="center">
          <template #default="{ row }">
            <img v-if="qrMap[row.kanbanNo]" :src="qrMap[row.kanbanNo]" alt="二维码" style="width: 80px; height: 80px;" />
          </template>
        </el-table-column>
        <el-table-column prop="docNo" label="入库单号" width="180" />
        <el-table-column prop="labelQty" label="看板数量" width="100" />
        <el-table-column prop="availableQty" label="可用数量" width="100">
          <template #default="{ row }">
            <span :class="row.availableQty <= 0 ? 'no-qty' : ''">{{ row.availableQty ?? row.labelQty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="packageSeq" label="第几包" width="80">
          <template #default="{ row }">{{ row.packageSeq }}/{{ row.packageTotal }}</template>
        </el-table-column>
        <el-table-column prop="warehouseArea" label="库区" width="100" />
        <el-table-column prop="labelStatus" label="入库状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.labelStatus === '已入库' ? 'success' : 'info'" size="small">{{ row.labelStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sealed" label="封存状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.sealed" type="danger" size="small">已封存</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="封存信息" min-width="160">
          <template #default="{ row }">
            <template v-if="row.sealed">
              {{ row.sealedBy || '-' }} / {{ formatDateTime(row.sealedAt) }}
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="receivedAt" label="入库时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.receivedAt) }}</template>
        </el-table-column>
      </el-table>

      <template v-if="!labelLoading && labelData.length === 0" #default>
        <el-empty description="该物料暂无看板记录" />
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import QRCode from 'qrcode'
import PageContainer from '@/components/PageContainer.vue'
import { getInventoryStocksApi, getInventoryLabelsApi } from '@/api/inventory'
import { getWarehouseAreasApi } from '@/api/basic'
import { formatDateTime } from '@/utils/inbound'

const query = reactive({
  materialCode: '',
  materialName: '',
  supplier: '',
  transferStatus: '',
  warehouseArea: ''
})

const loading = ref(false)
const tableData = ref([])
const warehouseAreaOptions = ref([])
const labelDialogVisible = ref(false)
const labelLoading = ref(false)
const labelMaterialCode = ref('')
const labelData = ref([])
const qrMap = ref({})
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  fetchStocks()
  fetchWarehouseAreas()
})

async function fetchStocks() {
  loading.value = true
  try {
    const { data } = await getInventoryStocksApi({
      materialCode: query.materialCode || undefined,
      materialName: query.materialName || undefined,
      supplier: query.supplier || undefined,
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

async function fetchWarehouseAreas() {
  try {
    const { data } = await getWarehouseAreasApi()
    warehouseAreaOptions.value = data || []
  } catch {
    warehouseAreaOptions.value = []
  }
}

async function handleViewLabels(row) {
  labelMaterialCode.value = `${row.materialCode} - ${row.materialName}`
  labelDialogVisible.value = true
  labelLoading.value = true
  labelData.value = []
  try {
    const { data } = await getInventoryLabelsApi({
      materialCode: row.materialCode,
      supplier: row.supplier,
      warehouseArea: row.warehouseArea
    })
    labelData.value = data || []
  } catch {
    labelData.value = []
  } finally {
    labelLoading.value = false
  }
}

// 监听看板数据变化，生成二维码
watch(labelData, async (labels) => {
  const nextMap = {}
  for (const label of labels || []) {
    try {
      nextMap[label.kanbanNo] = await QRCode.toDataURL(label.qrPayload || label.kanbanNo, {
        width: 160,
        margin: 1,
        errorCorrectionLevel: 'M'
      })
    } catch {
      // 二维码生成失败，跳过
    }
  }
  qrMap.value = nextMap
})

function handleSearch() {
  pagination.page = 1
  fetchStocks()
}

function handleReset() {
  query.materialCode = ''
  query.materialName = ''
  query.supplier = ''
  query.transferStatus = ''
  query.warehouseArea = ''
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

.no-qty {
  color: #f56c6c;
  font-weight: 600;
}
</style>
