<template>
  <div>
    <div class="page-header">
      <div class="container">
        <el-button @click="$router.back()" icon="ArrowLeft">返回</el-button>
        <div class="header-actions">
          <el-button v-if="isOwn" type="primary" icon="Edit" @click="editDialogVisible = true">编辑合集</el-button>
          <el-button v-if="isOwn" type="danger" icon="Delete" @click="handleDeleteCollection">删除合集</el-button>
          <template v-if="isOwn">
            <el-button type="success" icon="Plus" @click="openAddDialog">添加文章</el-button>
          </template>
          <template v-else-if="Number(collection.price) > 0">
            <el-tag v-if="collection.purchased === 1" type="success" size="large">已购买</el-tag>
            <el-button v-else type="danger" :loading="buying" @click="handleBuy">购买合集 ¥{{ Number(collection.price).toFixed(2) }}</el-button>
          </template>
        </div>
      </div>
    </div>

    <div class="container" style="padding-top: 20px;" v-loading="loading">
      <div class="collection-info">
        <h2 class="collection-title">{{ collection.name }}</h2>
        <p class="collection-desc">{{ collection.description || '暂无描述' }}</p>
        <div class="collection-meta">
          <el-tag type="info" size="small">👤 {{ collection.creatorNickname || '未知用户' }}</el-tag>
          <el-tag size="small" :type="Number(collection.price) > 0 ? 'danger' : 'success'">
            {{ Number(collection.price) > 0 ? '¥' + Number(collection.price).toFixed(2) : '免费' }}
          </el-tag>
          <el-tag type="info" size="small">📄 {{ collection.articleCount || 0 }} 篇文章</el-tag>
        </div>
      </div>

      <!-- 文章列表 -->
      <div class="manage-card">
        <h3 class="list-title">合集内容</h3>
        <div v-for="a in articles" :key="a.id" class="article-item">
          <div class="article-main" @click="$router.push(`/article/${a.id}`)">
            <span class="article-title">{{ a.title }}</span>
            <span class="article-summary">{{ a.summary || '暂无摘要' }}</span>
          </div>
          <div class="article-side">
            <span class="article-price" :class="{ free: !Number(a.price) }">{{ Number(a.price) > 0 ? '¥' + Number(a.price).toFixed(2) : '免费' }}</span>
            <el-button v-if="isOwn" type="danger" text size="small" @click="handleRemoveArticle(a.id)">移除</el-button>
          </div>
        </div>
        <el-empty v-if="articles.length === 0" description="合集还没有文章" :image-size="60" />
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑合集" width="480px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="editForm.price" :min="0" :max="10000" :precision="2" :step="1" style="width: 100%" />
          <div class="form-tip">0 表示免费合集</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加文章弹窗 -->
    <el-dialog v-model="addDialogVisible" title="添加文章到合集" width="480px">
      <el-select v-model="selectedArticleId" placeholder="从我的文章中选择" style="width: 100%" filterable>
        <el-option
          v-for="a in addableArticles"
          :key="a.id"
          :label="a.title"
          :value="a.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAddArticle">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getCollection,
  updateCollection,
  deleteCollection,
  addCollectionArticle,
  removeCollectionArticle,
  purchaseCollection,
  getMyArticles
} from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collectionId = Number(route.params.id)

const collection = ref({})
const articles = ref([])
const loading = ref(false)

const isOwn = computed(() => !!collection.value.own)

const editDialogVisible = ref(false)
const saving = ref(false)
const editFormRef = ref()
const editForm = reactive({ name: '', description: '', price: 0 })
const editRules = { name: [{ required: true, message: '请输入合集名称', trigger: 'blur' }] }

const addDialogVisible = ref(false)
const adding = ref(false)
const selectedArticleId = ref(null)
const myArticles = ref([])
// 我的文章里排除已在合集中的
const addableArticles = computed(() =>
  myArticles.value.filter(a => !articles.value.some(x => x.id === a.id))
)

const buying = ref(false)

async function loadCollection() {
  loading.value = true
  try {
    const res = await getCollection(collectionId)
    collection.value = res.data
    articles.value = res.data.articles || []
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  await editFormRef.value.validate()
  saving.value = true
  try {
    await updateCollection(collectionId, { ...editForm, price: Number(editForm.price) || 0 })
    editDialogVisible.value = false
    ElMessage.success('保存成功')
    loadCollection()
  } finally {
    saving.value = false
  }
}

async function handleDeleteCollection() {
  try {
    await ElMessageBox.confirm('确定删除该合集吗？已购买的买家将失去合集权益。', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteCollection(collectionId)
  ElMessage.success('删除成功')
  router.push('/collections')
}

async function openAddDialog() {
  addDialogVisible.value = true
  selectedArticleId.value = null
  if (myArticles.value.length === 0) {
    const res = await getMyArticles({ page: 1, size: 100 })
    myArticles.value = res.data.records
  }
}

async function handleAddArticle() {
  if (!selectedArticleId.value) {
    ElMessage.warning('请选择文章')
    return
  }
  adding.value = true
  try {
    await addCollectionArticle(collectionId, { articleId: selectedArticleId.value })
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    loadCollection()
  } finally {
    adding.value = false
  }
}

async function handleRemoveArticle(articleId) {
  await removeCollectionArticle(collectionId, articleId)
  ElMessage.success('移除成功')
  loadCollection()
}

async function handleBuy() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定以 ¥${Number(collection.value.price).toFixed(2)} 购买合集《${collection.value.name}》吗？`,
      '购买合集',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  buying.value = true
  try {
    const res = await purchaseCollection(collectionId)
    await userStore.refreshBalance()
    ElMessage.success(`购买成功，当前余额 ¥${Number(res.data).toFixed(2)}`)
    loadCollection()
  } finally {
    buying.value = false
  }
}

onMounted(() => {
  loadCollection()
})
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 8px;
}
.collection-info {
  background: #fff;
  border-radius: 12px;
  padding: 24px 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}
.collection-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.collection-desc {
  font-size: 14px;
  color: #909399;
  margin: 8px 0 12px;
  line-height: 1.6;
}
.collection-meta {
  display: flex;
  gap: 8px;
}
.manage-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.list-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.article-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 8px;
  border-bottom: 1px solid #f0f0f0;
}
.article-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
}
.article-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.article-main:hover .article-title {
  color: #409eff;
}
.article-summary {
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.article-side {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  margin-left: 16px;
}
.article-price {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}
.article-price.free {
  color: #67c23a;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
