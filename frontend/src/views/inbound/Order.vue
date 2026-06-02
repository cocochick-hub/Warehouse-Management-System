<template>
  <PageContainer title="入库单管理">
    <template #actions>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>创建入库单
      </el-button>
    </template>

    <el-form :model="query" inline>
      <el-form-item label="单号">
        <el-input v-model="query.docNo" placeholder="入库单号" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="query.supplier" placeholder="选择供应商" clearable style="width: 160px">
          <el-option label="全部" value="" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="单据状态" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="未入库" value="0" />
          <el-option label="部分完成" value="1" />
          <el-option label="已完成" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="docNo" label="单号" width="180" />
      <el-table-column prop="supplier" label="供应商" min-width="160" />
      <el-table-column prop="itemCount" label="零件种类数" width="100" />
      <el-table-column prop="totalQty" label="计划总数" width="90" />
      <el-table-column prop="actualQty" label="实收总数" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.statusType" size="small">{{ row.statusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <el-button type="primary" link size="small">查看</el-button>
        <el-button type="success" link size="small">入库</el-button>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'

const query = reactive({
  docNo: '',
  supplier: '',
  status: ''
})

const tableData = ref([])

function handleCreate() {
  ElMessage.info('创建入库单功能开发中')
}

function handleSearch() {
  ElMessage.info('查询功能开发中')
}

function handleReset() {
  query.docNo = ''
  query.supplier = ''
  query.status = ''
}
</script>
