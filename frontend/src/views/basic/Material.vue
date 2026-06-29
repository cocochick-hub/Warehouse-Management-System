<template>
  <PageContainer title="物料管理">
    <template #actions>
      <el-button v-if="isAdmin" type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增物料
      </el-button>
    </template>

    <el-form :model="query" inline>
      <el-form-item label="物料号">
        <el-input v-model="query.materialNo" placeholder="物料号" clearable />
      </el-form-item>
      <el-form-item label="物料名称">
        <el-input v-model="query.materialName" placeholder="物料名称" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="query.supplier" placeholder="选择供应商" clearable style="width: 160px">
          <el-option label="全部" value="" />
          <el-option
            v-for="supplier in supplierOptions"
            :key="supplier"
            :label="supplier"
            :value="supplier"
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

    <el-table v-loading="loading" :data="pagedData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="materialNo" label="物料号" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="160" />
      <el-table-column prop="materialType" label="物料类型" width="100" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="supplierName" label="供应商" width="160" />
      <el-table-column prop="packageModel" label="包装型号" width="130" />
      <el-table-column prop="packageCapacity" label="包装容量" width="100" />
      <el-table-column v-if="isAdmin" label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="filteredData.length"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑物料' : '新增物料'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="物料号" prop="materialNo" v-if="!isEdit">
          <el-input v-model="form.materialNo" placeholder="请输入物料号" maxlength="50" />
        </el-form-item>
        <el-form-item label="物料名称" prop="materialName">
          <el-input v-model="form.materialName" placeholder="请输入物料名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="物料类型" prop="materialType">
          <el-input v-model="form.materialType" placeholder="请输入物料类型" maxlength="50" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="请输入单位" maxlength="20" />
        </el-form-item>
        <el-form-item label="供应商代码" prop="supplierCode">
          <el-input v-model="form.supplierCode" placeholder="请输入供应商代码" maxlength="50" />
        </el-form-item>
        <el-form-item label="供应商名称" prop="supplierName">
          <el-input v-model="form.supplierName" placeholder="请输入供应商名称" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { getMaterialsApi, getSuppliersApi, createMaterialApi, updateMaterialApi, deleteMaterialApi } from '@/api/basic'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'admin')

const query = reactive({
  materialNo: '',
  materialName: '',
  supplier: ''
})

const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = ref(10)
const tableData = ref([])
const suppliers = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  materialNo: '',
  materialName: '',
  materialType: '',
  unit: '',
  supplierCode: '',
  supplierName: ''
})

const rules = {
  materialNo: [{ required: true, message: '请输入物料号', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入物料名称', trigger: 'blur' }],
  supplierCode: [{ required: true, message: '请输入供应商代码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }]
}

const supplierOptions = computed(() => {
  return suppliers.value.map((item) => item.supplierName)
})

const filteredData = computed(() => {
  return tableData.value.filter((item) => {
    const materialNo = query.materialNo.trim()
    const materialName = query.materialName.trim()
    const supplier = query.supplier.trim()
    const matchMaterialNo = !materialNo || item.materialNo.includes(materialNo)
    const matchMaterialName = !materialName || item.materialName.includes(materialName)
    const matchSupplier = !supplier || item.supplierName === supplier
    return matchMaterialNo && matchMaterialName && matchSupplier
  })
})

const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

onMounted(async () => {
  await Promise.all([fetchSuppliers(), fetchMaterials()])
})

async function fetchSuppliers() {
  const { data } = await getSuppliersApi()
  suppliers.value = data || []
}

async function fetchMaterials() {
  loading.value = true
  try {
    const { data } = await getMaterialsApi()
    tableData.value = data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.materialNo = ''
  form.materialName = ''
  form.materialType = ''
  form.unit = ''
  form.supplierCode = ''
  form.supplierName = ''
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.id = row.id
  form.materialNo = row.materialNo
  form.materialName = row.materialName
  form.materialType = row.materialType
  form.unit = row.unit
  form.supplierCode = row.supplierCode
  form.supplierName = row.supplierName
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateMaterialApi(form.id, {
        materialName: form.materialName,
        materialType: form.materialType,
        unit: form.unit,
        supplierCode: form.supplierCode,
        supplierName: form.supplierName
      })
      ElMessage.success('物料更新成功')
    } else {
      await createMaterialApi({
        materialNo: form.materialNo,
        materialName: form.materialName,
        materialType: form.materialType,
        unit: form.unit,
        supplierCode: form.supplierCode,
        supplierName: form.supplierName
      })
      ElMessage.success('物料创建成功')
    }
    dialogVisible.value = false
    await fetchMaterials()
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除物料「${row.materialName}」吗？`, '确认删除', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteMaterialApi(row.id)
      ElMessage.success('删除成功')
      await fetchMaterials()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}

function handleSearch() {
  page.value = 1
}

function handleReset() {
  query.materialNo = ''
  query.materialName = ''
  query.supplier = ''
  page.value = 1
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
