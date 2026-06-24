<template>
  <PageContainer title="需求导入（Excel）">
    <el-alert
      title="下载导入模板 → 按模板格式填写零件需求数据 → 上传文件导入系统。导入成功后可在「物料需求」页面查看。"
      type="info" :closable="false" show-icon style="margin-bottom: 16px" />

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-button type="success" @click="downloadTemplate">
          <el-icon><Download /></el-icon>下载导入模板
        </el-button>
      </el-col>
      <el-col :span="8">
        <el-upload
          ref="uploadRef"
          action="#"
          :auto-upload="false"
          :show-file-list="true"
          accept=".xlsx,.xls,.csv"
          :on-change="handleFileChange"
          :limit="1"
        >
          <el-button type="primary">
            <el-icon><Upload /></el-icon>选择需求文件
          </el-button>
        </el-upload>
      </el-col>
      <el-col :span="4">
        <el-button type="warning" :disabled="!selectedFile" @click="handleUpload" :loading="uploading">
          <el-icon><Check /></el-icon>开始导入
        </el-button>
      </el-col>
    </el-row>

    <el-divider />

    <el-alert v-if="importResult" :title="importResult" type="success" :closable="false" show-icon style="margin-bottom: 12px" />

    <el-table :data="batchList" stripe border style="width: 100%" v-loading="loading">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="batchNo" label="导入批次号" width="220" />
      <el-table-column prop="itemCount" label="物料种类数" width="110" />
      <el-table-column prop="totalQty" label="需求总数" width="100" />
      <el-table-column prop="importType" label="录入方式" width="100">
        <template #default="{ row }">
          <el-tag :type="row.importType === 'MANUAL' ? 'info' : 'success'" size="small">
            {{ row.importType === 'MANUAL' ? '手工' : 'Excel导入' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="操作人" width="120" />
      <el-table-column prop="createdAt" label="导入时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <el-button type="primary" link size="small" @click="goToList">查看</el-button>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer.vue'
import { listDemandsApi } from '@/api/demand'
import { formatDateTime } from '@/utils/inbound'
import { ElMessage } from 'element-plus'

const router = useRouter()
const uploadRef = ref(null)
const selectedFile = ref(null)
const uploading = ref(false)
const loading = ref(false)
const batchList = ref([])
const importResult = ref('')

function handleFileChange(file) {
  selectedFile.value = file.raw
}

function downloadTemplate() {
  // 生成 CSV 模板供下载
  const bom = '\uFEFF'
  const header = '物料号,物料名称,供应商代码,供应商名称,需求数量,需求日期,库区,备注'
  const sample = 'MAT-ENG-001,发动机支架,SUP-001,上海汽车零部件,50,2026-07-01,默认库区,'
  const csv = bom + header + '\n' + sample
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = '需求导入模板.csv'; a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('模板已下载，请按格式填写后上传')
}

function handleUpload() {
  ElMessage.info('Excel导入功能正在开发中，请先使用「手工创建需求」')
}

function goToList() {
  router.push('/demand/list')
}
</script>
