<template>
  <div>
    <!-- 顶部导航：标题 / 居中搜索 / 操作区 -->
    <div class="page-header home-header">
      <div class="container header-container">
        <div class="header-left">
          <h1 class="page-title">知识库</h1>
        </div>
        <div class="header-search">
          <el-input
            v-model="keyword"
            class="search-input"
            placeholder="搜索文章标题或摘要..."
            prefix-icon="Search"
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
                <span class="user-name" style="margin-left: 8px">{{ userStore.userInfo?.nickname }}</span>
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

    <div class="container content-layout">
      <!-- 左侧分类/标签：桌面端固定跟随滚动 -->
      <aside class="sidebar">
        <HomeSidebar
          :category-tree="categoryTree"
          :active-category="activeCategory"
          :tags="tags"
          :visible-tags="visibleTags"
          :active-tag="activeTag"
          :show-all-tags="showAllTags"
          :tag-limit="HOT_TAG_LIMIT"
          @category-select="handleCategorySelect"
          @toggle-expand="toggleExpand"
          @tag-click="handleTagClick"
          @toggle-show-all="showAllTags = !showAllTags"
        />
      </aside>

      <!-- 移动端：筛选抽屉（复用同一份侧栏组件） -->
      <el-drawer v-model="mobileFilterOpen" title="筛选" direction="rtl" size="300px">
        <HomeSidebar
          :category-tree="categoryTree"
          :active-category="activeCategory"
          :tags="tags"
          :visible-tags="visibleTags"
          :active-tag="activeTag"
          :show-all-tags="showAllTags"
          :tag-limit="HOT_TAG_LIMIT"
          @category-select="handleMobileCategorySelect"
          @toggle-expand="toggleExpand"
          @tag-click="handleMobileTagClick"
          @toggle-show-all="showAllTags = !showAllTags"
        />
        <template #footer>
          <div class="drawer-footer">
            <el-button @click="handleMobileClearAll">清除全部</el-button>
            <el-button type="primary" @click="mobileFilterOpen = false">完成</el-button>
          </div>
        </template>
      </el-drawer>

      <!-- 文章列表 -->
      <div class="main-content">
        <!-- 排序栏 -->
        <div class="toolbar">
          <div class="sort-tabs">
            <span
              v-for="tab in sortTabs"
              :key="tab.key"
              class="sort-tab"
              :class="{ active: activeSort === tab.key }"
              @click="handleSortChange(tab.key)"
            >
              {{ tab.label }}
            </span>
          </div>
          <div class="toolbar-right">
            <span class="result-count" v-if="!loading">共 {{ total }} 篇</span>
            <el-button class="mobile-filter-btn" @click="mobileFilterOpen = true">
              <el-icon><Filter /></el-icon> 筛选
            </el-button>
          </div>
        </div>

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

        <!-- 首次加载骨架屏 -->
        <div v-if="loading && articles.length === 0" class="article-list">
          <div v-for="i in 5" :key="i" class="article-card skeleton-card">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="h3" style="width: 42%" />
                <el-skeleton-item variant="text" style="width: 92%; margin-top: 14px" />
                <el-skeleton-item variant="text" style="width: 68%; margin-top: 8px" />
                <div class="skeleton-meta">
                  <el-skeleton-item variant="text" style="width: 64px" />
                  <el-skeleton-item variant="text" style="width: 64px" />
                  <el-skeleton-item variant="text" style="width: 140px; margin-left: auto" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>

        <div v-else v-loading="loading" class="article-list">
          <div v-for="article in articles" :key="article.id" class="article-card" @click="$router.push(`/article/${article.id}`)">
            <div class="article-card-body">
              <h3 class="article-title">{{ article.title }}</h3>
              <p class="article-summary">{{ article.summary || '暂无摘要' }}</p>
              <div class="article-meta">
                <div class="meta-tags">
                  <el-tag size="small" type="success">{{ getCategoryName(article.categoryId) }}</el-tag>
                  <!-- 付费/免费统一黄色标签，与详情页保持一致 -->
                  <el-tag size="small" type="warning">
                    {{ Number(article.price) > 0 ? '¥' + Number(article.price).toFixed(2) : '免费' }}
                  </el-tag>
                  <el-tag v-for="tag in article.tags" :key="tag.id" size="small" type="info">{{ tag.name }}</el-tag>
                </div>
                <div class="meta-stats">
                  <span class="meta-item"><el-icon><View /></el-icon> {{ article.viewCount || 0 }}</span>
                  <span class="meta-item"><el-icon><Star /></el-icon> {{ article.likeCount || 0 }}</span>
                  <span class="meta-item"><el-icon><ChatDotRound /></el-icon> {{ article.commentCount || 0 }}</span>
                  <span class="meta-item meta-date">{{ formatDate(article.createTime) }}</span>
                </div>
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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getArticles, getCategories, getTags, getNotificationUnreadCount, getMessageUnreadCount } from '@/api'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import HomeSidebar from '@/components/HomeSidebar.vue'

const router = useRouter()
const userStore = useUserStore()

const HOT_TAG_LIMIT = 10
const sortTabs = [
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '最热' },
  { key: 'free', label: '免费' }
]

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
const activeSort = ref('latest')
const showAllTags = ref(false)
const mobileFilterOpen = ref(false)
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
    const params = {
      page: page.value,
      size: pageSize.value,
      sort: activeSort.value === 'hot' ? 'hot' : 'latest'
    }
    if (activeSort.value === 'free') params.freeOnly = true
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

function handleSortChange(key) {
  if (activeSort.value === key) return
  activeSort.value = key
  page.value = 1
  loadArticles()
}

function handleCategorySelect(id) {
  // 点同一分类取消筛选，点“全部文章”（id 为 null）直接清空分类
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

// 移动端抽屉选择后自动收起
function handleMobileCategorySelect(id) {
  handleCategorySelect(id)
  mobileFilterOpen.value = false
}

function handleMobileTagClick(id) {
  handleTagClick(id)
  mobileFilterOpen.value = false
}

function handleMobileClearAll() {
  clearAllFilters()
  mobileFilterOpen.value = false
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
.header-container {
  display: flex;
  align-items: center;
  gap: 20px;
}
.header-left {
  flex-shrink: 0;
}
.header-search {
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
}
.search-input {
  width: 100%;
  max-width: 420px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.content-layout {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding-top: 20px;
}
.sidebar {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  scrollbar-width: thin;
}
.main-content {
  flex: 1;
  min-width: 0;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 10px 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.sort-tabs {
  display: flex;
  gap: 2px;
}
.sort-tab {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
}
.sort-tab:hover {
  background: #f5f7fa;
  color: #409eff;
}
.sort-tab.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.result-count {
  font-size: 13px;
  color: #909399;
}
.mobile-filter-btn {
  display: none;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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
  gap: 14px;
}
.article-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 22px;
  border: 1px solid transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.25s;
}
.article-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
  border-color: #d9ecff;
}
.article-title {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
  color: #303133;
  margin-bottom: 6px;
  transition: color 0.2s;
}
.article-card:hover .article-title {
  color: #409eff;
}
.article-summary {
  font-size: 13.5px;
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
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.meta-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  min-width: 0;
}
.meta-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
  font-size: 13px;
  color: #a8abb2;
  white-space: nowrap;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.meta-date {
  color: #c0c4cc;
}
.skeleton-card {
  cursor: default;
}
.skeleton-card:hover {
  transform: none;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.skeleton-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}

@media (max-width: 992px) {
  .sidebar {
    width: 210px;
  }
  .search-input {
    max-width: 320px;
  }
}

@media (max-width: 768px) {
  .home-header .header-container {
    flex-wrap: wrap;
    gap: 12px;
  }
  .header-left {
    flex: 1;
  }
  .header-search {
    order: 3;
    flex-basis: 100%;
  }
  .search-input {
    max-width: none;
  }
  .header-right {
    gap: 8px;
  }
  .user-name {
    display: none;
  }
  .content-layout {
    gap: 0;
    padding-top: 12px;
  }
  .sidebar {
    display: none;
  }
  .mobile-filter-btn {
    display: inline-flex;
  }
  .result-count {
    display: none;
  }
  .article-card {
    padding: 16px;
  }
  .article-title {
    font-size: 16px;
  }
  .article-summary {
    font-size: 13px;
  }
  .meta-stats {
    width: 100%;
    margin-left: 0;
  }
  .pagination-wrapper {
    padding: 12px 8px;
    overflow-x: auto;
  }
  .pagination-wrapper :deep(.el-pagination__sizes),
  .pagination-wrapper :deep(.el-pagination__total) {
    display: none;
  }
}
</style>
