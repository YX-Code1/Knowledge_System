<template>
  <div>
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="container">
        <div class="header-left">
          <h1 class="page-title">知识库</h1>
          <el-input
            v-model="keyword"
            placeholder="搜索文章..."
            prefix-icon="Search"
            style="width: 300px; margin-left: 20px"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
        <div class="header-right">
          <el-button type="primary" @click="$router.push('/article/create')">
            <el-icon><Plus /></el-icon> 写文章
          </el-button>
          <template v-if="userStore.isLoggedIn">
            <!-- 消息中心入口（红点 = 通知未读 + 私信未读） -->
            <el-badge :value="unreadTotal" :hidden="unreadTotal === 0" :max="99">
              <el-button circle @click="$router.push('/messages')" title="消息中心">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar" icon="UserFilled" />
                <span style="margin-left: 8px">{{ userStore.userInfo?.nickname }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="recharge">充值中心</el-dropdown-item>
                  <el-dropdown-item command="collections">合集广场</el-dropdown-item>
                  <el-dropdown-item command="category" v-if="userStore.isAdmin">分类管理</el-dropdown-item>
                  <el-dropdown-item command="tag" v-if="userStore.isAdmin">标签管理</el-dropdown-item>
                  <el-dropdown-item command="user" v-if="userStore.isSuperAdmin">用户管理</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </div>

    <div class="container" style="display: flex; gap: 20px; padding-top: 20px;">
      <!-- 左侧分类 -->
      <div class="sidebar">
        <div class="sidebar-card">
          <h3 class="sidebar-title">文章分类</h3>
          <div class="category-list">
            <div
              v-for="cat in categoryTree"
              :key="cat.id"
              class="category-item"
              :class="{ active: activeCategory === cat.id }"
              :style="{ paddingLeft: (8 + cat.depth * 16) + 'px' }"
              @click="handleCategorySelect(cat.id)"
            >
              <span
                v-if="cat.hasChildren"
                class="cat-arrow"
                :class="{ expanded: cat.expanded }"
                @click.stop="toggleExpand(cat.id)"
              >
                <el-icon><ArrowRight /></el-icon>
              </span>
              <span v-else class="cat-arrow-placeholder"></span>
              <span class="category-name">{{ cat.name }}</span>
              <span class="category-count">{{ cat.articleCount || 0 }}</span>
            </div>
          </div>
        </div>

        <div class="sidebar-card" style="margin-top: 16px;">
          <h3 class="sidebar-title">热门标签</h3>
          <div class="tag-cloud">
            <el-tag
              v-for="tag in visibleTags"
              :key="tag.id"
              :type="activeTag === tag.id ? 'primary' : 'info'"
              class="tag-item"
              @click="handleTagClick(tag.id)"
            >
              {{ tag.name }}<span v-if="tag.articleCount > 0" class="tag-count">{{ tag.articleCount }}</span>
            </el-tag>
          </div>
          <div v-if="tags.length > HOT_TAG_LIMIT" class="tag-toggle" @click="showAllTags = !showAllTags">
            {{ showAllTags ? '收起' : `全部标签 (${tags.length})` }}
          </div>
        </div>
      </div>

      <!-- 文章列表 -->
      <div class="main-content">
        <!-- 筛选条件栏 -->
        <div class="filter-bar" v-if="hasActiveFilter">
          <span class="filter-label">筛选：</span>
          <el-tag v-if="activeCategory" closable type="primary" class="filter-chip" @close="clearCategory">
            分类：{{ activeCategoryName }}
          </el-tag>
          <el-tag v-if="activeTag" closable type="primary" class="filter-chip" @close="clearTag">
            标签：{{ activeTagName }}
          </el-tag>
          <el-tag v-if="keyword" closable type="primary" class="filter-chip" @close="clearKeyword">
            关键词：{{ keyword }}
          </el-tag>
          <el-button text type="primary" size="small" @click="clearAllFilters">清除全部</el-button>
        </div>

        <div v-loading="loading" class="article-list">
          <div v-for="article in articles" :key="article.id" class="article-card" @click="$router.push(`/article/${article.id}`)">
            <div class="article-card-body">
              <h3 class="article-title">{{ article.title }}</h3>
              <p class="article-summary">{{ article.summary || '暂无摘要' }}</p>
              <div class="article-meta">
                <el-tag size="small" type="success">{{ getCategoryName(article.categoryId) }}</el-tag>
                <!-- 付费/免费统一黄色标签，与详情页保持一致 -->
                <el-tag size="small" type="warning">
                  {{ Number(article.price) > 0 ? '¥' + Number(article.price).toFixed(2) : '免费' }}
                </el-tag>
                <el-tag v-for="tag in article.tags" :key="tag.id" size="small" type="info" style="margin-left: 4px">{{ tag.name }}</el-tag>
                <span class="meta-item"><el-icon><View /></el-icon> {{ article.viewCount }}</span>
                <span class="meta-item"><el-icon><Star /></el-icon> {{ article.likeCount }}</span>
                <span class="meta-item"><el-icon><ChatDotRound /></el-icon> {{ article.commentCount }}</span>
                <span class="meta-item">{{ formatDate(article.createTime) }}</span>
              </div>
            </div>
          </div>

          <el-empty v-if="!loading && articles.length === 0" description="暂无文章" />
        </div>

        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadArticles"
            @size-change="loadArticles"
          />
        </div>
      </div>
    </div>

    <!-- 右下角微信式聊天小窗（登录可见） -->
    <ChatWidget :unread-total="unreadTotal" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getArticles, getCategories, getTags, getNotificationUnreadCount, getMessageUnreadCount } from '@/api'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ChatWidget from '@/components/ChatWidget.vue'

const router = useRouter()
const userStore = useUserStore()

const HOT_TAG_LIMIT = 10

const articles = ref([])
const categories = ref([])
const tags = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const activeCategory = ref(null)
const activeTag = ref(null)
const showAllTags = ref(false)
// 已展开的分类集合（默认全折叠，所以初始为空）
const expandedIds = ref(new Set())

// 分类树（按层级排序，带缩进深度，父分类可折叠/展开）
const categoryTree = computed(() => {
  const childrenMap = new Map()
  categories.value.forEach(c => {
    const key = c.parentId || 0
    if (!childrenMap.has(key)) childrenMap.set(key, [])
    childrenMap.get(key).push(c)
  })
  const result = []
  const walk = (pid, depth) => {
    const children = childrenMap.get(pid) || []
    children.sort((a, b) => a.name.localeCompare(b.name, 'zh'))
    children.forEach(c => {
      const hasChildren = (childrenMap.get(c.id) || []).length > 0
      const expanded = hasChildren && expandedIds.value.has(c.id)
      result.push({ ...c, depth, hasChildren, expanded })
      if (expanded) {
        walk(c.id, depth + 1)
      }
    })
  }
  walk(0, 0)
  return result
})

function toggleExpand(id) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

// 热门标签：默认只显示使用量最高的前10个
const visibleTags = computed(() => showAllTags.value ? tags.value : tags.value.slice(0, HOT_TAG_LIMIT))

const activeCategoryName = computed(() => {
  const cat = categories.value.find(c => c.id === activeCategory.value)
  return cat ? cat.name : ''
})
const activeTagName = computed(() => {
  const tag = tags.value.find(t => t.id === activeTag.value)
  return tag ? tag.name : ''
})
const hasActiveFilter = computed(() => !!(activeCategory.value || activeTag.value || keyword.value))

function getCategoryName(id) {
  const cat = categories.value.find(c => c.id === id)
  return cat ? cat.name : '未分类'
}

function formatDate(date) {
  if (!date) return ''
  return date.substring(0, 10)
}

async function loadArticles() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (activeCategory.value) params.categoryId = activeCategory.value
    if (activeTag.value) params.tagId = activeTag.value
    if (keyword.value) params.keyword = keyword.value
    const res = await getArticles(params)
    articles.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  const res = await getCategories()
  categories.value = res.data
}

async function loadTags() {
  const res = await getTags()
  tags.value = res.data
}

function handleSearch() {
  page.value = 1
  loadArticles()
}

function handleCategorySelect(id) {
  // 点同一分类取消筛选，点其他分类切换（标签筛选保留，可叠加）
  activeCategory.value = activeCategory.value === id ? null : id
  page.value = 1
  loadArticles()
}

function handleTagClick(tagId) {
  // 点同一标签取消筛选，点其他标签切换（分类筛选保留，可叠加）
  activeTag.value = activeTag.value === tagId ? null : tagId
  page.value = 1
  loadArticles()
}

function clearCategory() {
  activeCategory.value = null
  page.value = 1
  loadArticles()
}

function clearTag() {
  activeTag.value = null
  page.value = 1
  loadArticles()
}

function clearKeyword() {
  keyword.value = ''
  page.value = 1
  loadArticles()
}

function clearAllFilters() {
  activeCategory.value = null
  activeTag.value = null
  keyword.value = ''
  page.value = 1
  loadArticles()
}

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'recharge') {
    router.push('/recharge')
  } else if (command === 'collections') {
    router.push('/collections')
  } else if (command === 'category') {
    router.push('/category')
  } else if (command === 'tag') {
    router.push('/tag')
  } else if (command === 'user') {
    router.push('/user')
  }
}

// 消息中心未读合计（通知 + 私信）
const unreadTotal = ref(0)
let unreadTimer = null

async function loadUnreadTotal() {
  if (!userStore.isLoggedIn) return
  try {
    const [noticeRes, messageRes] = await Promise.all([getNotificationUnreadCount(), getMessageUnreadCount()])
    unreadTotal.value = noticeRes.data + messageRes.data
  } catch (e) {
    // 轮询失败静默忽略，下次再试
  }
}

onMounted(() => {
  loadArticles()
  loadCategories()
  loadTags()
  loadUnreadTotal()
  // 每 30 秒轮询一次未读数
  unreadTimer = setInterval(loadUnreadTotal, 30000)
})

onUnmounted(() => {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
})
</script>

<style scoped>
.header-left {
  display: flex;
  align-items: center;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.sidebar {
  width: 240px;
  flex-shrink: 0;
}
.sidebar-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.category-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.category-item {
  display: flex;
  align-items: center;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
}
.category-item:hover {
  background: #f5f7fa;
}
.category-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}
.category-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.category-count {
  font-size: 12px;
  color: #c0c4cc;
  margin-right: 12px;
}
.category-item.active .category-count {
  color: #409eff;
}
.cat-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  margin-right: 4px;
  font-size: 18px;
  color: #606266;
  border-radius: 6px;
  transition: transform 0.2s, background 0.2s, color 0.2s;
}
.cat-arrow:hover {
  background: #e4e7ed;
  color: #409eff;
}
.cat-arrow.expanded {
  transform: rotate(90deg);
  color: #409eff;
}
.cat-arrow-placeholder {
  width: 30px;
  flex-shrink: 0;
}
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
}
.tag-item {
  margin: 4px;
  cursor: pointer;
}
.tag-count {
  margin-left: 2px;
  opacity: 0.7;
}
.tag-toggle {
  margin-top: 8px;
  text-align: center;
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
}
.main-content {
  flex: 1;
  min-width: 0;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border-radius: 12px;
  padding: 10px 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.filter-label {
  font-size: 13px;
  color: #909399;
}
.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.article-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s;
}
.article-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.article-summary {
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #c0c4cc;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}
</style>
