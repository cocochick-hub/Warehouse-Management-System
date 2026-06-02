<template>
  <PageContainer title="高低储预警">
    <el-alert
      title="为每种物料设定低储天数和高储天数，系统自动计算安全库存线。当库存低于低储天数或高于高储天数时，在库存报表中触发预警。"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <el-table :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="materialNo" label="物料号" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="supplier" label="供应商" width="160" />
      <el-table-column prop="currentStock" label="当前库存" width="100" />
      <el-table-column prop="lowDays" label="低储天数" width="90">
        <template #default="{ row }">
          <el-input-number v-model="row.lowDays" :min="0" size="small" controls-position="right" style="width: 100px" />
        </template>
      </el-table-column>
      <el-table-column prop="highDays" label="高储天数" width="90">
        <template #default="{ row }">
          <el-input-number v-model="row.highDays" :min="0" size="small" controls-position="right" style="width: 100px" />
        </template>
      </el-table-column>
      <el-table-column prop="alertStatus" label="当前状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.currentStock < row.lowDays" type="danger" size="small">低储</el-tag>
          <el-tag v-else-if="row.currentStock > row.highDays" type="warning" size="small">高储</el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <el-button type="primary" link size="small">保存</el-button>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup>
import { ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'

const tableData = ref([])
</script>
