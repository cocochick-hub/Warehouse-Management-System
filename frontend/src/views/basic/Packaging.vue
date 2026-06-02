<template>
  <PageContainer title="包装管理">
    <template #actions>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增包装
      </el-button>
    </template>

    <el-table :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="materialNo" label="物料号" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="supplier" label="供应商" width="160" />
      <el-table-column prop="packageModel" label="包装型号" width="130" />
      <el-table-column prop="packageCapacity" label="包装容量" width="100" />
      <el-table-column label="操作" width="160" fixed="right">
        <el-button type="primary" link size="small">
          <el-icon><Edit /></el-icon>编辑
        </el-button>
        <el-button type="danger" link size="small">
          <el-icon><Delete /></el-icon>删除
        </el-button>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'

const tableData = ref([])

function handleAdd() {
  ElMessage.info('新增包装功能开发中')
}
</script>
