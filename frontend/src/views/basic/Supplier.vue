<template>
  <PageContainer title="供应商管理">
    <template #actions>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增供应商
      </el-button>
    </template>

    <el-table v-loading="loading" :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="supplierCode" label="供应商代码" width="130" />
      <el-table-column prop="supplierName" label="供应商名称" min-width="180" />
      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系方式" width="150" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'
import { getSuppliersApi } from '@/api/basic'

const loading = ref(false)
const tableData = ref([])

onMounted(() => {
  fetchSuppliers()
})

async function fetchSuppliers() {
  loading.value = true
  try {
    const { data } = await getSuppliersApi()
    tableData.value = data || []
  } catch (error) {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  ElMessage.info('新增供应商功能开发中')
}
</script>
