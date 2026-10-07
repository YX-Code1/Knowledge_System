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

      <!-- 联系人列表 -->
      <div v-if="activePeerId === null" v-loading="loading" class="chat-contacts">
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

      <!-- 会话视图 -->
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

async function openConversation(c) {
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

// 面板展开期间每 30 秒刷新会话列表（未读数/预览）
watch(expanded, (val) => {
  if (val) {
    refreshTimer = setInterval(loadConversations, 30000)
  } else if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})

onUnmounted(() => {
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
