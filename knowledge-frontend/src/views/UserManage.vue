<template>
  <div>
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">用户管理</h1>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>
    <div class="container" style="padding-top: 20px;">
      <div class="manage-card">
        <div class="manage-header">
          <h3>用户列表</h3>
          <el-input
            v-model="keyword"
            placeholder="搜索用户名/昵称..."
            prefix-icon="Search"
            style="width: 260px"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
        <el-table :data="users" v-loading="loading" style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="用户名" width="150" />
          <el-table-column prop="nickname" label="昵称" width="150" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column label="角色" width="160">
            <template #default="scope">
              <el-select
                :model-value="scope.row.role"
                size="small"
                :disabled="scope.row.role === 'SUPER_ADMIN' || scope.row.id === userStore.userInfo?.userId"
                @change="(val) => handleRoleChange(scope.row, val)"
              >
                <el-option label="普通用户" value="USER" />
                <el-option label="管理员" value="ADMIN" />
                <el-option v-if="scope.row.role === 'SUPER_ADMIN'" label="超级管理员" value="SUPER_ADMIN" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'danger' : 'success'" size="small">
                {{ scope.row.status === 1 ? '黑名单' : '正常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="余额" width="110">
            <template #default="scope">¥{{ Number(scope.row.balance || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210">
            <template #default="scope">
              <el-button type="primary" text @click="openAdjustDialog(scope.row)">调整余额</el-button>
              <el-button
                v-if="scope.row.role !== 'SUPER_ADMIN' && scope.row.id !== userStore.userInfo?.userId"
                :type="scope.row.status === 1 ? 'success' : 'danger'"
                text
                @click="handleToggleStatus(scope.row)"
              >
                {{ scope.row.status === 1 ? '解除拉黑' : '拉黑' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadUsers"
            @size-change="loadUsers"
          />
        </div>
      </div>
    </div>

    <!-- 调整余额弹窗 -->
    <el-dialog v-model="adjustDialogVisible" :title="`调整余额 - ${adjustTarget?.username}`" width="440px">
      <el-form label-width="80px">
        <el-form-item label="调整金额">
          <el-input-number v-model="adjustChange" :min="-10000" :max="10000" :precision="2" :step="1" style="width: 100%" />
          <div class="adjust-tip">正数为增加，负数为扣减；调整后余额不能为负</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="adjustRemark" placeholder="备注（选填）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="handleAdjustBalance">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUsers, updateUserRole, updateUserStatus, adjustUserBalance } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const users = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')

async function loadUsers() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await getUsers(params)
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadUsers()
}

async function handleRoleChange(row, role) {
  if (!role || role === row.role) return
  await ElMessageBox.confirm(
    `确定将用户「${row.username}」的角色改为${role === 'ADMIN' ? '管理员' : '普通用户'}吗？`,
    '提示',
    { type: 'warning' }
  )
  await updateUserRole(row.id, { role })
  ElMessage.success('角色已更新')
  loadUsers()
}

async function handleToggleStatus(row) {
  const toBlack = row.status !== 1
  await ElMessageBox.confirm(
    toBlack ? `确定将用户「${row.username}」拉入黑名单吗？拉黑后将无法登录和访问。` : `确定解除用户「${row.username}」的黑名单吗？`,
    '提示',
    { type: 'warning' }
  )
  await updateUserStatus(row.id, { status: toBlack ? 1 : 0 })
  ElMessage.success(toBlack ? '已拉黑' : '已解除拉黑')
  loadUsers()
}

// 调整余额
const adjustDialogVisible = ref(false)
const adjustTarget = ref(null)
const adjustChange = ref(0)
const adjustRemark = ref('')
const adjusting = ref(false)

function openAdjustDialog(row) {
  adjustTarget.value = row
  adjustChange.value = 0
  adjustRemark.value = ''
  adjustDialogVisible.value = true
}

async function handleAdjustBalance() {
  if (!adjustChange.value) {
    ElMessage.warning('请输入调整金额')
    return
  }
  adjusting.value = true
  try {
    await adjustUserBalance(adjustTarget.value.id, {
      change: adjustChange.value,
      remark: adjustRemark.value || undefined
    })
    ElMessage.success('余额已调整')
    adjustDialogVisible.value = false
    loadUsers()
  } finally {
    adjusting.value = false
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.manage-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.manage-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
.adjust-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
