<template>
  <div>
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">消息中心</h1>
        <el-button @click="$router.back()" icon="ArrowLeft">返回</el-button>
      </div>
    </div>

    <div class="container" style="padding-top: 20px;">
      <div class="manage-card">
        <el-tabs v-model="activeTab">
          <!-- 系统通知 -->
          <el-tab-pane label="系统通知" name="notifications">
            <div class="tab-toolbar">
              <el-button v-if="notifications.length > 0" size="small" @click="handleMarkAllRead">全部已读</el-button>
            </div>
            <div v-loading="noticeLoading">
              <div
                v-for="n in notifications"
                :key="n.id"
                class="notice-item"
                :class="{ unread: n.isRead === 0 }"
                @click="handleNoticeClick(n)"
              >
                <el-tag v-if="n.isRead === 0" type="danger" size="small" class="notice-tag">未读</el-tag>
                <el-tag v-else type="info" size="small" class="notice-tag">已读</el-tag>
                <div class="notice-content">{{ n.content }}</div>
                <div class="notice-time">{{ formatDateTime(n.createTime) }}</div>
              </div>
              <el-empty v-if="!noticeLoading && notifications.length === 0" description="暂无通知" />
            </div>
            <div class="pagination-wrapper" v-if="noticeTotal > 0">
              <el-pagination
                v-model:current-page="noticePage"
                v-model:page-size="noticePageSize"
                :total="noticeTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @current-change="loadNotifications"
                @size-change="loadNotifications"
              />
            </div>
          </el-tab-pane>

          <!-- 私信 -->
          <el-tab-pane label="私信" name="messages">
            <!-- 会话列表 -->
            <div v-if="activePeerId === null" v-loading="conversationLoading">
              <div
                v-for="c in conversations"
                :key="c.peerId"
                class="conversation-item"
                @click="openConversation(c)"
              >
                <el-avatar :size="40" :src="c.peerAvatar" icon="UserFilled" />
                <div class="conversation-body">
                  <div class="conversation-top">
                    <span class="conversation-name">{{ c.peerNickname }}</span>
                    <span class="conversation-time">{{ formatDateTime(c.lastTime) }}</span>
                  </div>
                  <div class="conversation-bottom">
                    <span class="conversation-preview">{{ c.lastContent }}</span>
                    <el-badge v-if="c.unreadCount > 0" :value="c.unreadCount" :max="99" />
                  </div>
                </div>
              </div>
              <el-empty v-if="!conversationLoading && conversations.length === 0" description="暂无会话" />
            </div>

            <!-- 会话详情 -->
            <div v-else class="conversation-panel">
              <div class="conversation-panel-header">
                <el-button text icon="ArrowLeft" @click="backToConversations">返回</el-button>
                <span class="conversation-panel-title">{{ activePeerNickname }}</span>
                <el-button text size="small" :type="activeBlocked ? 'success' : 'danger'" @click="toggleBlock">
                  {{ activeBlocked ? '解除拉黑' : '拉黑' }}
                </el-button>
              </div>
              <div class="message-list" ref="messageListRef">
                <div v-if="hasMoreMessages" class="load-more-wrapper">
                  <el-button text size="small" :loading="messageLoading" @click="loadMoreMessages">加载更早消息</el-button>
                </div>
                <div v-for="m in messages" :key="m.id" class="message-row" :class="{ mine: m.senderId === myUserId }">
                  <div class="message-bubble">{{ m.content }}</div>
                  <div class="message-time">{{ formatDateTime(m.createTime) }}</div>
                </div>
                <el-empty v-if="!messageLoading && messages.length === 0" description="暂无消息，发一条吧" :image-size="60" />
              </div>
              <div class="message-input">
                <el-input
                  v-model="newMessage"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  placeholder="输入消息..."
                  @keydown.enter.exact.prevent="sendNewMessage"
                />
                <el-button type="primary" @click="sendNewMessage" :loading="sending">发送</el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import {
  getNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  getConversations,
  getConversationMessages,
  sendMessage,
  blockUser,
  unblockUser
} from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const myUserId = computed(() => userStore.userInfo?.userId)

const activeTab = ref('notifications')

// 系统通知
const notifications = ref([])
const noticeLoading = ref(false)
const noticePage = ref(1)
const noticePageSize = ref(10)
const noticeTotal = ref(0)

// 私信会话
const conversations = ref([])
const conversationLoading = ref(false)
const activePeerId = ref(null)
const activePeerNickname = ref('')
const activeBlocked = ref(false)
const messages = ref([])
const messagePage = ref(1)
const messagePageSize = ref(50)
const messageTotal = ref(0)
const messageLoading = ref(false)
const newMessage = ref('')
const sending = ref(false)
const messageListRef = ref()

const hasMoreMessages = computed(() => messagePage.value * messagePageSize.value < messageTotal.value)

function formatDateTime(date) {
  if (!date) return ''
  return date.substring(0, 16)
}

async function loadNotifications() {
  noticeLoading.value = true
  try {
    const res = await getNotifications({ page: noticePage.value, size: noticePageSize.value })
    notifications.value = res.data.records
    noticeTotal.value = res.data.total
  } finally {
    noticeLoading.value = false
  }
}

async function loadConversations() {
  conversationLoading.value = true
  try {
    const res = await getConversations()
    conversations.value = res.data
  } finally {
    conversationLoading.value = false
  }
}

async function handleNoticeClick(n) {
  if (n.isRead === 0) {
    await markNotificationRead(n.id)
    n.isRead = 1
  }
}

async function handleMarkAllRead() {
  await markAllNotificationsRead()
  notifications.value.forEach(n => { n.isRead = 1 })
  ElMessage.success('已全部标记为已读')
}

async function openConversation(c) {
  activePeerId.value = c.peerId
  activePeerNickname.value = c.peerNickname
  activeBlocked.value = c.blocked === 1
  messages.value = []
  messagePage.value = 1
  await loadMessages()
  // 打开会话即已读，刷新会话列表的未读数
  await loadConversations()
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

async function loadMessages() {
  messageLoading.value = true
  try {
    const res = await getConversationMessages(activePeerId.value, { page: messagePage.value, size: messagePageSize.value })
    // 加载更早消息时前置拼接，首次加载直接赋值（后端返回正序）
    messages.value = messagePage.value === 1 ? res.data.records : [...res.data.records, ...messages.value]
    messageTotal.value = res.data.total
    scrollToBottom()
  } finally {
    messageLoading.value = false
  }
}

async function loadMoreMessages() {
  messagePage.value += 1
  await loadMessages()
}

async function sendNewMessage() {
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
    await loadConversations()
  } finally {
    sending.value = false
  }
}

function backToConversations() {
  activePeerId.value = null
  messages.value = []
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

onMounted(() => {
  loadNotifications()
  loadConversations()
})
</script>

<style scoped>
.manage-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.notice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.notice-item:hover {
  background: #f5f7fa;
}
.notice-item.unread {
  background: #fdf6ec;
}
.notice-tag {
  flex-shrink: 0;
}
.notice-content {
  flex: 1;
  font-size: 14px;
  color: #303133;
}
.notice-time {
  flex-shrink: 0;
  font-size: 12px;
  color: #c0c4cc;
}
.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 8px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.conversation-item:hover {
  background: #f5f7fa;
}
.conversation-body {
  flex: 1;
  min-width: 0;
}
.conversation-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.conversation-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.conversation-time {
  font-size: 12px;
  color: #c0c4cc;
}
.conversation-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.conversation-preview {
  flex: 1;
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conversation-panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}
.conversation-panel-header .conversation-panel-title {
  flex: 1;
}
.conversation-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.message-list {
  height: 400px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 8px;
}
.load-more-wrapper {
  text-align: center;
  padding-bottom: 8px;
}
.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 12px;
}
.message-row.mine {
  align-items: flex-end;
}
.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  font-size: 14px;
  color: #303133;
  word-break: break-all;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.message-row.mine .message-bubble {
  background: #409eff;
  color: #fff;
}
.message-time {
  margin-top: 4px;
  font-size: 12px;
  color: #c0c4cc;
}
.message-input {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  align-items: flex-end;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
