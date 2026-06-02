<template>
  <PageContainer title="需求导入">
    <el-alert title="下载导入模板 → 填写零件需求数据 → 上传导入系统。导入后可在物料需求页面查看。" type="info" :closable="false" show-icon style="margin-bottom: 16px" />

    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="6">
        <el-button type="success">
          <el-icon><Download /></el-icon>下载导入模板
        </el-button>
      </el-col>
      <el-col :span="6">
        <el-upload
          action="#"
          :auto-upload="false"
          :show-file-list="true"
          accept=".xlsx,.xls,.csv"
        >
          <el-button type="primary">
            <el-icon><Upload /></el-icon>上传需求文件
          </el-button>
        </el-upload>
      </el-col>
    </el-row>

    <el-table :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="batchNo" label="导入批次号" width="180" />
      <el-table-column prop="itemCount" label="零件种类数" width="110" />
      <el-table-column prop="totalQty" label="需求总数" width="100" />
      <el-table-column prop="importTime" label="导入时间" width="170" />
      <el-table-column prop="operator" label="操作人" width="120" />
      <el-table-column label="操作" width="80" fixed="right">
        <el-button type="primary" link size="small">查看</el-button>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup>
import { ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'

const tableData = ref([])
</script>
