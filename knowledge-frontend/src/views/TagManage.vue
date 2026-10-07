<template>
  <div>
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">标签管理</h1>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>
    <div class="container" style="padding-top: 20px;">
      <div class="manage-card">
        <div class="manage-header">
          <h3>标签列表</h3>
          <el-button type="primary" @click="showAddDialog">新增标签</el-button>
        </div>
        <el-table :data="tags" style="width: 100%">
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="name" label="标签名称" />
          <el-table-column label="文章数" width="100" sortable :sort-method="(a, b) => (a.articleCount || 0) - (b.articleCount || 0)">
            <template #default="scope">
              {{ scope.row.articleCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="200">
            <template #default="scope">
              {{ (scope.row.createTime || '').substring(0, 19) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button type="primary" text @click="showEditDialog(scope.row)">编辑</el-button>
              <el-button type="danger" text @click="handleDelete(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 新增/编辑标签弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑标签' : '新增标签'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTags, createTag, updateTag, deleteTag } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tags = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref(null)

const form = reactive({ name: '' })
const rules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

async function loadTags() {
  const res = await getTags()
  tags.value = res.data
}

function showAddDialog() {
  isEdit.value = false
  editId.value = null
  form.name = ''
  dialogVisible.value = true
}

function showEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateTag(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createTag(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTags()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('删除后，文章上挂的该标签也会被移除，确定删除吗？', '提示', { type: 'warning' })
  await deleteTag(id)
  ElMessage.success('删除成功')
  loadTags()
}

onMounted(loadTags)
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
</style>
