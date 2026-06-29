<template>
  <PageContainer title="供应商管理">
    <template #actions>
      <el-button v-if="canEdit" type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增供应商
      </el-button>
    </template>

    <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="supplierCode" label="供应商代码" width="130" />
      <el-table-column prop="supplierName" label="供应商名称" min-width="180" />
      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系方式" width="150" />
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
      :title="isEdit ? '编辑供应商' : '新增供应商'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="供应商代码" prop="supplierCode" v-if="!isEdit">
          <el-input v-model="form.supplierCode" placeholder="请输入供应商代码" maxlength="50" />
        </el-form-item>
        <el-form-item label="供应商名称" prop="supplierName">
          <el-input v-model="form.supplierName" placeholder="请输入供应商名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系人" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系方式" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系方式" maxlength="30" />
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
import { getSuppliersApi, createSupplierApi, updateSupplierApi, deleteSupplierApi } from '@/api/basic'
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
  supplierCode: '',
  supplierName: '',
  contact: '',
  phone: ''
})

const rules = {
  supplierCode: [{ required: true, message: '请输入供应商代码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }]
}

onMounted(() => {
  fetchSuppliers()
})

async function fetchSuppliers() {
  loading.value = true
  try {
    const { data } = await getSuppliersApi()
    tableData.value = data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.supplierCode = ''
  form.supplierName = ''
  form.contact = ''
  form.phone = ''
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.id = row.id
  form.supplierCode = row.supplierCode
  form.supplierName = row.supplierName
  form.contact = row.contact
  form.phone = row.phone
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateSupplierApi(form.id, {
        supplierName: form.supplierName,
        contact: form.contact,
        phone: form.phone
      })
      ElMessage.success('供应商更新成功')
    } else {
      await createSupplierApi({
        supplierCode: form.supplierCode,
        supplierName: form.supplierName,
        contact: form.contact,
        phone: form.phone
      })
      ElMessage.success('供应商创建成功')
    }
    dialogVisible.value = false
    await fetchSuppliers()
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除供应商「${row.supplierName}」吗？`, '确认删除', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteSupplierApi(row.id)
      ElMessage.success('删除成功')
      await fetchSuppliers()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}
</script>
