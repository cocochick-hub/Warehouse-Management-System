<template>
  <PageContainer title="物料管理">
    <template #actions>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增物料
      </el-button>
    </template>

    <el-form :model="query" inline>
      <el-form-item label="物料号">
        <el-input v-model="query.materialNo" placeholder="物料号" clearable />
      </el-form-item>
      <el-form-item label="物料名称">
        <el-input v-model="query.materialName" placeholder="物料名称" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="query.supplier" placeholder="选择供应商" clearable style="width: 160px">
          <el-option label="全部" value="" />
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
      <el-table-column prop="materialNo" label="物料号" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="160" />
      <el-table-column prop="materialType" label="物料类型" width="100" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="supplier" label="供应商" width="160" />
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

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </PageContainer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer.vue'

const query = reactive({
  materialNo: '',
  materialName: '',
  supplier: ''
})

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])

function handleAdd() {
  ElMessage.info('新增物料功能开发中')
}

function handleSearch() {
  ElMessage.info('查询功能开发中')
}

function handleReset() {
  query.materialNo = ''
  query.materialName = ''
  query.supplier = ''
}

function handleSizeChange() {}
function handleCurrentChange() {}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
