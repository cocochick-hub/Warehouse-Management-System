<template>
  <el-dialog
    :model-value="visible"
    title="创建入库单"
    width="880px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="供应商" prop="supplier">
        <el-input v-model="form.supplier" placeholder="请输入供应商名称" maxlength="100" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="可选，填写单据备注"
          maxlength="255"
          show-word-limit
        />
      </el-form-item>

      <div class="section-head">
        <span>入库明细</span>
        <el-button type="primary" link @click="addDetail">
          <el-icon><Plus /></el-icon>新增明细
        </el-button>
      </div>

      <el-table :data="form.details" border class="detail-table">
        <el-table-column type="index" label="行号" width="60" />
        <el-table-column label="物料号" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.materialCode" placeholder="请输入物料号" maxlength="50" />
          </template>
        </el-table-column>
        <el-table-column label="物料名称" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.materialName" placeholder="请输入物料名称" maxlength="100" />
          </template>
        </el-table-column>
        <el-table-column label="包装容量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.packagingCapacity" :min="0" :step="1" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="计划数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.plannedQty" :min="1" :step="1" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="可选" maxlength="255" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeDetail($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref()
const form = reactive(createDefaultForm())

const rules = {
  supplier: [
    { required: true, message: '请输入供应商名称', trigger: 'blur' }
  ]
}

watch(
  () => props.visible,
  (value) => {
    if (value) {
      resetForm()
    }
  }
)

function createDefaultDetail() {
  return {
    materialCode: '',
    materialName: '',
    packagingCapacity: 0,
    plannedQty: 1,
    remark: ''
  }
}

function createDefaultForm() {
  return {
    supplier: '',
    remark: '',
    details: [createDefaultDetail()]
  }
}

function resetForm() {
  const next = createDefaultForm()
  form.supplier = next.supplier
  form.remark = next.remark
  form.details.splice(0, form.details.length, ...next.details)
  formRef.value?.clearValidate?.()
}

function addDetail() {
  form.details.push(createDefaultDetail())
}

function removeDetail(index) {
  if (form.details.length === 1) {
    ElMessage.warning('至少保留一条明细')
    return
  }
  form.details.splice(index, 1)
}

function validateDetails() {
  if (!form.details.length) {
    ElMessage.warning('请至少填写一条入库明细')
    return false
  }

  const materialCodes = new Set()
  for (const item of form.details) {
    if (!item.materialCode?.trim()) {
      ElMessage.warning('请填写物料号')
      return false
    }
    if (!item.materialName?.trim()) {
      ElMessage.warning('请填写物料名称')
      return false
    }
    if (!item.plannedQty || item.plannedQty < 1) {
      ElMessage.warning('计划数量必须大于 0')
      return false
    }

    const code = item.materialCode.trim()
    if (materialCodes.has(code)) {
      ElMessage.warning('同一张入库单中不允许重复填写同一物料号')
      return false
    }
    materialCodes.add(code)
  }

  return true
}

function handleClose() {
  emit('update:visible', false)
}

function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (!valid || !validateDetails()) {
      return
    }

    emit('submit', {
      supplier: form.supplier.trim(),
      remark: form.remark?.trim() || '',
      details: form.details.map((item) => ({
        materialCode: item.materialCode.trim(),
        materialName: item.materialName.trim(),
        packagingCapacity: item.packagingCapacity ?? 0,
        plannedQty: item.plannedQty,
        remark: item.remark?.trim() || ''
      }))
    })
  })
}
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}

.detail-table {
  margin-bottom: 8px;
}
</style>
