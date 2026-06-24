<template>
  <van-dialog
    :show="visible"
    title="⚠ FIFO 先进先出预警"
    :message="dialogMessage"
    show-cancel-button
    confirm-text="继续出库"
    cancel-text="取消"
    @confirm="$emit('confirm')"
    @cancel="$emit('cancel')"
  />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  message: { type: String, default: '' },
  earliestDocNo: { type: String, default: '' }
})

defineEmits(['confirm', 'cancel'])

const dialogMessage = computed(() => {
  let msg = props.message || '当前出库的库存并非最早入库，是否要继续出库？'
  if (props.earliestDocNo) {
    msg += `\n最早入库单号：${props.earliestDocNo}`
  }
  return msg
})
</script>
