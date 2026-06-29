<template>
  <PageContainer title="包装管理">
    <template #actions>
      <el-button v-if="canEdit" type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增包装
      </el-button>
    </template>

    <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="materialNo" label="物料号" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="supplier" label="供应商" width="160" />
      <el-table-column prop="packageModel" label="包装型号" width="130" />
      <el-table-column prop="packageCapacity" label="包装容量" width="100" />
      <el-table-column v-if="canEdit" label="操作" width="160" fixed="right">
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑包装' : '新增包装'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="物料号" prop="materialNo" v-if="!isEdit">
          <el-input v-model="form.materialNo" placeholder="请输入物料号" maxlength="50" />
        </el-form-item>
        <el-form-item label="供应商代码" prop="supplierCode" v-if="!isEdit">
          <el-input v-model="form.supplierCode" placeholder="请输入供应商代码" maxlength="50" />
        </el-form-item>
        <el-form-item label="包装型号" prop="packageModel">
          <el-input v-model="form.packageModel" placeholder="请输入包装型号" maxlength="50" />
        </el-form-item>
        <el-form-item label="包装容量" prop="packageCapacity">
          <el-input-number v-model="form.packageCapacity" :min="1" :step="1" controls-position="right" />
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
import { getMaterialsApi, getPackagingApi, getSuppliersApi, createPackagingApi, updatePackagingApi, deletePackagingApi } from '@/api/basic'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const canEdit = computed(() => ['admin', 'manager'].includes(userStore.role))

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  materialNo: '',
  supplierCode: '',
  packageModel: '',
  packageCapacity: 1
})

const rules = {
  materialNo: [{ required: true, message: '请输入物料号', trigger: 'blur' }],
  supplierCode: [{ required: true, message: '请输入供应商代码', trigger: 'blur' }],
  packageModel: [{ required: true, message: '请输入包装型号', trigger: 'blur' }],
  packageCapacity: [{ required: true, message: '请输入包装容量', trigger: 'blur' }]
}

onMounted(() => {
  fetchPackaging()
})

async function fetchPackaging() {
  loading.value = true
  try {
    const [{ data: packagingList }, { data: materialList }, { data: supplierList }] = await Promise.all([
      getPackagingApi(),
      getMaterialsApi(),
      getSuppliersApi()
    ])

    const materialMap = new Map((materialList || []).map((item) => [item.materialNo, item]))
    const supplierMap = new Map((supplierList || []).map((item) => [item.supplierCode, item]))

    tableData.value = (packagingList || []).map((item) => ({
      ...item,
      materialName: materialMap.get(item.materialNo)?.materialName || '',
      supplier: supplierMap.get(item.supplierCode)?.supplierName || ''
    }))
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.materialNo = ''
  form.supplierCode = ''
  form.packageModel = ''
  form.packageCapacity = 1
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
  form.supplierCode = row.supplierCode
  form.packageModel = row.packageModel
  form.packageCapacity = row.packageCapacity
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updatePackagingApi(form.id, {
        packageModel: form.packageModel,
        packageCapacity: form.packageCapacity
      })
      ElMessage.success('包装更新成功')
    } else {
      await createPackagingApi({
        materialNo: form.materialNo,
        supplierCode: form.supplierCode,
        packageModel: form.packageModel,
        packageCapacity: form.packageCapacity
      })
      ElMessage.success('包装创建成功')
    }
    dialogVisible.value = false
    await fetchPackaging()
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除包装「${row.materialNo}」吗？`, '确认删除', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deletePackagingApi(row.id)
      ElMessage.success('删除成功')
      await fetchPackaging()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}
</script>
