<template>
  <el-dialog
    :model-value="visible"
    title="创建入库单"
    width="1200px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="供应商" prop="supplierCode" required>
            <el-select
              v-model="form.supplierCode"
              placeholder="请先选择供应商"
              filterable
              clearable
              style="width: 100%"
              @change="handleSupplierChange"
            >
              <el-option
                v-for="supplier in supplierOptions"
                :key="supplier.supplierCode"
                :label="`${supplier.supplierCode} / ${supplier.supplierName}`"
                :value="supplier.supplierCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="转包状态" prop="transferStatus">
            <el-radio-group v-model="form.transferStatus">
              <el-radio value="不转包">不转包</el-radio>
              <el-radio value="转包">转包</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

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
        <div>
          <el-button type="primary" link @click="addDetail">
            <el-icon><Plus /></el-icon>新增明细
          </el-button>
          <el-button type="primary" link @click="triggerExcelUpload" :disabled="!form.supplierCode">
            <el-icon><Upload /></el-icon>Excel导入
          </el-button>
        </div>
      </div>

      <input
        ref="fileInputRef"
        type="file"
        accept=".xlsx,.xls"
        style="display: none"
        @change="handleExcelUpload"
      />

      <el-table :data="form.details" border class="detail-table">
        <el-table-column type="index" label="行号" width="60" />
        <el-table-column label="物料号" min-width="200">
          <template #default="{ row }">
            <el-select
              v-model="row.materialCode"
              placeholder="搜索并选择物料"
              filterable
              clearable
              style="width: 100%"
              :disabled="!form.supplierCode"
              :filter-method="(query) => filterMaterialOptions(row, query)"
              @change="(value) => handleMaterialChange(row, value)"
              @visible-change="(visible) => { if (visible && row.materialCode) row._filterText = '' }"
            >
              <el-option
                v-for="material in row._filteredOptions"
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
        <el-table-column label="计划数量" width="130">
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
      <el-button type="primary" :loading="submitting" :disabled="!form.supplierCode" @click="handleSubmit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getMaterialsApi, getSuppliersApi, getWarehouseAreasApi } from '@/api/basic'
import * as XLSX from 'xlsx'

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
const fileInputRef = ref()
const form = reactive(createDefaultForm())
const supplierOptions = ref([])
const warehouseAreaOptions = ref([])
const allMaterials = ref([])

const rules = {
  supplierCode: [{ required: true, message: '请选择供应商', trigger: 'change' }]
}

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
    materialCode: '',
    materialName: '',
    packageModel: '',
    packagingCapacity: 0,
    plannedQty: 1,
    packageCount: 1,
    warehouseArea: '默认库区',
    remark: '',
    _filterText: '',
    _filteredOptions: []
  }
}

function createDefaultForm() {
  return {
    supplierCode: '',
    supplierName: '',
    remark: '',
    transferStatus: '不转包',
    details: [createDefaultDetail()]
  }
}

function resetForm() {
  const next = createDefaultForm()
  form.supplierCode = next.supplierCode
  form.supplierName = next.supplierName
  form.remark = next.remark
  form.transferStatus = next.transferStatus
  form.details.splice(0, form.details.length, ...next.details)
  allMaterials.value = []
  formRef.value?.clearValidate?.()
}

function addDetail() {
  const next = createDefaultDetail()
  next._filteredOptions = allMaterials.value
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

  const materialKeys = new Set()
  for (const item of form.details) {
    if (!item.materialCode?.trim()) {
      ElMessage.warning('请选择物料')
      return false
    }
    if (!item.plannedQty || item.plannedQty < 1) {
      ElMessage.warning('计划数量必须大于 0')
      return false
    }
    const key = `${form.supplierCode}::${item.materialCode.trim()}`
    if (materialKeys.has(key)) {
      ElMessage.warning('同一张入库单中不允许重复选择同一物料')
      return false
    }
    materialKeys.add(key)
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

async function handleSupplierChange(supplierCode) {
  const selectedSupplier = supplierOptions.value.find((item) => item.supplierCode === supplierCode)
  form.supplierName = selectedSupplier?.supplierName || ''

  // Reset all details when supplier changes
  const firstDetail = createDefaultDetail()
  form.details.splice(0, form.details.length, firstDetail)

  if (!selectedSupplier) {
    allMaterials.value = []
    return
  }

  const { data } = await getMaterialsApi({ supplierCode: selectedSupplier.supplierCode })
  allMaterials.value = data || []
  firstDetail._filteredOptions = allMaterials.value
}

function filterMaterialOptions(row, query) {
  row._filterText = query || ''
  if (!query) {
    row._filteredOptions = allMaterials.value
  } else {
    const lower = query.toLowerCase()
    row._filteredOptions = allMaterials.value.filter(
      (m) => m.materialNo.toLowerCase().includes(lower) || m.materialName.toLowerCase().includes(lower)
    )
  }
}

function handleMaterialChange(row, materialNo) {
  const material = allMaterials.value.find((item) => item.materialNo === materialNo)
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
  row.warehouseArea = '默认库区'
  updatePackageCount(row)
}

function triggerExcelUpload() {
  fileInputRef.value?.click()
}

function handleExcelUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const workbook = XLSX.read(e.target.result, { type: 'array' })
      const sheetName = workbook.SheetNames[0]
      const sheet = workbook.Sheets[sheetName]
      const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 })

      if (rows.length < 2) {
        ElMessage.warning('Excel 文件至少需要一行数据（表头+数据）')
        return
      }

      // Auto-detect header row and columns
      const headerRow = rows[0]
      const colMap = detectColumns(headerRow)

      if (!colMap.materialCode) {
        ElMessage.warning('未识别到物料号列，请确保表头包含"物料号"或"零件号"')
        return
      }

      const details = []
      const seenMaterials = new Set()

      for (let i = 1; i < rows.length; i++) {
        const row = rows[i]
        if (!row || row.every((cell) => cell === undefined || cell === null || cell === '')) continue

        const materialCode = String(row[colMap.materialCode] ?? '').trim()
        if (!materialCode) {
          ElMessage.warning(`第 ${i + 1} 行物料号为空，已跳过`)
          continue
        }

        if (seenMaterials.has(materialCode)) {
          ElMessage.warning(`物料号"${materialCode}"在第 ${i + 1} 行重复，已跳过`)
          continue
        }
        seenMaterials.add(materialCode)

        const plannedQty = colMap.qty !== undefined ? Number(row[colMap.qty]) || 1 : 1
        const warehouseArea = colMap.area !== undefined ? String(row[colMap.area] || '').trim() : ''
        const remark = colMap.remark !== undefined ? String(row[colMap.remark] || '').trim() : ''

        // Find matching material in loaded options
        const material = allMaterials.value.find(
          (m) => m.materialNo === materialCode || m.materialName === materialCode
        )

        details.push({
          materialCode: materialCode,
          materialName: material?.materialName || '',
          packageModel: material?.packageModel || '',
          packagingCapacity: material?.packageCapacity ?? 0,
          plannedQty: Math.max(1, plannedQty),
          packageCount: calculatePackageCount(Math.max(1, plannedQty), material?.packageCapacity ?? 0),
          warehouseArea: warehouseArea || '默认库区',
          remark,
          _filterText: '',
          _filteredOptions: allMaterials.value
        })
      }

      if (details.length === 0) {
        ElMessage.warning('未从 Excel 中解析到有效数据')
        return
      }

      form.details.splice(0, form.details.length, ...details)
      ElMessage.success(`成功导入 ${details.length} 条物料明细`)
    } catch (err) {
      ElMessage.error('Excel 文件解析失败：' + (err.message || '未知错误'))
    }
  }
  reader.readAsArrayBuffer(file)

  // Reset file input so the same file can be re-uploaded
  event.target.value = ''
}

function detectColumns(headerRow) {
  const map = {}
  for (let i = 0; i < headerRow.length; i++) {
    const header = String(headerRow[i] ?? '').trim()
    if (/物料号|零件号|物料编码|编码/.test(header)) {
      map.materialCode = i
    } else if (/数量|计划数量|入库数量|个数/.test(header)) {
      map.qty = i
    } else if (/库区|仓库区域/.test(header)) {
      map.area = i
    } else if (/备注|说明/.test(header)) {
      map.remark = i
    }
  }
  return map
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
      supplier: form.supplierName || '未知供应商',
      transferStatus: form.transferStatus || '不转包',
      remark: form.remark?.trim() || '',
      details: form.details.map((item) => ({
        supplierCode: form.supplierCode,
        supplierName: form.supplierName,
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
