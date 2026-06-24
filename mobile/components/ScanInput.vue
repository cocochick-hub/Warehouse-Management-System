<template>
  <view class="scan-input">
    <van-field
      v-model="inputValue"
      :placeholder="placeholder || '请输入或扫码看板号'"
      :border="true"
      @confirm="onConfirm"
    >
      <template #button>
        <van-button type="primary" size="small" @click="onScan">
          <van-icon name="scan" />
          扫码
        </van-button>
      </template>
    </van-field>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  placeholder: { type: String, default: '请输入或扫码看板号' }
})

const emit = defineEmits(['scan'])
const inputValue = ref('')

function onConfirm() {
  const v = inputValue.value.trim()
  if (v) {
    emit('scan', v)
  }
}

function onScan() {
  uni.scanCode({
    onlyFromCamera: true,
    success(res) {
      inputValue.value = res.result
      emit('scan', res.result)
    },
    fail() {
      uni.showToast({ title: '扫码取消', icon: 'none' })
    }
  })
}

defineExpose({
  clear() {
    inputValue.value = ''
  }
})
</script>
