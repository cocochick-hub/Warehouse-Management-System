<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-label">待入库单</div>
              <div class="stat-value">5</div>
              <div class="stat-trend">今日新增 +2</div>
            </div>
            <el-icon :size="48" color="#409eff"><Download /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-label">待出库单</div>
              <div class="stat-value">3</div>
              <div class="stat-trend">今日新增 +1</div>
            </div>
            <el-icon :size="48" color="#67c23a"><Upload /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-label">低储预警</div>
              <div class="stat-value warning">2</div>
              <div class="stat-trend">需及时补货</div>
            </div>
            <el-icon :size="48" color="#e6a23c"><WarningFilled /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-label">总物料数</div>
              <div class="stat-value">128</div>
              <div class="stat-trend">涵盖 32 个供应商</div>
            </div>
            <el-icon :size="48" color="#909399"><Box /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>待办任务清单</span>
              <el-button text type="primary">查看全部</el-button>
            </div>
          </template>
          <el-table :data="todoList" stripe style="width: 100%">
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type === '入库' ? 'primary' : 'warning'" size="small">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="docNo" label="单号" width="180" />
            <el-table-column prop="supplier" label="供应商" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.statusColor" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="date" label="日期" width="110" />
            <el-table-column label="操作" width="90" fixed="right">
              <el-button type="primary" link size="small">处理</el-button>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>库存健康度</span>
              <el-button text type="primary" @click="$router.push('/inventory/report')">详情</el-button>
            </div>
          </template>
          <div class="health-section">
            <div class="health-item">
              <div class="health-label">库存健康度</div>
              <div class="health-value">
                <el-progress :percentage="85" :stroke-width="12" striped />
              </div>
            </div>
            <el-divider />
            <div class="status-list">
              <div class="status-row">
                <span class="status-dot dot-normal" />
                <span>库存正常</span>
                <span class="status-count">120 项</span>
              </div>
              <div class="status-row">
                <span class="status-dot dot-warning" />
                <span>低储预警</span>
                <span class="status-count">2 项</span>
              </div>
              <div class="status-row">
                <span class="status-dot dot-danger" />
                <span>高储预警</span>
                <span class="status-count">6 项</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/inbound/order')">
              <el-icon><Download /></el-icon>新建入库单
            </el-button>
            <el-button type="warning" size="large" @click="$router.push('/outbound/order')">
              <el-icon><Upload /></el-icon>新建出库单
            </el-button>
            <el-button type="success" size="large" @click="$router.push('/inventory/report')">
              <el-icon><DataBoard /></el-icon>查看库存
            </el-button>
            <el-button size="large" @click="$router.push('/alert/threshold')">
              <el-icon><WarningFilled /></el-icon>预警设置
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const todoList = [
  { type: '入库', docNo: 'IN20240601001', supplier: '上海汽车零部件', status: '未入库', statusColor: 'info', date: '2024-06-01' },
  { type: '入库', docNo: 'IN20240601002', supplier: '长春一汽配套', status: '部分完成', statusColor: 'warning', date: '2024-06-01' },
  { type: '出库', docNo: 'OUT20240601001', supplier: '广州本田', status: '待出库', statusColor: 'primary', date: '2024-06-01' },
  { type: '出库', docNo: 'OUT20240601002', supplier: '武汉东风', status: '待出库', statusColor: 'primary', date: '2024-06-01' }
]
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
}

.stat-card {
  border-radius: 8px;
}

.stat-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-trend {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
}

.health-section {
  padding: 4px 0;
}

.health-item {
  margin-bottom: 8px;
}

.health-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.status-count {
  margin-left: auto;
  font-weight: 600;
  color: #303133;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.dot-normal {
  background: #67c23a;
}

.dot-warning {
  background: #e6a23c;
}

.dot-danger {
  background: #f56c6c;
}

.quick-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
</style>
