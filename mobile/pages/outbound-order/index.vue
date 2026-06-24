<template>
  <view class="order-page">
    <van-nav-bar title="带单出库" left-arrow @click-left="goBack" fixed safe-area-inset-top />

    <view class="order-body">
      <van-search v-model="searchDocNo" placeholder="搜索出库单号" @search="onSearch" />

      <van-list v-if="orders.length" finished-text="没有更多了">
        <van-cell
          v-for="order in orders"
          :key="order.docNo"
          :title="order.docNo"
          :label="`供应商：${order.supplier} | ${order.status} | 计划 ${order.plannedTotalQty} / 实发 ${order.actualTotalQty}`"
          is-link
          @click="onSelect(order)"
        />
      </van-list>

      <van-empty v-else description="暂无未完成的出库单" />
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOutboundOrders } from '@/api/outbound'

const orders = ref([])
const searchDocNo = ref('')

async function fetchOrders(docNo) {
  try {
    const res = await getOutboundOrders({ status: '待出库,部分完成', docNo: docNo || undefined })
    orders.value = res.data?.records || []
  } catch {
    orders.value = []
  }
}

function onSearch() {
  fetchOrders(searchDocNo.value)
}

function onSelect(order) {
  uni.navigateTo({
    url: `/pages/outbound-scan/index?outboundOrderId=${order.id}`
  })
}

function goBack() {
  uni.navigateBack()
}

onMounted(() => fetchOrders())
</script>

<style scoped>
.order-page { min-height: 100vh; }
.order-body { margin-top: 46px; }
</style>
