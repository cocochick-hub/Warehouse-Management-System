<template>
  <van-cell-group v-if="visible" title="物料信息">
    <van-cell title="物料号" :value="materialCode" />
    <van-cell title="物料名称" :value="materialName" />
    <van-cell title="供应商" :value="supplier" />
    <van-cell title="数量" :value="`${qty} 件`" />
    <van-cell title="库区" :value="warehouseArea || '默认库区'" />
    <van-cell
      v-for="(row, i) in extraRows"
      :key="i"
      :title="row.label"
      :value="row.value"
      :label-style="row.warn ? 'color: #ee0a24' : ''"
    />
  </van-cell-group>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  materialCode: { type: String, default: '' },
  materialName: { type: String, default: '' },
  supplier: { type: String, default: '' },
  qty: { type: [Number, String], default: 0 },
  warehouseArea: { type: String, default: '默认库区' },
  extraRows: { type: Array, default: () => [] }
})

const visible = computed(() => !!props.materialCode)
</script>
