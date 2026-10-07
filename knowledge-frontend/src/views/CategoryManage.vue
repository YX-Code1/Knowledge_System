<template>
  <div>
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">分类管理</h1>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>
    <div class="container" style="padding-top: 20px;">
      <div class="manage-card">
        <div class="manage-header">
          <h3>分类列表</h3>
          <el-button type="primary" @click="showAddDialog">新增分类</el-button>
        </div>
        <el-table :data="categoryTree" style="width: 100%" row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
          <el-table-column prop="name" label="分类名称" />
          <el-table-column label="文章数(含子分类)" width="140">
            <template #default="scope">
              {{ scope.row.articleCount || 0 }}
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

    <!-- 新增/编辑分类弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父分类" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择父分类" style="width: 100%">
            <el-option label="顶级分类" :value="0" />
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const categories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const editId = ref(null)

// 树形结构（供 el-table 层级展示）
const categoryTree = computed(() => {
  const childrenMap = new Map()
  categories.value.forEach(c => {
    const key = c.parentId || 0
    if (!childrenMap.has(key)) childrenMap.set(key, [])
    childrenMap.get(key).push(c)
  })
  const build = (pid) => {
    const children = childrenMap.get(pid) || []
    children.sort((a, b) => a.name.localeCompare(b.name, 'zh'))
    return children.map(c => ({ ...c, children: build(c.id) }))
  }
  return build(0)
})

const form = reactive({ name: '', parentId: 0 })
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

async function loadCategories() {
  const res = await getCategories()
  categories.value = res.data
}

function showAddDialog() {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.parentId = 0
  dialogVisible.value = true
}

function showEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.parentId = row.parentId || 0
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCategory(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createCategory(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadCategories()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该分类吗？', '提示', { type: 'warning' })
  await deleteCategory(id)
  ElMessage.success('删除成功')
  loadCategories()
}

onMounted(loadCategories)
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
