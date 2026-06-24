<template>
  <el-dialog
    :model-value="visible"
    title="创建入库单"
    width="1200px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
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

      <el-form-item label="转包状态" prop="transferStatus">
        <el-radio-group v-model="form.transferStatus">
          <el-radio value="不转包">不转包</el-radio>
          <el-radio value="转包">转包</el-radio>
        </el-radio-group>
      </el-form-item>

      <div class="section-head">
        <span>入库明细</span>
        <el-button type="primary" link @click="addDetail">
          <el-icon><Plus /></el-icon>新增明细
        </el-button>
      </div>

      <el-table :data="form.details" border class="detail-table">
        <el-table-column type="index" label="行号" width="60" />
        <el-table-column label="供应商" min-width="180">
          <template #default="{ row, $index }">
            <el-select
              v-model="row.supplierCode"
              placeholder="选择供应商"
              filterable
              clearable
              style="width: 100%"
              :disabled="$index > 0 && form.details[0].supplierCode"
              @change="(value) => handleRowSupplierChange(row, value, $index)"
            >
              <el-option
                v-for="supplier in supplierOptions"
                :key="supplier.supplierCode"
                :label="supplier.supplierName"
                :value="supplier.supplierCode"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="物料选择" min-width="220">
          <template #default="{ row }">
            <el-select
              v-model="row.materialCode"
              placeholder="请选择物料"
              filterable
              clearable
              style="width: 100%"
              :disabled="!row.supplierCode"
              @change="(value) => handleMaterialChange(row, value)"
            >
              <el-option
                v-for="material in row.materialOptions"
                :key="material.materialNo"
                :label="`${material.materialNo} / ${material.materialName}`"
                :value="material.materialNo"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="物料名称" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.materialName" placeholder="自动带出" maxlength="100" readonly />
          </template>
        </el-table-column>
        <el-table-column label="包装容量" width="110">
          <template #default="{ row }">
            <span :class="{ 'no-capacity': !row.packagingCapacity }">
              {{ row.packagingCapacity ? row.packagingCapacity + ' 个/箱' : '未配置' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="计划数量" width="110">
          <template #default="{ row }">
            <el-input-number
              v-model="row.plannedQty"
              :min="1"
              :step="1"
              controls-position="right"
              @change="() => updatePackageCount(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="箱数" width="90">
          <template #default="{ row }">
            <div class="package-note">{{ boxCountText(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="库区" width="130">
          <template #default="{ row }">
            <el-select v-model="row.warehouseArea" placeholder="库区" style="width: 100%">
              <el-option
                v-for="area in warehouseAreaOptions"
                :key="area.areaCode"
                :label="area.areaName"
                :value="area.areaName"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
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
import { getMaterialsApi, getSuppliersApi, getWarehouseAreasApi } from '@/api/basic'

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
const warehouseAreaOptions = ref([])

const rules = {}

watch(
  () => props.visible,
  async (value) => {
    if (value) {
      resetForm()
      await Promise.all([fetchSuppliers(), fetchWarehouseAreas()])
    }
  }
)

function createDefaultDetail() {
  return {
    supplierCode: '',
    supplierName: '',
    materialCode: '',
    materialName: '',
    packageModel: '',
    packagingCapacity: 0,
    plannedQty: 1,
    packageCount: 1,
    warehouseArea: '默认库区',
    materialOptions: [],
    remark: ''
  }
}

function createDefaultForm() {
  return {
    remark: '',
    transferStatus: '不转包',
    details: [createDefaultDetail()]
  }
}

function resetForm() {
  const next = createDefaultForm()
  form.remark = next.remark
  form.transferStatus = next.transferStatus
  form.details.splice(0, form.details.length, ...next.details)
  formRef.value?.clearValidate?.()
}

function addDetail() {
  const previous = form.details[form.details.length - 1]
  const next = createDefaultDetail()
  if (previous?.supplierCode) {
    next.supplierCode = previous.supplierCode
    next.supplierName = previous.supplierName
    next.materialOptions = previous.materialOptions || []
  }
  form.details.push(next)
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

  // Single supplier check: all details must have the same supplier code
  const supplierCodes = new Set()
  const materialKeys = new Set()
  for (const item of form.details) {
    if (!item.supplierCode?.trim()) {
      ElMessage.warning('请选择供应商')
      return false
    }
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

    supplierCodes.add(item.supplierCode.trim())

    const key = `${item.supplierCode.trim()}::${item.materialCode.trim()}`
    if (materialKeys.has(key)) {
      ElMessage.warning('同一张入库单中不允许重复选择同一供应商下的同一物料')
      return false
    }
    materialKeys.add(key)
  }

  if (supplierCodes.size > 1) {
    ElMessage.warning('一张入库单只能包含同一个供应商的物料，请分批创建')
    return false
  }

  return true
}

async function fetchSuppliers() {
  const { data } = await getSuppliersApi()
  supplierOptions.value = data || []
}

async function fetchWarehouseAreas() {
  const { data } = await getWarehouseAreasApi()
  warehouseAreaOptions.value = data || []
}

async function handleRowSupplierChange(row, supplierCode, index) {
  const selectedSupplier = supplierOptions.value.find((item) => item.supplierCode === supplierCode)
  row.supplierName = selectedSupplier?.supplierName || ''
  row.materialCode = ''
  row.materialName = ''
  row.packageModel = ''
  row.packagingCapacity = 0
  row.packageCount = 1
  row.materialOptions = []
  if (!selectedSupplier) {
    return
  }

  const { data } = await getMaterialsApi({ supplierCode: selectedSupplier.supplierCode })
  row.materialOptions = data || []

  // For single-supplier enforcement: if first row's supplier changes, update all other rows
  if (index === 0) {
    for (let i = 1; i < form.details.length; i++) {
      const otherRow = form.details[i]
      otherRow.supplierCode = supplierCode
      otherRow.supplierName = selectedSupplier.supplierName
      otherRow.materialCode = ''
      otherRow.materialName = ''
      otherRow.packageModel = ''
      otherRow.packagingCapacity = 0
      otherRow.packageCount = 1
      otherRow.materialOptions = data || []
    }
  }
}

function handleMaterialChange(row, materialNo) {
  const material = (row.materialOptions || []).find((item) => item.materialNo === materialNo)
  if (!material) {
    row.materialCode = ''
    row.materialName = ''
    row.packageModel = ''
    row.packagingCapacity = 0
    row.packageCount = 1
    return
  }
  row.materialCode = material.materialNo
  row.materialName = material.materialName
  row.packageModel = material.packageModel || ''
  row.packagingCapacity = material.packageCapacity ?? 0
  updatePackageCount(row)
}

function handleClose() {
  emit('update:visible', false)
}

function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (!valid || !validateDetails()) {
      return
    }

    const supplierNames = Array.from(new Set(form.details.map((item) => item.supplierName).filter(Boolean)))
    emit('submit', {
      supplier: supplierNames.length === 1 ? supplierNames[0] : (supplierNames[0] || '未知供应商'),
      transferStatus: form.transferStatus || '不转包',
      remark: form.remark?.trim() || '',
      details: form.details.map((item) => ({
        supplierCode: item.supplierCode.trim(),
        supplierName: item.supplierName.trim(),
        materialCode: item.materialCode.trim(),
        materialName: item.materialName.trim(),
        packageModel: item.packageModel?.trim() || '',
        packagingCapacity: item.packagingCapacity ?? 0,
        plannedQty: item.plannedQty,
        packageCount: calculatePackageCount(item.plannedQty, item.packagingCapacity),
        warehouseArea: item.warehouseArea?.trim() || '默认库区',
        transferStatus: form.transferStatus || '不转包',
        remark: item.remark?.trim() || ''
      }))
    })
  })
}

function updatePackageCount(row) {
  row.packageCount = calculatePackageCount(row.plannedQty, row.packagingCapacity)
}

function calculatePackageCount(plannedQty, packagingCapacity) {
  const planned = Number(plannedQty) || 0
  const capacity = Number(packagingCapacity) || 0
  if (planned <= 0 || capacity <= 0) {
    return 1
  }
  return Math.ceil(planned / capacity)
}

function calculateBoxCount(qty, packagingCapacity) {
  const quantity = Number(qty) || 0
  const capacity = Number(packagingCapacity) || 0
  if (quantity <= 0 || capacity <= 0) {
    return 0
  }
  return Math.round((quantity * 10) / capacity) / 10
}

function boxCountText(row) {
  const capacity = Number(row.packagingCapacity) || 0
  if (capacity <= 0) {
    return '未配置'
  }
  return calculateBoxCount(row.plannedQty, row.packagingCapacity)
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

.package-note {
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.no-capacity {
  color: #999;
  font-style: italic;
}
</style>
