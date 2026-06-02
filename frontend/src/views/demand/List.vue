<template>
  <PageContainer title="物料需求 / 出库记录">
    <template #actions>
      <el-button type="primary" @click="$router.push('/inventory/import')">
        <el-icon><Upload /></el-icon>导入需求
      </el-button>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="物料需求" name="demand">
        <el-table :data="demandData" stripe border style="width: 100%">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="partNo" label="零件号" width="140" />
          <el-table-column prop="partName" label="物料名称" min-width="140" />
          <el-table-column prop="demandQty" label="需求数量" width="100" />
          <el-table-column prop="demandDate" label="需求日期" width="110" />
          <el-table-column prop="batchNo" label="导入批次" width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.statusType" size="small">{{ row.statusLabel }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="出库记录" name="record">
        <el-table :data="recordData" stripe border style="width: 100%">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="docNo" label="出库单号" width="180" />
          <el-table-column prop="partNo" label="物料号" width="140" />
          <el-table-column prop="outQty" label="出库数量" width="100" />
          <el-table-column prop="outDate" label="出库日期" width="110" />
          <el-table-column prop="operator" label="操作人" width="120" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </PageContainer>
</template>

<script setup>
import { ref } from 'vue'
import PageContainer from '@/components/PageContainer.vue'

const activeTab = ref('demand')
const demandData = ref([])
const recordData = ref([])
</script>
