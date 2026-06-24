<template>
  <div class="ai-floating">
    <!-- 悬浮按钮 -->
    <div v-if="!panelVisible" class="ai-float-btn" @click="openPanel">
      <el-icon :size="28"><ChatDotRound /></el-icon>
      <span v-if="hasAlerts" class="ai-dot" />
    </div>

    <!-- 迷你聊天面板 -->
    <transition name="panel-slide">
      <div v-if="panelVisible" class="ai-mini-panel">
        <div class="panel-header">
          <div class="panel-title">
            <el-icon :size="18" color="#409eff"><ChatDotRound /></el-icon>
            <span>AI 仓库管理员</span>
          </div>
          <div class="panel-actions">
            <el-button text size="small" @click="goFullPage">
              <el-icon><FullScreen /></el-icon>
            </el-button>
            <el-button text size="small" @click="panelVisible = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="panel-body" ref="msgBody">
          <div v-if="messages.length === 0" class="panel-welcome">
            <p>👋 你好！我是 AI 仓库管理员，可以帮你：</p>
            <ul>
              <li>查询库存状态</li>
              <li>分析缺货风险</li>
              <li>识别呆滞物料</li>
              <li>对比供应商表现</li>
            </ul>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" :class="['msg-row', msg.role]">
            <div class="msg-bubble" v-html="renderMd(msg.content)" />
          </div>

          <div v-if="sending" class="msg-row assistant">
            <div class="msg-bubble typing">
              <span class="dot" /><span class="dot" /><span class="dot" />
            </div>
          </div>
        </div>

        <div class="panel-footer">
          <div class="input-row">
            <el-input
              v-model="input"
              placeholder="输入问题..."
              @keyup.enter="send"
              :disabled="sending"
              class="input-field"
            />
            <el-button
              :icon="Promotion"
              @click="send"
              :disabled="sending || !input.trim()"
              :type="input.trim() ? 'primary' : 'default'"
              class="send-btn"
            />
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Promotion } from '@element-plus/icons-vue'
import { chatApi, getLatestAlertsApi } from '@/api/ai'

const router = useRouter()

const panelVisible = ref(false)
const input = ref('')
const messages = ref([])
const sending = ref(false)
const hasAlerts = ref(false)
const msgBody = ref(null)

function renderMd(text) {
  if (!text) return ''
  let html = text
    .replace(/\r\n/g, '\n')                          // 标准化换行

  // 表格渲染：匹配连续的 |...| 行
  html = html.replace(
    /((?:^\|.+\|\n?)+)/gm,
    (match) => {
      const lines = match.trim().split('\n').filter(l => l.includes('|') && !l.match(/^\|[-:| ]+\|$/))
      if (lines.length < 1) return match
      const parseRow = (line) => line.split('|').slice(1, -1).map(c => c.trim())
      const header = parseRow(lines[0])
      let table = '<table class="md-table"><thead><tr>'
      header.forEach(h => { table += `<th>${h}</th>` })
      table += '</tr></thead><tbody>'
      for (let i = 1; i < lines.length; i++) {
        table += '<tr>'
        parseRow(lines[i]).forEach(c => { table += `<td>${c}</td>` })
        table += '</tr>'
      }
      table += '</tbody></table>'
      return table
    }
  )

  html = html
    .replace(/\n\n+/g, '<br><br>')                   // 段落分隔
    .replace(/### (.+)/g, '<h4>$1</h4>')             // 标题
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>') // 粗体
    .replace(/\*(.+?)\*/g, '<em>$1</em>')             // 斜体
    .replace(/^- (.+)/gm, '• $1')                    // 无序列表
    .replace(/\n/g, '<br>')                           // 剩余换行
  return html
}

function openPanel() {
  panelVisible.value = true
  checkAlerts()
}

function goFullPage() {
  panelVisible.value = false
  router.push('/ai/chat')
}

function checkAlerts() {
  getLatestAlertsApi().then(res => {
    hasAlerts.value = (res.data?.highRiskCount || 0) > 0
  }).catch(() => {})
}

function scrollBottom() {
  nextTick(() => {
    if (msgBody.value) {
      msgBody.value.scrollTop = msgBody.value.scrollHeight
    }
  })
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return

  input.value = ''
  messages.value.push({ role: 'user', content: text })
  scrollBottom()

  sending.value = true
  try {
    const res = await chatApi(messages.value.map(m => ({ role: m.role, content: m.content })))
    // 后端返回 ApiResult 格式: { code, message, data: { choices: [...] } }
    const deepseekRes = res?.data
    const choice = deepseekRes?.choices?.[0]
    const reply = choice?.message?.content || '抱歉，我暂时无法回答这个问题。'
    messages.value.push({ role: 'assistant', content: reply })
  } catch {
    messages.value.push({ role: 'assistant', content: '抱歉，AI 服务暂时不可用，请稍后再试。' })
  } finally {
    sending.value = false
    scrollBottom()
  }
}
</script>

<style scoped>
.ai-floating {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
}

.ai-float-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
}

.ai-float-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(64, 158, 255, 0.5);
}

.ai-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f56c6c;
  border: 2px solid #fff;
}

.ai-mini-panel {
  position: absolute;
  right: 0;
  bottom: 72px;
  width: 380px;
  height: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  flex-shrink: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
}

.panel-title .el-icon {
  color: #fff !important;
}

.panel-actions {
  display: flex;
  gap: 4px;
}

.panel-actions .el-button {
  color: rgba(255, 255, 255, 0.85) !important;
}

.panel-actions .el-button:hover {
  color: #fff !important;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  background: #f5f7fa;
}

.panel-welcome {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.panel-welcome ul {
  margin: 8px 0 0 16px;
  padding: 0;
}

.msg-row {
  margin-bottom: 10px;
  display: flex;
}

.msg-row.user {
  justify-content: flex-end;
}

.msg-row.assistant {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 85%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.msg-row.user .msg-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 2px;
}

.msg-row.assistant .msg-bubble {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 2px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

/* 表格样式 */
.msg-bubble :deep(.md-table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin: 4px 0;
}

.msg-bubble :deep(.md-table th),
.msg-bubble :deep(.md-table td) {
  border: 1px solid #dcdfe6;
  padding: 4px 6px;
  text-align: left;
}

.msg-bubble :deep(.md-table th) {
  background: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

.msg-bubble :deep(.md-table td) {
  color: #606266;
}

.msg-bubble.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
}

.msg-bubble.typing .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: blink 1.4s infinite both;
}

.msg-bubble.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.msg-bubble.typing .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

.panel-footer {
  padding: 10px 12px;
  border-top: 1px solid #ebeef5;
  flex-shrink: 0;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.input-field {
  flex: 1;
}

.send-btn {
  flex-shrink: 0;
  transition: all 0.2s ease;
}

/* 有内容时加强 primary 按钮视觉 */
.send-btn.el-button--primary {
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

/* transition */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.25s ease;
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}
</style>
