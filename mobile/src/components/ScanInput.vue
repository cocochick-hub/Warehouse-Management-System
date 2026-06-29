<template>
  <view class="scan-input">
    <view class="input-row">
      <input
        v-model="inputValue"
        class="scan-field"
        :placeholder="placeholder || '请输入或扫码看板号'"
        @confirm="onQuery"
      />
      <van-button type="primary" size="small" @click="onScan">
        <van-icon name="scan" />
      </van-button>
      <van-button type="default" size="small" @click="onQuery" style="margin-left:6px">
        查询
      </van-button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Button, Icon } from 'vant'

const props = defineProps({
  placeholder: { type: String, default: '请输入或扫码看板号' },
  autoscan: { type: Boolean, default: true }
})

const emit = defineEmits(['scan'])
const inputValue = ref('')

function parseKanbanNo(raw) {
  if (!raw) return ''
  // 去掉 WMS-INBOUND| 前缀
  return raw.replace(/^WMS-INBOUND\|/, '').trim()
}

function onQuery() {
  const kanbanNo = parseKanbanNo(inputValue.value)
  if (kanbanNo) {
    emit('scan', kanbanNo)
  }
}

function onScan() {
  uni.scanCode({
    onlyFromCamera: true,
    success(res) {
      const kanbanNo = parseKanbanNo(res.result)
      inputValue.value = kanbanNo
      emit('scan', kanbanNo)
    },
    fail() {
      uni.showToast({ title: '扫码取消', icon: 'none' })
    }
  })
}

function triggerAutoScan() {
  uni.scanCode({
    onlyFromCamera: true,
    success(res) {
      const kanbanNo = parseKanbanNo(res.result)
      inputValue.value = kanbanNo
      emit('scan', kanbanNo)
    },
    fail() {
      // 自动扫码失败不提示，用户可手动输入或点击扫码按钮
    }
  })
}

// 进入页面自动弹出扫码（延时等页面过渡动画完成）
onMounted(() => {
  if (props.autoscan) {
    setTimeout(() => {
      triggerAutoScan()
    }, 300)
  }
})

defineExpose({
  clear() {
    inputValue.value = ''
  }
})
</script>

<style scoped>
.scan-input {
  margin-bottom: 12px;
}
.input-row {
  display: flex;
  align-items: center;
}
.scan-field {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #ebedf0;
  border-radius: 4px;
  font-size: 15px;
  background: #fff;
}
</style>
