<template>
  <PageContainer title="手工出库">
    <el-alert title="选择出库单后，录入实际发出的零件数量，系统校验库存充足后扣减库存。" type="warning" :closable="false" show-icon style="margin-bottom: 16px" />

    <el-form :model="query" inline>
      <el-form-item label="出库单号">
        <el-input v-model="query.docNo" placeholder="搜索出库单号" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="query.supplier" placeholder="选择供应商" clearable style="width: 160px">
          <el-option label="全部" value="" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button>重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" stripe border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="docNo" label="出库单号" width="180" />
      <el-table-column prop="supplier" label="供应商" width="160" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.statusType" size="small">{{ row.statusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <el-button type="primary" link size="small">开始出库</el-button>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import PageContainer from '@/components/PageContainer.vue'

const query = reactive({
  docNo: '',
  supplier: ''
})

const tableData = ref([])
</script>
