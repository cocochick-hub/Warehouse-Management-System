<template>
  <PageContainer title="库存报表">
    <template #actions>
      <el-button type="success">
        <el-icon><Download /></el-icon>导出报表
      </el-button>
    </template>

    <el-form :model="query" inline>
      <el-form-item label="物料号">
        <el-input v-model="query.materialNo" placeholder="物料号" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="query.supplier" placeholder="选择供应商" clearable style="width: 160px">
          <el-option label="全部" value="" />
        </el-select>
      </el-form-item>
      <el-form-item label="预警状态">
        <el-select v-model="query.alertType" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="正常" value="normal" />
          <el-option label="低储预警" value="low" />
          <el-option label="高储预警" value="high" />
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
      <el-table-column prop="materialNo" label="物料号" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="supplier" label="供应商" width="160" />
      <el-table-column prop="stock" label="当前库存" width="100" />
      <el-table-column prop="lowDays" label="低储天数" width="90" />
      <el-table-column prop="highDays" label="高储天数" width="90" />
      <el-table-column prop="alertStatus" label="预警状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.alertType" size="small">{{ row.alertStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <el-button type="primary" link size="small">详情</el-button>
      </el-table-column>
    </el-table>
  </PageContainer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import PageContainer from '@/components/PageContainer.vue'

const query = reactive({
  materialNo: '',
  supplier: '',
  alertType: ''
})

const tableData = ref([])
</script>
