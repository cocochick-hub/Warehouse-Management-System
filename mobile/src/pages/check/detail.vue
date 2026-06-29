<template>
  <view class="check-detail-page">
    <van-nav-bar
      :title="taskName"
      left-arrow
      @click-left="goBack"
      fixed
      safe-area-inset-top
    />

    <view class="check-detail-body">
      <!-- 扫码区 -->
      <view class="scan-section">
        <ScanInput ref="scanInputRef" placeholder="请扫码或输入物料编码" @scan="onScan" />
      </view>

      <!-- 盘点进度 -->
      <view class="progress-summary">
        <view class="progress-info">
          <text class="progress-text">已盘 {{ checkedCount }}/{{ totalCount }}</text>
          <text class="progress-pct">{{ progressPercent }}%</text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: progressPercent + '%' }" />
        </view>
      </view>

      <!-- 差异提示 -->
      <view v-if="diffCount > 0" class="diff-banner">
        <van-icon name="warning-o" size="16" color="#ff976a" />
        <text>存在 {{ diffCount }} 条差异，完成后请联系管理员处理</text>
      </view>

      <!-- 明盘模式：显示系统库存 -->
      <view v-if="checkType === '明盘' && currentDetail" class="material-card">
        <view class="card-header">
          <text class="material-name">{{ currentDetail.materialName }}</text>
          <van-tag :type="getStatusType(currentDetail.status)" size="small">
            {{ currentDetail.status }}
          </van-tag>
        </view>
        <view class="card-rows">
          <view class="card-row">
            <text class="row-label">物料编码</text>
            <text class="row-value">{{ currentDetail.materialCode }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">供应商</text>
            <text class="row-value">{{ currentDetail.supplier }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">库区</text>
            <text class="row-value">{{ currentDetail.warehouseArea || '默认库区' }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">系统库存</text>
            <text class="row-value highlight">{{ currentDetail.systemQty }} 件</text>
          </view>
          <view v-if="currentDetail.status === '已盘'" class="card-row">
            <text class="row-label">实盘数量</text>
            <text class="row-value">{{ currentDetail.actualQty }} 件</text>
          </view>
          <view v-if="currentDetail.status === '已盘'" class="card-row">
            <text class="row-label">差异</text>
            <text class="row-value" :class="getDiffClass(currentDetail.diffQty)">
              {{ formatDiff(currentDetail.diffQty) }}
            </text>
          </view>
        </view>

        <!-- 输入实际数量 -->
        <view v-if="currentDetail.status === '待盘'" class="qty-input-section">
          <van-field
            v-model="inputQty"
            type="number"
            label="实盘数量"
            placeholder="请输入实际盘点数量"
            :disabled="submitting"
          />
          <van-button
            type="primary"
            block
            round
            :loading="submitting"
            style="margin-top: 12px"
            @click="confirmScan"
          >
            确认盘点
          </van-button>
        </view>

        <van-button
          v-if="currentDetail.status === '已盘'"
          plain
          block
          round
          style="margin-top: 12px"
          @click="resetCurrent"
        >
          继续盘点其他
        </van-button>
      </view>

      <!-- 盲盘模式：不显示系统库存 -->
      <view v-if="checkType === '盲盘' && currentDetail" class="material-card">
        <view class="card-header">
          <text class="material-name">{{ currentDetail.materialName }}</text>
          <van-tag :type="getStatusType(currentDetail.status)" size="small">
            {{ currentDetail.status }}
          </van-tag>
        </view>
        <view class="card-rows">
          <view class="card-row">
            <text class="row-label">物料编码</text>
            <text class="row-value">{{ currentDetail.materialCode }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">供应商</text>
            <text class="row-value">{{ currentDetail.supplier }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">库区</text>
            <text class="row-value">{{ currentDetail.warehouseArea || '默认库区' }}</text>
          </view>
          <view v-if="currentDetail.status === '已盘'" class="card-row">
            <text class="row-label">实盘数量</text>
            <text class="row-value">{{ currentDetail.actualQty }} 件</text>
          </view>
          <view v-if="currentDetail.status === '已盘'" class="card-row">
            <text class="row-label">差异</text>
            <text class="row-value" :class="getDiffClass(currentDetail.diffQty)">
              {{ formatDiff(currentDetail.diffQty) }}
            </text>
          </view>
        </view>

        <view v-if="currentDetail.status === '待盘'" class="qty-input-section">
          <van-field
            v-model="inputQty"
            type="number"
            label="实盘数量"
            placeholder="请输入实际盘点数量"
            :disabled="submitting"
          />
          <van-button
            type="primary"
            block
            round
            :loading="submitting"
            style="margin-top: 12px"
            @click="confirmScan"
          >
            确认盘点
          </van-button>
        </view>

        <van-button
          v-if="currentDetail.status === '已盘'"
          plain
          block
          round
          style="margin-top: 12px"
          @click="resetCurrent"
        >
          继续盘点其他
        </van-button>
      </view>

      <!-- 已盘列表（显示差异项） -->
      <view v-if="checkedList.length > 0" class="checked-list">
        <view class="list-title">已盘物料</view>
        <view
          v-for="item in checkedList"
          :key="item.id"
          class="checked-item"
        >
          <view class="item-info">
            <text class="item-name">{{ item.materialName }}</text>
            <text class="item-code">{{ item.materialCode }}</text>
          </view>
          <view class="item-qty">
            <text>差异：</text>
            <text :class="getDiffClass(item.diffQty)">{{ formatDiff(item.diffQty) }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NavBar, Tag, Field, Button, Loading, Icon, Empty } from 'vant'
import ScanInput from '@/components/ScanInput.vue'
import { getCheckDetail, scanCheck } from '@/api/check'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const scanInputRef = ref(null)

// 页面参数
const taskId = ref(null)
const taskName = ref('')
const checkType = ref('明盘')

// 进度
const totalCount = ref(0)
const checkedCount = ref(0)
const progressPercent = ref(0)
const diffCount = ref(0)

// 当前扫码的明细
const currentDetail = ref(null)
const inputQty = ref('')
const submitting = ref(false)

// 已盘列表（仅显示有差异的）
const checkedList = ref([])

onMounted(() => {
  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  const opts = cur.options || {}
  taskId.value = Number(opts.taskId)
  taskName.value = decodeURIComponent(opts.taskName || '盘点')
  checkType.value = decodeURIComponent(opts.checkType || '明盘')
  loadProgress()
})

async function loadProgress() {
  // 从已加载的tasks中更新进度（如果从列表页进入）
}

async function onScan(materialCode) {
  try {
    // 先获取明细列表，找到匹配的物料
    const res = await uni.request({
      url: `${uni.getStorageSync('wms_base_url') || 'http://localhost:8080'}/api/check/tasks/${taskId.value}`,
      method: 'GET',
      header: { 'Authorization': `Bearer ${uni.getStorageSync('wms_token')}` }
    })
    const details = (res.data?.data || []).filter(d => d.materialCode === materialCode)
    if (details.length === 0) {
      uni.showToast({ title: '该物料不在盘点范围内', icon: 'none' })
      return
    }
    const detail = details[0]
    currentDetail.value = detail
    inputQty.value = ''
    checkedCount.value++
    // 更新差异数
    diffCount.value = details.filter(d => d.diffQty != null && d.diffQty !== 0).length
  } catch (e) {
    uni.showToast({ title: '物料不在盘点范围内', icon: 'none' })
  }
}

async function confirmScan() {
  if (!inputQty.value || isNaN(Number(inputQty.value))) {
    uni.showToast({ title: '请输入有效的实盘数量', icon: 'none' })
    return
  }
  if (!currentDetail.value) return
  submitting.value = true
  try {
    const username = userStore.userInfo?.username || 'operator'
    const res = await scanCheck({
      taskId: taskId.value,
      materialCode: currentDetail.value.materialCode,
      actualQty: Number(inputQty.value),
      warehouseArea: currentDetail.value.warehouseArea,
      checkedBy: username
    })
    const progress = res.data
    totalCount.value = progress.total
    checkedCount.value = progress.checked
    progressPercent.value = progress.progressPercent
    diffCount.value = progress.diffCount

    // 更新当前明细状态
    currentDetail.value.actualQty = Number(inputQty.value)
    currentDetail.value.diffQty = Number(inputQty.value) - currentDetail.value.systemQty
    currentDetail.value.status = '已盘'

    // 加入已盘列表
    if (currentDetail.value.diffQty !== 0) {
      checkedList.value.push({ ...currentDetail.value })
    }

    uni.showToast({ title: '盘点成功', icon: 'success' })
    inputQty.value = ''
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    submitting.value = false
  }
}

function resetCurrent() {
  currentDetail.value = null
  inputQty.value = ''
  scanInputRef.value?.clear()
}

function goBack() {
  uni.navigateBack()
}

function getStatusType(status) {
  if (status === '已盘') return 'success'
  if (status === '已调整') return 'primary'
  return 'default'
}

function getDiffClass(diff) {
  if (diff == null) return ''
  if (diff > 0) return 'diff-profit'
  if (diff < 0) return 'diff-loss'
  return ''
}

function formatDiff(diff) {
  if (diff == null) return '0'
  if (diff > 0) return `+${diff}`
  return String(diff)
}
</script>

<style scoped>
.check-detail-page { min-height: 100vh; background: #f7f8fa; }
.check-detail-body { padding: 12px; margin-top: 46px; }
.scan-section { margin-bottom: 12px; }
.progress-summary { background: #fff; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.progress-info { display: flex; justify-content: space-between; margin-bottom: 8px; }
.progress-text { font-size: 13px; color: #646566; }
.progress-pct { font-size: 13px; color: #1989fa; font-weight: 600; }
.progress-bar { height: 6px; background: #ebedf0; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: #1989fa; border-radius: 3px; transition: width 0.3s; }
.diff-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff3e0;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #ff976a;
}
.material-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.material-name { font-size: 15px; font-weight: 600; color: #323233; }
.card-rows { display: flex; flex-direction: column; gap: 6px; }
.card-row { display: flex; justify-content: space-between; font-size: 13px; }
.row-label { color: #969799; }
.row-value { color: #323233; }
.row-value.highlight { color: #1989fa; font-weight: 600; }
.row-value.diff-profit { color: #07c160; font-weight: 600; }
.row-value.diff-loss { color: #ee0a24; font-weight: 600; }
.qty-input-section { margin-top: 12px; }
.checked-list { margin-top: 12px; }
.list-title { font-size: 13px; color: #646566; margin-bottom: 8px; }
.checked-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 8px;
}
.item-info { display: flex; flex-direction: column; gap: 2px; }
.item-name { font-size: 13px; color: #323233; }
.item-code { font-size: 11px; color: #969799; }
.item-qty { font-size: 13px; color: #646566; }
.diff-profit { color: #07c160; font-weight: 600; }
.diff-loss { color: #ee0a24; font-weight: 600; }
</style>