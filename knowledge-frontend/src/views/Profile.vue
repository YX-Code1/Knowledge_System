<template>
  <div>
    <div class="page-header">
      <div class="container">
        <div class="header-left">
          <el-button @click="$router.push('/')" icon="ArrowLeft">返回</el-button>
          <h1 class="page-title">个人中心</h1>
        </div>
      </div>
    </div>
    <div class="container" style="padding-top: 20px;">
      <div class="profile-card">
        <div class="profile-header">
          <el-avatar :size="80" :src="profile?.avatar || userStore.userInfo?.avatar" icon="UserFilled" />
          <div class="profile-info">
            <h2>{{ profile?.nickname || userStore.userInfo?.nickname || '未设置昵称' }}</h2>
            <p>@{{ userStore.userInfo?.username }}</p>
            <el-tag :type="userStore.isAdmin ? 'danger' : 'success'">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</el-tag>
          </div>
          <div class="balance-box">
            <div class="balance-label">账户余额</div>
            <div class="balance-value">¥{{ Number(userStore.userInfo?.balance || 0).toFixed(2) }}</div>
            <el-button type="primary" size="small" @click="$router.push('/recharge')">去充值</el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab">
          <el-tab-pane label="我的收藏" name="favorites">
            <div v-loading="favLoading">
              <div v-for="fav in favorites" :key="fav.id" class="fav-item" @click="$router.push(`/article/${fav.articleId}`)">
                <span class="fav-title" :title="fav.articleTitle">{{ truncateTitle(fav.articleTitle) }}</span>
                <span class="fav-date">{{ formatDate(fav.createTime) }}</span>
              </div>
              <el-empty v-if="!favLoading && favorites.length === 0" description="暂无收藏" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="我的合集" name="collections">
            <div v-loading="collectionLoading">
              <div v-for="c in myCollections" :key="c.id" class="fav-item" @click="$router.push(`/collection/${c.id}`)">
                <span class="fav-title" :title="c.name">{{ c.name }}（{{ c.articleCount || 0 }} 篇）</span>
                <span class="fav-date">{{ Number(c.price) > 0 ? '¥' + Number(c.price).toFixed(2) : '免费' }}</span>
              </div>
              <el-empty v-if="!collectionLoading && myCollections.length === 0" description="暂无合集，去合集广场创建吧" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="账号信息" name="account">
            <div class="account-toolbar">
              <el-button type="primary" size="small" icon="Edit" @click="openEditDialog">编辑资料</el-button>
            </div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
              <el-descriptions-item label="昵称">{{ profile?.nickname || '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ profile?.email || '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="头像">{{ profile?.avatar || '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="角色">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</el-descriptions-item>
              <el-descriptions-item label="余额">¥{{ Number(profile?.balance ?? userStore.userInfo?.balance ?? 0).toFixed(2) }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑资料" width="440px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-input v-model="editForm.avatar" placeholder="头像图片 URL（选填）" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyFavorites, getMyCollections, getProfile, updateProfile } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('favorites')
const favorites = ref([])
const favLoading = ref(false)
const myCollections = ref([])
const collectionLoading = ref(false)
const profile = ref(null)
const editDialogVisible = ref(false)
const savingProfile = ref(false)
const editFormRef = ref()
const editForm = reactive({ nickname: '', email: '', avatar: '' })
const editRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function formatDate(date) {
  if (!date) return ''
  return date.substring(0, 10)
}

// 标题最多展示15字，超出截断加省略号
function truncateTitle(title) {
  if (!title) return ''
  return title.length > 15 ? title.substring(0, 15) + '…' : title
}

function openEditDialog() {
  editForm.nickname = profile.value?.nickname || ''
  editForm.email = profile.value?.email || ''
  editForm.avatar = profile.value?.avatar || ''
  editDialogVisible.value = true
}

async function handleSaveProfile() {
  await editFormRef.value.validate()
  savingProfile.value = true
  try {
    const res = await updateProfile({ ...editForm })
    profile.value = res.data
    // 同步 store 与 sessionStorage，全站昵称/头像立即生效
    userStore.updateUserInfo({ nickname: res.data.nickname, avatar: res.data.avatar, email: res.data.email })
    editDialogVisible.value = false
    ElMessage.success('资料已更新')
  } finally {
    savingProfile.value = false
  }
}

onMounted(async () => {
  // 拉取完整资料（含邮箱等登录接口不返回的字段），并同步 store 保证全局昵称最新
  getProfile().then(res => {
    profile.value = res.data
    userStore.updateUserInfo({ nickname: res.data.nickname, avatar: res.data.avatar, email: res.data.email, balance: res.data.balance })
  }).catch(() => {})
  // 余额以服务端为准
  userStore.refreshBalance().catch(() => {})
  favLoading.value = true
  try {
    const res = await getMyFavorites()
    favorites.value = res.data
  } finally {
    favLoading.value = false
  }
  collectionLoading.value = true
  try {
    const res = await getMyCollections()
    myCollections.value = res.data
  } finally {
    collectionLoading.value = false
  }
})
</script>

<style scoped>
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}
.profile-info h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.profile-info p {
  color: #909399;
  margin-bottom: 8px;
}
.balance-box {
  margin-left: auto;
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.balance-label {
  font-size: 13px;
  color: #909399;
}
.balance-value {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}
.account-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.fav-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.fav-item:hover {
  background: #f5f7fa;
}
.fav-title {
  font-size: 14px;
  color: #303133;
}
.fav-date {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
