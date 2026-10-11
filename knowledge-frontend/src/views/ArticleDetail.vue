<template>
  <div>
    <div class="page-header">
      <div class="container">
        <el-button @click="$router.push('/')" icon="ArrowLeft">返回</el-button>
        <div class="header-actions">
          <el-button @click="handleLike" :type="liked ? 'warning' : 'default'" :icon="liked ? 'StarFilled' : 'Star'">
            {{ article.likeCount || 0 }} 点赞
          </el-button>
          <el-button @click="handleFavorite" :type="favorited ? 'danger' : 'default'" :icon="favorited ? 'CollectionFilled' : 'Collection'">
            {{ favorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button v-if="userStore.isLoggedIn && !isAuthor" @click="openMessageDialog" icon="Message">私信作者</el-button>
          <el-button v-if="isAuthor" type="primary" @click="$router.push(`/article/edit/${articleId}`)" icon="Edit">编辑</el-button>
          <el-button v-if="isAuthor" type="danger" @click="handleDelete" icon="Delete">删除</el-button>
        </div>
      </div>
    </div>

    <div class="container" style="display: flex; gap: 20px; padding-top: 20px;">
      <div class="article-content" v-loading="loading">
        <template v-if="article.id">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-info">
            <el-tag type="success">{{ categoryName }}</el-tag>
            <!-- 付费/免费统一黄色标签，增强视觉一致性 -->
            <el-tag type="warning">
              {{ Number(article.price) > 0 ? '付费 ¥' + Number(article.price).toFixed(2) : '免费' }}
            </el-tag>
            <el-tag v-for="tag in article.tags" :key="tag.id" type="info" style="margin-left: 4px">{{ tag.name }}</el-tag>
            <span class="info-item"><el-icon><View /></el-icon> {{ article.viewCount }} 阅读</span>
            <span class="info-item">{{ formatDate(article.createTime) }}</span>
          </div>
          <!-- 付费未购买：正文锁定，显示购买面板；购买后渲染正文 -->
          <div v-if="article.locked" class="article-locked">
            <el-icon :size="48" color="#c0c4cc"><Lock /></el-icon>
            <p class="locked-text">本文为付费内容，购买后即可阅读全文</p>
            <p class="locked-price">价格：¥{{ Number(article.price).toFixed(2) }}</p>
            <p v-if="userStore.isLoggedIn" class="locked-balance">
              当前余额：¥{{ Number(userStore.userInfo?.balance || 0).toFixed(2) }}
            </p>
            <el-button v-if="userStore.isLoggedIn" type="danger" size="large" :loading="buying" @click="handleBuy">
              立即购买
            </el-button>
            <el-button v-else type="primary" size="large" @click="$router.push('/login')">登录后购买</el-button>
            <el-button v-if="userStore.isLoggedIn" text type="primary" @click="$router.push('/recharge')">余额不足？去充值</el-button>
          </div>
          <div v-else class="article-body" v-html="renderedContent"></div>
        </template>
      </div>

      <div class="sidebar" style="width: 300px; flex-shrink: 0;">
        <div class="sidebar-card">
          <h3 class="sidebar-title">评论区 ({{ comments.length }})</h3>
          <div v-if="userStore.isLoggedIn" style="margin-bottom: 16px;">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="写下你的评论..."
              maxlength="500"
              show-word-limit
            />
            <el-button type="primary" style="margin-top: 8px; width: 100%" @click="submitComment" :loading="commentLoading">发表评论</el-button>
          </div>
          <div v-else style="text-align: center; color: #909399; padding: 12px 0;">
            <router-link to="/login" style="color: #409eff">登录</router-link> 后参与评论
          </div>
          <div class="comment-list">
            <div v-for="c in comments" :key="c.id" class="comment-item">
              <div class="comment-content">{{ c.content }}</div>
              <div class="comment-meta">
                <span>{{ formatDate(c.createTime) }}</span>
                <el-button v-if="userStore.userInfo?.userId === c.userId" type="danger" text size="small" @click="deleteComment(c.id)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="60" />
          </div>
        </div>
      </div>
    </div>

    <!-- 私信作者：聊天对话框（含历史记录，可持续对话） -->
    <el-dialog v-model="messageDialogVisible" title="私信作者" width="520px">
      <div class="chat-box">
        <div v-loading="messageLoading" class="chat-history" ref="chatHistoryRef">
          <div v-for="m in chatMessages" :key="m.id" class="chat-msg-row" :class="{ mine: m.senderId === userStore.userInfo?.userId }">
            <div class="chat-msg-bubble">{{ m.content }}</div>
          </div>
          <el-empty v-if="!messageLoading && chatMessages.length === 0" description="暂无消息，和作者打个招呼吧" :image-size="50" />
        </div>
        <div class="chat-send-row">
          <el-input
            v-model="messageContent"
            type="textarea"
            :rows="2"
            maxlength="500"
            placeholder="输入消息..."
            @keydown.enter.exact.prevent="sendPrivateMessage"
          />
          <el-button type="primary" :loading="messageSending" @click="sendPrivateMessage">发送</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticle, likeArticle, toggleFavorite, checkFavorite, getComments, createComment, deleteComment as deleteCommentApi, deleteArticle, getCategories, sendMessage, getConversationMessages, purchaseArticle } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const articleId = ref(Number(route.params.id))

const article = ref({})
const comments = ref([])
const loading = ref(false)
const liked = ref(false)
const favorited = ref(false)
const commentContent = ref('')
const commentLoading = ref(false)
const messageDialogVisible = ref(false)
const messageContent = ref('')
const messageSending = ref(false)
const messageLoading = ref(false)
const chatMessages = ref([])
const chatHistoryRef = ref()
const buying = ref(false)
const categories = ref([])
const categoryName = computed(() => {
  const cat = categories.value.find(c => c.id === article.value.categoryId)
  return cat ? cat.name : '未分类'
})

// marked + DOMPurify 的消毒渲染逻辑抽到 utils/markdown.js，与 AI 问答页共用
const renderedContent = computed(() => renderMarkdown(article.value.content))

const isAuthor = computed(() => userStore.userInfo?.userId === article.value.authorId)

function formatDate(date) {
  if (!date) return ''
  return date.substring(0, 10)
}

async function loadArticle() {
  loading.value = true
  try {
    const [articleRes, catRes] = await Promise.all([getArticle(articleId.value), getCategories()])
    article.value = articleRes.data
    categories.value = catRes.data
    liked.value = !!articleRes.data.liked
    if (userStore.isLoggedIn) {
      const favRes = await checkFavorite(articleId.value)
      favorited.value = favRes.data
    }
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  const res = await getComments(articleId.value)
  comments.value = res.data
}

async function handleLike() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  // 点赞/取消点赞切换
  await likeArticle(articleId.value)
  liked.value = !liked.value
  article.value.likeCount = (article.value.likeCount || 0) + (liked.value ? 1 : -1)
  ElMessage.success(liked.value ? '点赞成功' : '已取消点赞')
}

async function handleFavorite() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  await toggleFavorite(articleId.value)
  favorited.value = !favorited.value
  ElMessage.success(favorited.value ? '收藏成功' : '取消收藏')
}

async function submitComment() {
  if (!commentContent.value.trim()) { ElMessage.warning('请输入评论内容'); return }
  commentLoading.value = true
  try {
    await createComment({ articleId: articleId.value, content: commentContent.value })
    commentContent.value = ''
    ElMessage.success('评论成功')
    loadComments()
    loadArticle()
  } finally {
    commentLoading.value = false
  }
}

async function deleteComment(id) {
  await deleteCommentApi(id)
  ElMessage.success('删除成功')
  loadComments()
  loadArticle()
}

async function handleDelete() {
  await ElMessageBox.confirm('确定删除这篇文章吗？', '提示', { type: 'warning' })
  await deleteArticle(articleId.value)
  ElMessage.success('删除成功')
  router.push('/')
}

async function openMessageDialog() {
  messageDialogVisible.value = true
  messageLoading.value = true
  chatMessages.value = []
  try {
    // 加载与作者的历史聊天记录
    const res = await getConversationMessages(article.value.authorId, { page: 1, size: 50 })
    chatMessages.value = res.data.records
    scrollChatBottom()
  } finally {
    messageLoading.value = false
  }
}

async function sendPrivateMessage() {
  const content = messageContent.value.trim()
  if (!content) { ElMessage.warning('请输入消息内容'); return }
  messageSending.value = true
  try {
    const res = await sendMessage({ receiverId: article.value.authorId, content })
    chatMessages.value.push(res.data)
    messageContent.value = ''
    scrollChatBottom()
  } finally {
    messageSending.value = false
  }
}

function scrollChatBottom() {
  nextTick(() => {
    if (chatHistoryRef.value) {
      chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
    }
  })
}

async function handleBuy() {
  try {
    await ElMessageBox.confirm(
      `确定以 ¥${Number(article.value.price).toFixed(2)} 购买本文吗？`,
      '购买文章',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  buying.value = true
  try {
    const res = await purchaseArticle(articleId.value)
    await userStore.refreshBalance()
    ElMessage.success(`购买成功，当前余额 ¥${Number(res.data).toFixed(2)}`)
    loadArticle()
  } finally {
    buying.value = false
  }
}

onMounted(() => {
  loadArticle()
  loadComments()
})
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 8px;
}
.article-content {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.article-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 16px;
}
.article-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  margin-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #909399;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.article-body {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
}
.article-locked {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
}
.locked-text {
  font-size: 15px;
  color: #606266;
}
.locked-price {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}
.locked-balance {
  font-size: 13px;
  color: #909399;
}
.chat-box {
  display: flex;
  flex-direction: column;
}
.chat-history {
  height: 300px;
  overflow-y: auto;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}
.chat-msg-row {
  display: flex;
  margin-bottom: 10px;
}
.chat-msg-row.mine {
  justify-content: flex-end;
}
.chat-msg-bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 10px;
  background: #fff;
  font-size: 14px;
  color: #303133;
  word-break: break-all;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.chat-msg-row.mine .chat-msg-bubble {
  background: #409eff;
  color: #fff;
}
.chat-send-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-top: 12px;
}
.chat-send-row .el-input {
  flex: 1;
}
.article-body :deep(h1), .article-body :deep(h2), .article-body :deep(h3) {
  margin: 24px 0 12px;
  font-weight: 600;
}
.article-body :deep(p) { margin-bottom: 16px; }
.article-body :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 14px;
}
.article-body :deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
}
.article-body :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}
.article-body :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding-left: 16px;
  color: #606266;
  margin: 16px 0;
}
.article-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 16px 0;
}
.sidebar-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}
.comment-list {
  max-height: 400px;
  overflow-y: auto;
}
.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.comment-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 6px;
}
.comment-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
