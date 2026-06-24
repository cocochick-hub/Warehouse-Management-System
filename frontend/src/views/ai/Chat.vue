<template>
  <PageContainer title="AI 仓库管理员">
    <template #actions>
      <el-button @click="clearChat" :disabled="messages.length === 0">
        <el-icon><Delete /></el-icon>清空对话
      </el-button>
    </template>

    <div class="chat-container">
      <!-- 消息列表 -->
      <div class="chat-messages" ref="msgBody">
        <div v-if="messages.length === 0" class="chat-welcome">
          <div class="welcome-icon">
            <el-icon :size="48" color="#409eff"><ChatDotRound /></el-icon>
          </div>
          <h3>AI 仓库管理员</h3>
          <p>我是您的智能仓库管理助手，基于实时数据分析库存状态。</p>
          <div class="quick-prompts">
            <span class="prompt-label">快速提问：</span>
            <el-tag
              v-for="q in quickQuestions"
              :key="q"
              class="prompt-tag"
              @click="sendQuick(q)"
              type="info"
            >
              {{ q }}
            </el-tag>
          </div>
        </div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['message-row', msg.role]"
        >
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'assistant'" :size="32" :icon="ChatDotRound" />
            <el-avatar v-else :size="32" :icon="UserFilled" />
          </div>
          <div class="message-content">
            <div class="message-role">{{ msg.role === 'assistant' ? 'AI 助手' : '我' }}</div>
            <div class="message-bubble" v-html="renderContent(msg.content)" />
            <div v-if="msg.toolCalls" class="message-tools">
              <el-tag
                v-for="tc in msg.toolCalls"
                :key="tc.id"
                size="small"
                type="info"
                class="tool-tag"
              >
                已查询: {{ tc.name }}
              </el-tag>
            </div>
          </div>
        </div>

        <div v-if="sending" class="message-row assistant">
          <div class="message-avatar">
            <el-avatar :size="32" :icon="ChatDotRound" />
          </div>
          <div class="message-content">
            <div class="message-role">AI 助手</div>
            <div class="message-bubble typing-indicator">
              <span class="typing-dot" />
              <span class="typing-dot" />
              <span class="typing-dot" />
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chat-input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          placeholder="输入您的问题，例如：当前哪些物料有缺货风险？"
          :disabled="sending"
          resize="none"
          @keyup.enter.exact.native="send"
          @keyup.enter.shift.exact.native="() => {}"
        />
        <div class="input-actions">
          <span class="input-hint">Enter 发送 · Shift+Enter 换行</span>
          <el-button type="primary" :disabled="sending || !input.trim()" @click="send">
            <el-icon><Promotion /></el-icon>发送
          </el-button>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, UserFilled } from '@element-plus/icons-vue'
import { chatApi } from '@/api/ai'
import PageContainer from '@/components/PageContainer.vue'

const route = useRoute()

const input = ref('')
const messages = ref([])
const sending = ref(false)
const msgBody = ref(null)

const quickQuestions = [
  '当前哪些物料有缺货风险？',
  '有没有呆滞物料需要关注？',
  '库存整体健康状况如何？',
  '帮我分析控制器模块的消耗趋势'
]

function renderContent(text) {
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
    .replace(/### (.+)/g, '<h4>$1</h4>')             // 三级标题
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>') // 粗体
    .replace(/\*(.+?)\*/g, '<em>$1</em>')             // 斜体
    .replace(/^- (.+)/gm, '• $1')                    // 无序列表
    .replace(/\n/g, '<br>')                           // 剩余换行
  return html
}

function scrollToBottom() {
  nextTick(() => {
    if (msgBody.value) {
      msgBody.value.scrollTop = msgBody.value.scrollHeight
    }
  })
}

async function sendQuick(question) {
  input.value = question
  await send()
}

async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return

  input.value = ''
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()

  sending.value = true
  try {
    // 构建发送给 API 的消息列表（只传 role + content）
    const apiMessages = messages.value.map(m => ({
      role: m.role,
      content: m.content
    }))

    const res = await chatApi(apiMessages)

    // 后端返回 ApiResult 格式: { code, message, data }
    // data 是 DeepSeek 的原始响应: { choices: [{ message: { content, tool_calls } }] }
    const deepseekRes = res?.data
    let reply = ''
    let toolCalls = null

    if (deepseekRes?.choices?.[0]?.message) {
      const msg = deepseekRes.choices[0].message
      reply = msg.content || ''
      toolCalls = msg.tool_calls || null
    }

    if (!reply && !toolCalls) {
      reply = '抱歉，我暂时无法回答这个问题，请稍后再试。'
    }

    if (!reply && toolCalls) {
      reply = '正在分析数据...'
    }

    messages.value.push({
      role: 'assistant',
      content: reply || '分析完成，请查看结果。',
      toolCalls: toolCalls ? toolCalls.map(tc => ({
        id: tc.id,
        name: tc.function?.name || 'unknown'
      })) : null
    })
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，AI 服务暂时不可用，请稍后再试。'
    })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
}

// 支持从 Dashboard 预警卡片跳转时带入物料号
onMounted(() => {
  const material = route.query.material
  if (material) {
    input.value = `帮我分析物料 ${material} 的库存状况`
    nextTick(() => send())
  }
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 200px);
  min-height: 500px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #f5f7fa;
  border-radius: 8px 8px 0 0;
  border: 1px solid #ebeef5;
  border-bottom: none;
}

/* Welcome */
.chat-welcome {
  text-align: center;
  padding: 48px 20px;
}

.welcome-icon {
  margin-bottom: 16px;
}

.chat-welcome h3 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #303133;
}

.chat-welcome p {
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.prompt-label {
  font-size: 13px;
  color: #909399;
}

.prompt-tag {
  cursor: pointer;
  transition: transform 0.15s;
}

.prompt-tag:hover {
  transform: translateY(-1px);
}

/* Messages */
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
}

.message-role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.message-row.user .message-role {
  text-align: right;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-row.assistant .message-bubble {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 2px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.message-row.user .message-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 2px;
}

.message-tools {
  margin-top: 4px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tool-tag {
  font-size: 11px;
}

/* Typing */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 18px;
}

.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: typing-blink 1.4s infinite both;
}

.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing-blink {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* Input */
.chat-input-area {
  border: 1px solid #ebeef5;
  border-radius: 0 0 8px 8px;
  padding: 12px 16px;
  background: #fff;
  flex-shrink: 0;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hint {
  font-size: 12px;
  color: #c0c4cc;
}

/* 表格样式 */
.message-bubble :deep(.md-table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin: 6px 0;
}

.message-bubble :deep(.md-table th),
.message-bubble :deep(.md-table td) {
  border: 1px solid #dcdfe6;
  padding: 5px 10px;
  text-align: left;
}

.message-bubble :deep(.md-table th) {
  background: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

.message-bubble :deep(.md-table td) {
  color: #606266;
}
</style>
