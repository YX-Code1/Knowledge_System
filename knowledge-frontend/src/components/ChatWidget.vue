<template>
  <div v-if="userStore.isLoggedIn">
    <!-- 收起态：右下角圆形入口 -->
    <el-badge v-if="!expanded" :value="unreadTotal" :hidden="unreadTotal === 0" :max="99" class="chat-fab-wrap">
      <el-button class="chat-fab" circle @click="openPanel">
        <el-icon :size="22"><ChatDotRound /></el-icon>
      </el-button>
    </el-badge>

    <!-- 展开态：微信式聊天面板 -->
    <div v-if="expanded" class="chat-panel">
      <div class="chat-panel-header">
        <span class="chat-panel-title">消息</span>
        <div class="chat-panel-actions">
          <el-button text size="small" @click="goMessageCenter">消息中心</el-button>
          <el-button text size="small" @click="expanded = false">收起</el-button>
        </div>
      </div>

      <!-- 联系人列表：AI 助手固定置顶（无论是否有历史会话），下面才是私信会话 -->
      <div v-if="!aiChatActive && activePeerId === null" v-loading="loading" class="chat-contacts">
        <div class="chat-ai-entry" @click="openAiChat">
          <div class="chat-ai-avatar">
            <el-icon :size="20"><MagicStick /></el-icon>
          </div>
          <div class="chat-contact-body">
            <div class="chat-contact-top">
              <span class="chat-contact-name">AI 助手</span>
            </div>
            <span class="chat-contact-preview">基于知识库文章的智能问答</span>
          </div>
        </div>
        <div class="chat-contacts-divider">私信会话</div>
        <div v-for="c in conversations" :key="c.peerId" class="chat-contact" @click="openConversation(c)">
          <el-badge :value="c.unreadCount" :hidden="!c.unreadCount" :max="99">
            <el-avatar :size="36" :src="c.peerAvatar" icon="UserFilled" />
          </el-badge>
          <div class="chat-contact-body">
            <div class="chat-contact-top">
              <span class="chat-contact-name">{{ c.peerNickname }}</span>
              <span class="chat-contact-time">{{ formatTime(c.lastTime) }}</span>
            </div>
            <span class="chat-contact-preview">{{ c.lastContent }}</span>
          </div>
        </div>
        <el-empty v-if="!loading && conversations.length === 0" description="暂无会话" :image-size="50" />
      </div>

      <!-- AI 问答视图 -->
      <div v-else-if="aiChatActive" class="chat-conversation">
        <div class="chat-conv-header">
          <el-button text size="small" icon="ArrowLeft" @click="backToList">返回</el-button>
          <span class="chat-conv-title">AI 助手</span>
          <span style="width: 48px"></span>
        </div>
        <div class="chat-messages" ref="aiMsgListRef">
          <div v-if="aiMessages.length === 0" class="ai-welcome">
            <p>👋 我是知识库 AI 助手</p>
            <p>我会检索知识库文章来回答你的问题</p>
          </div>
          <div v-for="(m, i) in aiMessages" :key="i" class="chat-msg" :class="{ mine: m.role === 'user' }">
            <div class="chat-bubble" :class="{ 'ai-markdown': m.role === 'assistant' }">
              <div v-if="m.role === 'assistant'" v-html="renderMarkdown(m.content)"></div>
              <template v-else>{{ m.content }}</template>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <el-input
            v-model="aiQuestion"
            type="textarea"
            :rows="2"
            maxlength="500"
            placeholder="向知识库提问..."
            :disabled="aiSending"
            @keydown.enter.exact.prevent="sendAi"
          />
          <el-button v-if="!aiSending" type="primary" size="small" @click="sendAi">发送</el-button>
          <el-button v-else type="danger" size="small" @click="stopAi">停止</el-button>
        </div>
      </div>

      <!-- 私信会话视图 -->
      <div v-else class="chat-conversation">
        <div class="chat-conv-header">
          <el-button text size="small" icon="ArrowLeft" @click="backToList">返回</el-button>
          <span class="chat-conv-title">{{ activePeerNickname }}</span>
          <el-button text size="small" :type="activeBlocked ? 'success' : 'danger'" @click="toggleBlock">
            {{ activeBlocked ? '解除拉黑' : '拉黑' }}
          </el-button>
        </div>
        <div v-loading="msgLoading" class="chat-messages" ref="msgListRef">
          <div v-for="m in messages" :key="m.id" class="chat-msg" :class="{ mine: m.senderId === myUserId }">
            <div class="chat-bubble">{{ m.content }}</div>
          </div>
          <el-empty v-if="!msgLoading && messages.length === 0" description="暂无消息，打个招呼吧" :image-size="50" />
        </div>
        <div class="chat-input">
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="2"
            maxlength="500"
            placeholder="输入消息..."
            @keydown.enter.exact.prevent="send"
          />
          <el-button type="primary" size="small" :loading="sending" @click="send">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getConversations, getConversationMessages, sendMessage, blockUser, unblockUser } from '@/api'
import { useUserStore } from '@/store/user'
import { renderMarkdown } from '@/utils/markdown'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  /** 未读合计（通知+私信），由 Home 轮询传入 */
  unreadTotal: { type: Number, default: 0 }
})

const router = useRouter()
const userStore = useUserStore()
const myUserId = computed(() => userStore.userInfo?.userId)

const expanded = ref(false)
const conversations = ref([])
const loading = ref(false)
const activePeerId = ref(null)
const activePeerNickname = ref('')
const activeBlocked = ref(false)
const messages = ref([])
const msgLoading = ref(false)
const newMessage = ref('')
const sending = ref(false)
const msgListRef = ref()
let refreshTimer = null

// AI 问答状态（会话记录仅存组件内存，刷新即清空——入口永远置顶不受影响）
const aiChatActive = ref(false)
const aiMessages = ref([])
const aiQuestion = ref('')
const aiSending = ref(false)
const aiMsgListRef = ref()
let aiAbortController = null

function formatTime(date) {
  if (!date) return ''
  return date.substring(0, 16)
}

function openPanel() {
  expanded.value = true
  loadConversations()
}

async function loadConversations() {
  loading.value = true
  try {
    const res = await getConversations()
    conversations.value = res.data
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function openAiChat() {
  aiChatActive.value = true
  activePeerId.value = null
}

async function openConversation(c) {
  aiChatActive.value = false
  activePeerId.value = c.peerId
  activePeerNickname.value = c.peerNickname
  activeBlocked.value = c.blocked === 1
  messages.value = []
  msgLoading.value = true
  try {
    const res = await getConversationMessages(c.peerId, { page: 1, size: 50 })
    messages.value = res.data.records
    scrollToBottom()
  } finally {
    msgLoading.value = false
  }
  // 打开会话即已读，刷新列表的未读数
  loadConversations()
}

function backToList() {
  stopAi()
  aiChatActive.value = false
  activePeerId.value = null
  messages.value = []
  loadConversations()
}

async function send() {
  const content = newMessage.value.trim()
  if (!content) {
    ElMessage.warning('请输入消息内容')
    return
  }
  sending.value = true
  try {
    const res = await sendMessage({ receiverId: activePeerId.value, content })
    messages.value.push(res.data)
    newMessage.value = ''
    scrollToBottom()
    loadConversations()
  } finally {
    sending.value = false
  }
}

async function toggleBlock() {
  if (activeBlocked.value) {
    await unblockUser(activePeerId.value)
    activeBlocked.value = false
    ElMessage.success('已解除拉黑')
  } else {
    try {
      await ElMessageBox.confirm(`确定拉黑「${activePeerNickname.value}」吗？拉黑后双方无法互发私信。`, '拉黑', { type: 'warning' })
    } catch (e) {
      return
    }
    await blockUser(activePeerId.value)
    activeBlocked.value = true
    ElMessage.success('已拉黑')
  }
  loadConversations()
}

function goMessageCenter() {
  expanded.value = false
  router.push('/messages')
}

function scrollToBottom() {
  nextTick(() => {
    if (msgListRef.value) {
      msgListRef.value.scrollTop = msgListRef.value.scrollHeight
    }
  })
}

function scrollAiToBottom() {
  nextTick(() => {
    if (aiMsgListRef.value) {
      aiMsgListRef.value.scrollTop = aiMsgListRef.value.scrollHeight
    }
  })
}

// 给后端带最近几轮对话（排除末尾的 AI 空占位和刚发送的问题本身，后端自己截断）
function buildAiHistory() {
  return aiMessages.value.slice(-8, -2).map(m => ({ role: m.role, content: m.content }))
}

async function sendAi() {
  const q = aiQuestion.value.trim()
  if (!q || aiSending.value) return
  aiQuestion.value = ''
  aiMessages.value.push({ role: 'user', content: q })
  const aiMsg = { role: 'assistant', content: '' }
  aiMessages.value.push(aiMsg)
  aiSending.value = true
  scrollAiToBottom()

  aiAbortController = new AbortController()
  try {
    // EventSource 不支持 POST/自定义请求头，这里用 fetch + ReadableStream 手动解析 SSE
    const res = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${sessionStorage.getItem('token')}`
      },
      body: JSON.stringify({ question: q, history: buildAiHistory() }),
      signal: aiAbortController.signal
    })
    // 非 SSE 响应说明后端返回了 JSON 业务错误（如限流、索引未建立）
    const contentType = res.headers.get('content-type') || ''
    if (!res.ok || !contentType.includes('text/event-stream')) {
      aiMessages.value.pop()
      const errData = await res.json().catch(() => null)
      ElMessage.error(errData?.message || '请求失败')
      return
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // SSE 事件以空行分隔，data 行是 JSON 字符串
      const events = buffer.split('\n\n')
      buffer = events.pop()
      for (const event of events) {
        for (const line of event.split('\n')) {
          if (!line.startsWith('data:')) continue
          let payload
          try {
            payload = JSON.parse(line.slice(5).trim())
          } catch (e) {
            continue
          }
          if (payload.type === 'token') {
            aiMsg.content += payload.content
            scrollAiToBottom()
          } else if (payload.type === 'error') {
            if (!aiMsg.content) aiMessages.value.pop()
            ElMessage.error(payload.message || 'AI 服务调用失败')
          }
        }
      }
    }
    if (!aiMsg.content) {
      aiMessages.value.pop()
      ElMessage.warning('AI 未返回内容，请重试')
    }
  } catch (e) {
    if (e.name === 'AbortError') {
      // 用户主动停止，保留已生成的部分
    } else {
      aiMessages.value.pop()
      ElMessage.error('网络异常，请稍后重试')
    }
  } finally {
    aiSending.value = false
    aiAbortController = null
    scrollAiToBottom()
  }
}

function stopAi() {
  if (aiAbortController) aiAbortController.abort()
}

// 面板展开期间每 30 秒刷新会话列表（未读数/预览）；收起时中断进行中的 AI 流
watch(expanded, (val) => {
  if (val) {
    refreshTimer = setInterval(loadConversations, 30000)
  } else {
    stopAi()
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }
})

onUnmounted(() => {
  stopAi()
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
.chat-fab-wrap {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
}
.chat-fab {
  width: 52px;
  height: 52px;
  font-size: 22px;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
}
.chat-panel {
  position: fixed;
  right: 24px;
  bottom: 88px;
  z-index: 1000;
  width: 340px;
  height: 480px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #409eff;
  color: #fff;
}
.chat-panel-title {
  font-size: 15px;
  font-weight: 600;
}
.chat-panel-actions :deep(.el-button) {
  color: #fff;
}
.chat-contacts {
  flex: 1;
  overflow-y: auto;
}
/* AI 助手：固定置顶入口，与私信会话视觉区分 */
.chat-ai-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #ecf5ff, #f7fbff);
}
.chat-ai-entry:hover {
  background: #e6f1ff;
}
.chat-ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.chat-contacts-divider {
  padding: 6px 16px;
  font-size: 12px;
  color: #c0c4cc;
  background: #fafafa;
}
.chat-contact {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
}
.chat-contact:hover {
  background: #f5f7fa;
}
.chat-contact-body {
  flex: 1;
  min-width: 0;
}
.chat-contact-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}
.chat-contact-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.chat-contact-time {
  font-size: 11px;
  color: #c0c4cc;
}
.chat-contact-preview {
  font-size: 12px;
  color: #909399;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-conversation {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.chat-conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-bottom: 1px solid #f0f0f0;
}
.chat-conv-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f5f7fa;
}
.chat-msg {
  display: flex;
  margin-bottom: 10px;
}
.chat-msg.mine {
  justify-content: flex-end;
}
.chat-bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 10px;
  background: #fff;
  font-size: 13px;
  color: #303133;
  word-break: break-all;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.chat-msg.mine .chat-bubble {
  background: #409eff;
  color: #fff;
}
.ai-welcome {
  text-align: center;
  color: #909399;
  padding-top: 60px;
  font-size: 13px;
  line-height: 1.8;
}
/* AI 回答的 markdown 紧凑样式（小面板适配） */
.ai-markdown {
  word-break: break-word;
}
.ai-markdown :deep(p) {
  margin: 4px 0;
}
.ai-markdown :deep(h1),
.ai-markdown :deep(h2),
.ai-markdown :deep(h3) {
  margin: 6px 0 3px;
  font-size: 14px;
}
.ai-markdown :deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 8px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 12px;
}
.ai-markdown :deep(code) {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
.ai-markdown :deep(ul),
.ai-markdown :deep(ol) {
  padding-left: 16px;
  margin: 4px 0;
}
.ai-markdown :deep(blockquote) {
  border-left: 3px solid #dcdfe6;
  margin: 4px 0;
  padding-left: 8px;
  color: #909399;
}
.chat-input {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 10px;
  border-top: 1px solid #f0f0f0;
}
.chat-input .el-input {
  flex: 1;
}
</style>
