<template>
  <PageContainer title="库区管理">
    <template #actions>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增库区
      </el-button>
    </template>

    <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="areaCode" label="库区代码" width="130" />
      <el-table-column prop="areaName" label="库区名称" min-width="160" />
      <el-table-column prop="sortOrder" label="排序号" width="80" />
      <el-table-column prop="description" label="描述说明" min-width="200" />
      <el-table-column label="操作" width="160" fixed="right">
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
      :title="isEdit ? '编辑库区' : '新增库区'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="库区代码" prop="areaCode" v-if="!isEdit">
          <el-input v-model="form.areaCode" placeholder="请输入库区代码" maxlength="50" />
        </el-form-item>
        <el-form-item label="库区名称" prop="areaName">
          <el-input v-model="form.areaName" placeholder="请输入库区名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="描述说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选" maxlength="255" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { getWarehouseAreasApi, createWarehouseAreaApi, updateWarehouseAreaApi, deleteWarehouseAreaApi } from '@/api/basic'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  areaCode: '',
  areaName: '',
  sortOrder: 0,
  description: ''
})

const rules = {
  areaCode: [{ required: true, message: '请输入库区代码', trigger: 'blur' }],
  areaName: [{ required: true, message: '请输入库区名称', trigger: 'blur' }]
}

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getWarehouseAreasApi()
    tableData.value = data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = null
  form.areaCode = ''
  form.areaName = ''
  form.sortOrder = 0
  form.description = ''
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.id = row.id
  form.areaCode = row.areaCode
  form.areaName = row.areaName
  form.sortOrder = row.sortOrder
  form.description = row.description
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateWarehouseAreaApi(form.id, {
        areaName: form.areaName,
        sortOrder: form.sortOrder,
        description: form.description
      })
      ElMessage.success('库区更新成功')
    } else {
      await createWarehouseAreaApi({
        areaCode: form.areaCode,
        areaName: form.areaName,
        sortOrder: form.sortOrder,
        description: form.description
      })
      ElMessage.success('库区创建成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除库区「${row.areaName}」吗？`, '确认删除', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteWarehouseAreaApi(row.id)
      ElMessage.success('删除成功')
      await fetchData()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}
</script>
