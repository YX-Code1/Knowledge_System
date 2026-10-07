<template>
  <div>
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">编辑文章</h1>
        <el-button @click="$router.back()">取消</el-button>
      </div>
    </div>
    <div class="container" style="padding-top: 20px;">
      <div class="form-card" v-loading="loading">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入文章标题" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入文章摘要（选填）" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <!-- 只能选择叶子分类（没有子分类的分类），非叶子分类禁用 -->
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="isLeaf(cat.id) ? cat.name : `${cat.name}（含子分类）`"
                :value="cat.id"
                :disabled="!isLeaf(cat.id)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%">
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="售价">
            <el-input-number v-model="form.price" :min="0" :max="10000" :precision="2" :step="1" style="width: 100%" />
            <div class="form-tip">0 表示免费文章，设置价格后其他用户需购买才能阅读</div>
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <el-input v-model="form.content" type="textarea" :rows="20" placeholder="支持 Markdown 格式" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" @click="handleSubmit" :loading="saving">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticle, updateArticle, getCategories, getTags } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const categories = ref([])
const tags = ref([])
const articleId = Number(route.params.id)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  categoryId: null,
  tagIds: [],
  price: 0
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

// 被用作父分类的分类ID集合（非叶子分类不可选）
const parentIdSet = computed(() => new Set(categories.value.map(c => c.parentId)))
function isLeaf(id) {
  return !parentIdSet.value.has(id)
}

async function handleSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await updateArticle(articleId, { ...form, price: Number(form.price) || 0 })
    ElMessage.success('保存成功')
    router.push(`/article/${articleId}`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [articleRes, catRes, tagRes] = await Promise.all([
      getArticle(articleId),
      getCategories(),
      getTags()
    ])
    const article = articleRes.data
    form.title = article.title
    form.summary = article.summary || ''
    form.content = article.content
    form.categoryId = article.categoryId
    form.tagIds = (article.tags || []).map(t => t.id)
    form.price = Number(article.price) || 0
    categories.value = catRes.data
    tags.value = tagRes.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
  line-height: 32px;
}
</style>
