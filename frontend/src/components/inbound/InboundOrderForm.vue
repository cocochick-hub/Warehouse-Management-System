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
        <el-select
          v-model="form.supplier"
          placeholder="请选择供应商"
          filterable
          clearable
          style="width: 100%"
          @change="handleSupplierChange"
        >
          <el-option
            v-for="supplier in supplierOptions"
            :key="supplier.supplierCode"
            :label="supplier.supplierName"
            :value="supplier.supplierName"
          />
        </el-select>
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
        <el-table-column label="物料选择" min-width="220">
          <template #default="{ row }">
            <el-select
              v-model="row.materialCode"
              placeholder="请选择物料"
              filterable
              clearable
              style="width: 100%"
              :disabled="!form.supplier"
              @change="(value) => handleMaterialChange(row, value)"
            >
              <el-option
                v-for="material in availableMaterials"
                :key="material.materialNo"
                :label="`${material.materialNo} / ${material.materialName}`"
                :value="material.materialNo"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="物料名称" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.materialName" placeholder="自动带出" maxlength="100" readonly />
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
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getMaterialsApi, getSuppliersApi } from '@/api/basic'

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
const supplierOptions = ref([])
const materialOptions = ref([])
const availableMaterials = computed(() => {
  return materialOptions.value
})

const rules = {
  supplier: [
    { required: true, message: '请选择供应商', trigger: 'change' }
  ]
}

watch(
  () => props.visible,
  async (value) => {
    if (value) {
      resetForm()
      await fetchSuppliers()
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

async function fetchSuppliers() {
  const { data } = await getSuppliersApi()
  supplierOptions.value = data || []
}

async function handleSupplierChange() {
  form.details.splice(0, form.details.length, createDefaultDetail())
  materialOptions.value = []

  const selectedSupplier = supplierOptions.value.find((item) => item.supplierName === form.supplier)
  if (!selectedSupplier) {
    return
  }

  const { data } = await getMaterialsApi({ supplierCode: selectedSupplier.supplierCode })
  materialOptions.value = data || []
}

function handleMaterialChange(row, materialNo) {
  const material = materialOptions.value.find((item) => item.materialNo === materialNo)
  if (!material) {
    row.materialCode = ''
    row.materialName = ''
    row.packagingCapacity = 0
    return
  }
  row.materialCode = material.materialNo
  row.materialName = material.materialName
  row.packagingCapacity = material.packageCapacity ?? 0
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
