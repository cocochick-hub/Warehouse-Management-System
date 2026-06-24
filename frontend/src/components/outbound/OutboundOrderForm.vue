<template>
  <el-dialog
    :model-value="visible"
    title="创建出库单"
    width="880px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" label-width="110px">
      <el-form-item label="需求方" prop="supplier">
        <el-input v-model="form.supplier" placeholder="可选，不填则自动根据明细生成" maxlength="100" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选，填写单据备注" maxlength="255" show-word-limit />
      </el-form-item>

      <div class="section-head">
        <span>出库明细</span>
        <el-button type="primary" link @click="addDetail"><el-icon><Plus /></el-icon>新增明细</el-button>
      </div>

      <el-table :data="form.details" border class="detail-table">
        <el-table-column type="index" label="行号" width="60" />
        <el-table-column label="供应商" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.supplierCode" placeholder="选择供应商" filterable clearable style="width: 100%"
              @change="(value) => handleRowSupplierChange(row, value)">
              <el-option v-for="s in suppliers" :key="s.supplierCode" :label="s.supplierName" :value="s.supplierCode" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="物料选择" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.materialCode" placeholder="请选择物料" filterable clearable style="width: 100%"
              :disabled="!row.supplierCode"
              @change="(value) => handleMaterialChange(row, value)">
              <el-option v-for="m in row.materialOptions" :key="m.materialNo"
                :label="`${m.materialNo} / ${m.materialName}`" :value="m.materialNo" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="物料名称" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.materialName" placeholder="自动带出" maxlength="100" readonly />
          </template>
        </el-table-column>
        <el-table-column label="计划数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.plannedQty" :min="1" :step="1" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="库区" width="140">
          <template #default="{ row }">
            <el-select v-model="row.warehouseArea" placeholder="默认库区" style="width: 100%">
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
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getMaterialsApi, getWarehouseAreasApi } from '@/api/basic'

const props = defineProps({
  visible: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
  suppliers: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref()
const form = reactive(createDefaultForm())
const warehouseAreaOptions = ref([])

watch(() => props.visible, async (value) => {
  if (value) {
    resetForm()
    await fetchWarehouseAreas()
  }
})

function createDefaultDetail() {
  return {
    supplierCode: '', supplierName: '', materialCode: '', materialName: '',
    plannedQty: 1, warehouseArea: '默认库区', materialOptions: [], remark: ''
  }
}

function createDefaultForm() {
  return { supplier: '', remark: '', details: [createDefaultDetail()] }
}

function resetForm() {
  const next = createDefaultForm()
  form.supplier = next.supplier
  form.remark = next.remark
  form.details.splice(0, form.details.length, ...next.details)
}

function addDetail() {
  const prev = form.details[form.details.length - 1]
  const next = createDefaultDetail()
  if (prev?.supplierCode) {
    next.supplierCode = prev.supplierCode
    next.supplierName = prev.supplierName
    next.materialOptions = prev.materialOptions || []
  }
  form.details.push(next)
}

function removeDetail(index) {
  if (form.details.length === 1) { ElMessage.warning('至少保留一条明细'); return }
  form.details.splice(index, 1)
}

async function handleRowSupplierChange(row, code) {
  const s = props.suppliers.find(s => s.supplierCode === code)
  row.supplierName = s?.supplierName || ''
  row.materialCode = ''
  row.materialName = ''
  row.materialOptions = []
  if (!s) return
  try {
    const { data } = await getMaterialsApi({ supplierCode: s.supplierCode })
    row.materialOptions = data || []
  } catch { row.materialOptions = [] }
}

function handleMaterialChange(row, materialNo) {
  const m = (row.materialOptions || []).find(m => m.materialNo === materialNo)
  if (!m) { row.materialCode = ''; row.materialName = ''; return }
  row.materialCode = m.materialNo
  row.materialName = m.materialName
}

async function fetchWarehouseAreas() {
  try {
    const { data } = await getWarehouseAreasApi()
    warehouseAreaOptions.value = data || []
  } catch {
    warehouseAreaOptions.value = []
  }
}

function validateDetails() {
  if (!form.details.length) { ElMessage.warning('请至少填写一条出库明细'); return false }
  const keys = new Set()
  for (const item of form.details) {
    if (!item.supplierCode?.trim()) { ElMessage.warning('请选择供应商'); return false }
    if (!item.materialCode?.trim()) { ElMessage.warning('请选择物料'); return false }
    if (!item.materialName?.trim()) { ElMessage.warning('请填写物料名称'); return false }
    if (!item.plannedQty || item.plannedQty < 1) { ElMessage.warning('计划数量必须大于0'); return false }
    const key = `${item.supplierCode.trim()}::${item.materialCode.trim()}`
    if (keys.has(key)) { ElMessage.warning('同一张出库单中不允许重复选择同一供应商下的同一物料'); return false }
    keys.add(key)
  }
  return true
}

function handleSubmit() {
  if (!validateDetails()) return

  const supplierNames = [...new Set(form.details.map(d => d.supplierName).filter(Boolean))]
  emit('submit', {
    supplier: form.supplier?.trim() || (supplierNames.length === 1 ? supplierNames[0] : '多需求方'),
    remark: form.remark?.trim() || '',
    details: form.details.map(d => ({
      supplierCode: d.supplierCode.trim(),
      supplierName: d.supplierName.trim(),
      materialCode: d.materialCode.trim(),
      materialName: d.materialName.trim(),
      plannedQty: d.plannedQty,
      warehouseArea: d.warehouseArea?.trim() || '默认库区',
      remark: d.remark?.trim() || ''
    }))
  })
}

function handleClose() { emit('update:visible', false) }
</script>

<style scoped>
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; font-weight: 600; }
.detail-table { margin-bottom: 8px; }
</style>
