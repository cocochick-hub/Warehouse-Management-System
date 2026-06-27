<template>
  <el-dialog
    :model-value="visible"
    title="创建出库单"
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
              placeholder="请选择供应商"
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
        </el-col>
      </el-row>

      <!-- 可选零件列表 -->
      <div v-if="form.supplierCode" class="section-head">
        <span>可选零件</span>
        <el-button type="primary" link @click="triggerExcelUpload">
          <el-icon><Upload /></el-icon>Excel导入
        </el-button>
      </div>

      <input
        ref="fileInputRef"
        type="file"
        accept=".xlsx,.xls"
        style="display: none"
        @change="handleExcelUpload"
      />

      <div v-if="form.supplierCode" class="material-picker">
        <el-input
          v-model="materialSearch"
          placeholder="搜索零件（物料号/名称）"
          clearable
          class="material-search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="material-list">
          <div
            v-for="item in filteredMaterials"
            :key="`${item._supplierCode}-${item.materialNo}`"
            class="material-card"
            :class="{ added: isMaterialAdded(item.materialNo, item._supplierCode) }"
            @click="addMaterialToDetails(item)"
          >
            <div class="card-left">
              <span class="card-code">{{ item.materialNo }}</span>
              <span class="card-divider">|</span>
              <span class="card-name">{{ item.materialName }}</span>
            </div>
            <div class="card-right">
              <el-tag size="small" type="warning">{{ item._supplierName }}</el-tag>
              <el-tag size="small" :type="item.packageCapacity > 0 ? 'success' : 'info'">
                {{ item.packageCapacity > 0 ? item.packageCapacity + '个/箱' : '未配置包装' }}
              </el-tag>
              <el-button
                v-if="isMaterialAdded(item.materialNo, item._supplierCode)"
                size="small"
                type="danger"
                plain
                @click.stop="removeMaterialFromDetails(item.materialNo, item._supplierCode)"
              >
                移除
              </el-button>
              <el-button
                v-else
                size="small"
                type="primary"
                @click.stop="addMaterialToDetails(item)"
              >
                <el-icon><Plus /></el-icon>添加
              </el-button>
            </div>
          </div>
          <el-empty v-if="filteredMaterials.length === 0" description="暂无匹配的零件" :image-size="60" />
        </div>
      </div>

      <!-- 空状态提示 -->
      <div v-else class="select-supplier-hint">
        <el-icon :size="48"><FolderOpened /></el-icon>
        <p>请先选择供应商，然后从零件列表中点击添加</p>
      </div>

      <!-- 出库明细 -->
      <div v-if="form.details.length > 0" class="section-head">
        <span>出库明细</span>
      </div>

      <el-table v-if="form.details.length > 0" :data="form.details" border class="detail-table">
        <el-table-column type="index" label="行号" width="60" />
        <el-table-column label="供应商" width="140">
          <template #default="{ row }">
            <span>{{ row.supplierName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="物料号" width="180">
          <template #default="{ row }">
            <span class="detail-material-code">{{ row.materialCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="物料名称" min-width="150">
          <template #default="{ row }">
            <span>{{ row.materialName }}</span>
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
            <span class="package-note">{{ boxCountText(row) }}</span>
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
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeDetail($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="form.details.length === 0" @click="handleSubmit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getMaterialsApi, getSuppliersApi, getWarehouseAreasApi } from '@/api/basic'
import * as XLSX from 'xlsx'

const props = defineProps({
  visible: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref()
const fileInputRef = ref()
const form = reactive(createDefaultForm())
const supplierOptions = ref([])
const warehouseAreaOptions = ref([])
const allMaterials = ref([])
const materialSearch = ref('')

const rules = {
  supplierCode: [{ required: true, message: '请选择供应商', trigger: 'change' }]
}

const filteredMaterials = computed(() => {
  const query = materialSearch.value.trim().toLowerCase()
  if (!query) return allMaterials.value
  return allMaterials.value.filter(
    (m) => m.materialNo.toLowerCase().includes(query) || m.materialName.toLowerCase().includes(query)
  )
})

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
    remark: ''
  }
}

function createDefaultForm() {
  return {
    supplierCode: '',
    remark: '',
    details: []
  }
}

function resetForm() {
  form.supplierCode = ''
  form.remark = ''
  form.details.splice(0, form.details.length)
  allMaterials.value = []
  materialSearch.value = ''
  formRef.value?.clearValidate?.()
}

function isMaterialAdded(materialCode, supplierCode) {
  return form.details.some(
    (d) => d.materialCode === materialCode && d.supplierCode === supplierCode
  )
}

function addMaterialToDetails(item) {
  if (isMaterialAdded(item.materialNo, item._supplierCode)) return

  form.details.push({
    supplierCode: item._supplierCode,
    supplierName: item._supplierName,
    materialCode: item.materialNo,
    materialName: item.materialName,
    packageModel: item.packageModel || '',
    packagingCapacity: item.packageCapacity ?? 0,
    plannedQty: 1,
    packageCount: calculatePackageCount(1, item.packageCapacity ?? 0),
    warehouseArea: '默认库区',
    remark: ''
  })
}

function removeMaterialFromDetails(materialCode, supplierCode) {
  const idx = form.details.findIndex(
    (d) => d.materialCode === materialCode && d.supplierCode === supplierCode
  )
  if (idx !== -1) form.details.splice(idx, 1)
}

function removeDetail(index) {
  form.details.splice(index, 1)
}

async function fetchSuppliers() {
  const { data } = await getSuppliersApi()
  supplierOptions.value = data || []
}

async function fetchWarehouseAreas() {
  const { data } = await getWarehouseAreasApi()
  warehouseAreaOptions.value = data || []
}

async function handleSupplierChange(code) {
  materialSearch.value = ''

  if (!code) {
    allMaterials.value = []
    return
  }

  const supplier = supplierOptions.value.find((s) => s.supplierCode === code)
  if (!supplier) {
    allMaterials.value = []
    return
  }

  try {
    const { data } = await getMaterialsApi({ supplierCode: code })
    allMaterials.value = (data || []).map((m) => ({
      ...m,
      _supplierCode: supplier.supplierCode,
      _supplierName: supplier.supplierName
    }))
  } catch {
    allMaterials.value = []
  }
}

// ── Excel import ──

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
      const sheet = workbook.Sheets[workbook.SheetNames[0]]
      const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 })

      if (rows.length < 2) {
        ElMessage.warning('Excel 文件至少需要一行数据（表头+数据）')
        return
      }

      const colMap = detectColumns(rows[0])
      if (!colMap.materialCode) {
        ElMessage.warning('未识别到物料号列，请确保表头包含"物料号"或"零件号"')
        return
      }

      const currentSupplierCode = form.supplierCode
      const currentSupplier = supplierOptions.value.find((s) => s.supplierCode === currentSupplierCode)
      const currentSupplierName = currentSupplier?.supplierName || ''

      let addedCount = 0
      for (let i = 1; i < rows.length; i++) {
        const row = rows[i]
        if (!row || row.every((c) => c === undefined || c === null || c === '')) continue

        const materialCode = String(row[colMap.materialCode] ?? '').trim()
        if (!materialCode) continue

        const supplierCode = colMap.supplierCode !== undefined
          ? String(row[colMap.supplierCode] ?? '').trim()
          : currentSupplierCode
        const supplierName = colMap.supplierCode !== undefined
          ? (supplierOptions.value.find((s) => s.supplierCode === supplierCode)?.supplierName || supplierCode)
          : currentSupplierName

        if (!supplierCode) continue

        if (isMaterialAdded(materialCode, supplierCode)) continue

        const material = allMaterials.value.find(
          (m) => m.materialNo === materialCode && m._supplierCode === supplierCode
        )
        if (!material) {
          ElMessage.warning(`物料"${materialCode}"(供应商:${supplierCode})不在可选列表中，已跳过`)
          continue
        }

        const plannedQty = colMap.qty !== undefined ? Math.max(1, Number(row[colMap.qty]) || 1) : 1
        const warehouseArea = colMap.area !== undefined ? String(row[colMap.area] || '').trim() : '默认库区'
        const remark = colMap.remark !== undefined ? String(row[colMap.remark] || '').trim() : ''

        form.details.push({
          supplierCode,
          supplierName,
          materialCode,
          materialName: material.materialName,
          packageModel: material.packageModel || '',
          packagingCapacity: material.packageCapacity ?? 0,
          plannedQty,
          packageCount: calculatePackageCount(plannedQty, material.packageCapacity ?? 0),
          warehouseArea: warehouseArea || '默认库区',
          remark
        })
        addedCount++
      }

      if (addedCount > 0) {
        ElMessage.success(`成功导入 ${addedCount} 条物料明细`)
      } else {
        ElMessage.warning('未从 Excel 中解析到可导入的物料')
      }
    } catch (err) {
      ElMessage.error('Excel 文件解析失败：' + (err.message || '未知错误'))
    }
  }
  reader.readAsArrayBuffer(file)
  event.target.value = ''
}

function detectColumns(headerRow) {
  const map = {}
  for (let i = 0; i < headerRow.length; i++) {
    const h = String(headerRow[i] ?? '').trim()
    if (/物料号|零件号|物料编码|编码/.test(h)) map.materialCode = i
    else if (/数量|计划数量|出库数量|个数/.test(h)) map.qty = i
    else if (/库区|仓库区域/.test(h)) map.area = i
    else if (/备注|说明/.test(h)) map.remark = i
    else if (/供应商/.test(h)) map.supplierCode = i
  }
  return map
}

// ── Submit ──

function handleClose() {
  emit('update:visible', false)
}

function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (!valid || !validateDetails()) return

    const supplierNames = [...new Set(form.details.map((d) => d.supplierName).filter(Boolean))]
    emit('submit', {
      supplier: supplierNames.length === 1 ? supplierNames[0] : '多需求方',
      remark: form.remark?.trim() || '',
      details: form.details.map((d) => ({
        supplierCode: d.supplierCode,
        supplierName: d.supplierName,
        materialCode: d.materialCode.trim(),
        materialName: d.materialName.trim(),
        packageModel: d.packageModel?.trim() || '',
        packagingCapacity: d.packagingCapacity ?? 0,
        plannedQty: d.plannedQty,
        packageCount: calculatePackageCount(d.plannedQty, d.packagingCapacity),
        warehouseArea: d.warehouseArea?.trim() || '默认库区',
        remark: d.remark?.trim() || ''
      }))
    })
  })
}

function validateDetails() {
  if (!form.details.length) {
    ElMessage.warning('请至少添加一条出库明细')
    return false
  }
  const keys = new Set()
  for (const item of form.details) {
    if (!item.plannedQty || item.plannedQty < 1) {
      ElMessage.warning(`${item.materialName || item.materialCode} 的计划数量必须大于 0`)
      return false
    }
    const key = `${item.supplierCode}::${item.materialCode}`
    if (keys.has(key)) {
      ElMessage.warning('同一张出库单中不允许重复选择同一供应商下的同一物料')
      return false
    }
    keys.add(key)
  }
  return true
}

function updatePackageCount(row) {
  row.packageCount = calculatePackageCount(row.plannedQty, row.packagingCapacity)
}

function calculatePackageCount(plannedQty, packagingCapacity) {
  const planned = Number(plannedQty) || 0
  const capacity = Number(packagingCapacity) || 0
  if (planned <= 0 || capacity <= 0) return 1
  return Math.ceil(planned / capacity)
}

function calculateBoxCount(qty, packagingCapacity) {
  const quantity = Number(qty) || 0
  const capacity = Number(packagingCapacity) || 0
  if (quantity <= 0 || capacity <= 0) return 0
  return Math.round((quantity * 10) / capacity) / 10
}

function boxCountText(row) {
  const capacity = Number(row.packagingCapacity) || 0
  if (capacity <= 0) return '未配置'
  return calculateBoxCount(row.plannedQty, row.packagingCapacity)
}
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0 10px;
  font-weight: 600;
}

/* 可选零件区域 */
.material-picker {
  margin-bottom: 8px;
}

.material-search {
  margin-bottom: 10px;
}

.material-list {
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.material-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}

.material-card:last-child {
  border-bottom: none;
}

.material-card:hover {
  background: #f5f7fa;
}

.material-card.added {
  background: #f0f9eb;
}

.card-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-code {
  font-weight: 600;
  color: #303133;
}

.card-divider {
  color: #dcdfe6;
}

.card-name {
  color: #606266;
}

.card-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 选择供应商提示 */
.select-supplier-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
  gap: 8px;
}

/* 明细表格 */
.detail-table {
  margin-bottom: 8px;
}

.detail-material-code {
  font-weight: 600;
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
