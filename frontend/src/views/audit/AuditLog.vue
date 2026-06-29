<template>
  <PageContainer title="操作日志">
    <el-form :model="query" inline @submit.prevent="fetchData">
      <el-form-item label="操作人">
        <el-input v-model="query.username" placeholder="用户名" clearable />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-select v-model="query.action" placeholder="全部" clearable>
          <el-option label="新增" value="CREATE" />
          <el-option label="修改" value="UPDATE" />
          <el-option label="删除" value="DELETE" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchData">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="username" label="操作人" width="100" />
      <el-table-column prop="action" label="操作类型" width="90">
        <template #default="{ row }">
          <el-tag :type="actionTag(row.action)" size="small">{{ actionLabel(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="target" label="操作对象" width="140" />
      <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="createdAt" label="操作时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @change="fetchData"
    />
  </PageContainer>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import PageContainer from '@/components/PageContainer.vue'
import { getAuditLogs } from '@/api/audit'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])

const query = reactive({
  page: 1,
  size: 20,
  username: '',
  action: ''
})

function actionTag(action) {
  return { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger' }[action] || 'info'
}

function actionLabel(action) {
  return { CREATE: '新增', UPDATE: '修改', DELETE: '删除' }[action] || action
}

function formatTime(val) {
  return val ? val.replace('T', ' ') : '-'
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      username: query.username || undefined,
      action: query.action || undefined
    }
    if (dateRange.value?.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const { data } = await getAuditLogs(params)
    tableData.value = data.content || []
    total.value = data.totalElements || 0
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.username = ''
  query.action = ''
  query.page = 1
  dateRange.value = []
  fetchData()
}

onMounted(fetchData)
</script>
