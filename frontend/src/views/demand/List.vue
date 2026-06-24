<template>
  <PageContainer title="物料需求">
    <template #actions>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>手工创建需求
      </el-button>
      <el-button @click="$router.push('/inventory/import')">
        <el-icon><Upload /></el-icon>Excel导入
      </el-button>
    </template>

    <!-- 查询条件 -->
    <el-form :model="query" inline style="margin-bottom: 12px">
      <el-form-item label="物料号">
        <el-input v-model="query.materialCode" placeholder="物料号" clearable />
      </el-form-item>
      <el-form-item label="物料名称">
        <el-input v-model="query.materialName" placeholder="物料名称" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-input v-model="query.supplier" placeholder="供应商名称" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
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

    <!-- 需求列表 -->
    <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="batchNo" label="需求批次号" width="200" />
      <el-table-column prop="materialCode" label="物料号" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="supplierName" label="供应商" min-width="150" />
      <el-table-column prop="demandQty" label="需求数量" width="100" />
      <el-table-column prop="fulfilledQty" label="已满足" width="80" />
      <el-table-column prop="demandDate" label="需求日期" width="110">
        <template #default="{ row }">{{ row.demandDate || '-' }}</template>
      </el-table-column>
      <el-table-column prop="warehouseArea" label="期望库区" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.statusType || 'info'" size="small">{{ row.statusLabel || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
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

    <!-- 手工创建需求弹窗 -->
    <el-dialog v-model="createVisible" title="手工创建需求" width="700px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" label-width="90px">
        <div v-for="(item, idx) in createForm.items" :key="idx" class="create-item-row">
          <el-row :gutter="8">
            <el-col :span="5">
              <el-form-item label="物料号" :prop="`items.${idx}.materialCode`"
                :rules="[{ required: true, message: '必填' }]">
                <el-input v-model="item.materialCode" placeholder="如 MAT-TOOL-001" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="名称" :prop="`items.${idx}.materialName`"
                :rules="[{ required: true, message: '必填' }]">
                <el-input v-model="item.materialName" placeholder="物料名称" />
              </el-form-item>
            </el-col>
            <el-col :span="5">
              <el-form-item label="供应商" :prop="`items.${idx}.supplierName`"
                :rules="[{ required: true, message: '必填' }]">
                <el-input v-model="item.supplierName" placeholder="供应商名称" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="8">
            <el-col :span="3">
              <el-form-item label="数量" :prop="`items.${idx}.demandQty`"
                :rules="[{ required: true, message: '必填' }]">
                <el-input-number v-model="item.demandQty" :min="1" controls-position="right" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-form-item label="日期">
                <el-date-picker v-model="item.demandDate" type="date" placeholder="需求日期" value-format="YYYY-MM-DD"
                  style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-form-item label="库区">
                <el-input v-model="item.warehouseArea" placeholder="默认库区" />
              </el-form-item>
            </el-col>
            <el-col :span="3">
              <el-form-item label="备注">
                <el-input v-model="item.remark" placeholder="备注" />
              </el-form-item>
            </el-col>
            <el-col :span="2">
              <el-button type="danger" link @click="removeItem(idx)" :disabled="createForm.items.length <= 1">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-col>
          </el-row>
          <el-row :gutter="8">
            <el-col :span="5">
              <el-form-item label="供应商代码" :prop="`items.${idx}.supplierCode`"
                :rules="[{ required: true, message: '必填' }]">
                <el-input v-model="item.supplierCode" placeholder="如 SUP-001" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-divider v-if="idx < createForm.items.length - 1" style="margin: 8px 0" />
        </div>
      </el-form>
      <div style="margin-bottom: 12px">
        <el-button type="success" @click="addItem">
          <el-icon><Plus /></el-icon>添加一行
        </el-button>
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'
import { listDemandsApi, createDemandApi } from '@/api/demand'
import { formatDateTime } from '@/utils/inbound'
import { ElMessage } from 'element-plus'

const query = reactive({ materialCode: '', materialName: '', supplier: '', status: '' })
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

// 创建弹窗
const createVisible = ref(false)
const createFormRef = ref(null)
const submitting = ref(false)
const createForm = reactive({
  items: [newItem()]
})

function newItem() {
  return { materialCode: '', materialName: '', supplierCode: '', supplierName: '', demandQty: null, demandDate: null, warehouseArea: '', remark: '' }
}

function addItem() { createForm.items.push(newItem()) }
function removeItem(idx) { createForm.items.splice(idx, 1) }

function openCreateDialog() {
  createForm.items = [newItem()]
  createVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createDemandApi(createForm.items.map(it => ({
      materialCode: it.materialCode.trim(),
      materialName: it.materialName.trim(),
      supplierCode: it.supplierCode.trim(),
      supplierName: it.supplierName.trim(),
      demandQty: it.demandQty,
      demandDate: it.demandDate || null,
      warehouseArea: it.warehouseArea || undefined,
      remark: it.remark || undefined
    })))
    ElMessage.success('需求创建成功')
    createVisible.value = false
    fetchData()
  } catch { /* 拦截器已处理 */ }
  finally { submitting.value = false }
}

// 列表加载
onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const { data } = await listDemandsApi({
      page: pagination.page,
      size: pagination.size,
      materialCode: query.materialCode || undefined,
      materialName: query.materialName || undefined,
      supplier: query.supplier || undefined,
      status: query.status || undefined
    })
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

function handleSearch() { pagination.page = 1; fetchData() }
function handleReset() {
  query.materialCode = ''; query.materialName = ''; query.supplier = ''; query.status = ''
  pagination.page = 1; fetchData()
}
function handlePageChange(p) { pagination.page = p; fetchData() }
function handleSizeChange(s) { pagination.size = s; pagination.page = 1; fetchData() }
</script>

<style scoped>
.pager-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.create-item-row { padding: 4px 0; }
</style>
