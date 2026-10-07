<template>
  <div>
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">合集广场</h1>
        <div style="display: flex; gap: 12px;">
          <el-button v-if="userStore.isLoggedIn" type="primary" @click="createDialogVisible = true" icon="Plus">创建合集</el-button>
          <el-button @click="$router.push('/')" icon="ArrowLeft">返回首页</el-button>
        </div>
      </div>
    </div>

    <div class="container" style="padding-top: 20px;">
      <div class="manage-card">
        <el-tabs v-model="activeTab">
          <!-- 合集广场 -->
          <el-tab-pane label="合集广场" name="plaza">
            <div v-loading="loading" class="collection-grid" v-if="collections.length > 0">
              <div v-for="c in collections" :key="c.id" class="collection-card" @click="$router.push(`/collection/${c.id}`)">
                <div class="collection-card-header">
                  <h3 class="collection-name">{{ c.name }}</h3>
                  <el-tag v-if="c.purchased === 1" type="success" size="small">已购买</el-tag>
                  <el-tag v-else-if="c.userId === userStore.userInfo?.userId" type="primary" size="small">我的合集</el-tag>
                </div>
                <p class="collection-desc">{{ c.description || '暂无描述' }}</p>
                <div class="collection-meta">
                  <span class="meta-item">👤 {{ c.creatorNickname || '未知用户' }}</span>
                  <span class="meta-item">📄 {{ c.articleCount || 0 }} 篇文章</span>
                </div>
                <div class="collection-footer">
                  <span class="price" :class="{ free: !Number(c.price) }">{{ Number(c.price) > 0 ? '¥' + Number(c.price).toFixed(2) : '免费' }}</span>
                  <el-button
                    v-if="c.purchased !== 1 && c.userId !== userStore.userInfo?.userId && Number(c.price) > 0"
                    type="primary"
                    size="small"
                    @click.stop="handleBuy(c)"
                  >购买</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else-if="!loading" description="暂无合集，快来创建第一个吧" />
            <div class="pagination-wrapper" v-if="total > 0">
              <el-pagination
                v-model:current-page="page"
                v-model:page-size="size"
                :total="total"
                :page-sizes="[12, 24, 48]"
                layout="total, sizes, prev, pager, next"
                @current-change="loadCollections"
                @size-change="loadCollections"
              />
            </div>
          </el-tab-pane>

          <!-- 我的合集 -->
          <el-tab-pane label="我的合集" name="mine">
            <div v-if="!userStore.isLoggedIn" class="login-tip">
              <router-link to="/login" style="color: #409eff">登录</router-link> 后查看我的合集
            </div>
            <template v-else>
              <div v-loading="mineLoading" class="collection-grid" v-if="myCollections.length > 0">
                <div v-for="c in myCollections" :key="c.id" class="collection-card" @click="$router.push(`/collection/${c.id}`)">
                  <div class="collection-card-header">
                    <h3 class="collection-name">{{ c.name }}</h3>
                    <el-tag type="primary" size="small">我的合集</el-tag>
                  </div>
                  <p class="collection-desc">{{ c.description || '暂无描述' }}</p>
                  <div class="collection-meta">
                    <span class="meta-item">📄 {{ c.articleCount || 0 }} 篇文章</span>
                  </div>
                  <div class="collection-footer">
                    <span class="price" :class="{ free: !Number(c.price) }">{{ Number(c.price) > 0 ? '¥' + Number(c.price).toFixed(2) : '免费' }}</span>
                    <el-button type="primary" size="small" @click.stop="$router.push(`/collection/${c.id}`)">管理</el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else-if="!mineLoading" description="暂无合集，点击右上角创建合集" />
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 创建合集弹窗 -->
    <el-dialog v-model="createDialogVisible" title="创建合集" width="480px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入合集名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="合集描述（选填）" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="createForm.price" :min="0" :max="10000" :precision="2" :step="1" style="width: 100%" />
          <div class="form-tip">0 表示免费合集</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getCollections, getMyCollections, createCollection, purchaseCollection } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('plaza')

// 广场
const collections = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)

// 我的合集
const myCollections = ref([])
const mineLoading = ref(false)

const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({ name: '', description: '', price: 0 })
const createRules = {
  name: [{ required: true, message: '请输入合集名称', trigger: 'blur' }]
}

async function loadCollections() {
  loading.value = true
  try {
    const res = await getCollections({ page: page.value, size: size.value })
    collections.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadMyCollections() {
  mineLoading.value = true
  try {
    const res = await getMyCollections()
    myCollections.value = res.data
  } finally {
    mineLoading.value = false
  }
}

// 切换到"我的合集"tab 时刷新
watch(activeTab, (tab) => {
  if (tab === 'mine' && userStore.isLoggedIn) {
    loadMyCollections()
  }
})

async function handleCreate() {
  await createFormRef.value.validate()
  creating.value = true
  try {
    const res = await createCollection({ ...createForm, price: Number(createForm.price) || 0 })
    createDialogVisible.value = false
    createForm.name = ''
    createForm.description = ''
    createForm.price = 0
    ElMessage.success('创建成功')
    router.push(`/collection/${res.data.id}`)
  } finally {
    creating.value = false
  }
}

async function handleBuy(c) {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    await ElMessageBox.confirm(`确定以 ¥${Number(c.price).toFixed(2)} 购买合集《${c.name}》吗？`, '购买合集', { type: 'warning' })
  } catch (e) {
    return
  }
  const res = await purchaseCollection(c.id)
  await userStore.refreshBalance()
  ElMessage.success(`购买成功，当前余额 ¥${Number(res.data).toFixed(2)}`)
  loadCollections()
}

onMounted(() => {
  loadCollections()
})
</script>

<style scoped>
.manage-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.collection-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.collection-card {
  background: #f9fafc;
  border: 1px solid #f0f2f5;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}
.collection-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.collection-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.collection-name {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.collection-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.collection-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #c0c4cc;
  margin: 8px 0;
}
.collection-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f2f5;
  padding-top: 12px;
}
.price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}
.price.free {
  color: #67c23a;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
.login-tip {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}
</style>
