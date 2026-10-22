<template>
  <div>
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">写文章</h1>
        <el-button @click="$router.back()">取消</el-button>
      </div>
    </div>
    <div class="container" style="padding-top: 20px;">
      <div class="form-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入文章标题" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="摘要">
            <div style="width: 100%">
              <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入文章摘要（选填）" maxlength="500" show-word-limit />
              <el-button size="small" style="margin-top: 8px" :loading="summaryLoading" @click="handleGenerateSummary">AI 生成摘要</el-button>
            </div>
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <!-- 树形下拉：父分类只用于分组，只有末级分类可选 -->
            <el-tree-select
              v-model="form.categoryId"
              :data="categorySelectTree"
              placeholder="请选择分类"
              default-expand-all
              style="width: 100%"
            />
            <div class="category-tip">仅末级分类可选，带子分类的分类只用于分组</div>
          </el-form-item>
          <el-form-item label="标签">
            <div style="width: 100%">
              <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%">
                <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
              </el-select>
              <el-button size="small" style="margin-top: 8px" :loading="tagsLoading" @click="handleRecommendTags">AI 推荐标签</el-button>
            </div>
          </el-form-item>
          <el-form-item label="售价">
            <el-input-number v-model="form.price" :min="0" :max="10000" :precision="2" :step="1" style="width: 100%" />
            <div class="form-tip">0 表示免费文章，设置价格后其他用户需购买才能阅读</div>
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <el-input v-model="form.content" type="textarea" :rows="20" placeholder="支持 Markdown 格式" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" @click="handleSubmit" :loading="loading">发布文章</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createArticle, getCategories, getTags, generateSummary, recommendTags } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const summaryLoading = ref(false)
const tagsLoading = ref(false)
const categories = ref([])
const tags = ref([])

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

// 分类下拉树：父分类只用于分组（disabled），只有末级分类可以选中
const categorySelectTree = computed(() => {
  const childrenMap = new Map()
  categories.value.forEach(c => {
    const key = c.parentId || 0
    if (!childrenMap.has(key)) childrenMap.set(key, [])
    childrenMap.get(key).push(c)
  })
  const build = (pid) => (childrenMap.get(pid) || [])
    .sort((a, b) => a.name.localeCompare(b.name, 'zh'))
    .map(c => {
      const children = build(c.id)
      return {
        value: c.id,
        label: c.name,
        disabled: children.length > 0,
        children
      }
    })
  return build(0)
})

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    // 清空输入框时 price 为 undefined，归一化为 0（免费）
    const res = await createArticle({ ...form, price: Number(form.price) || 0 })
    ElMessage.success('发布成功')
    router.push(`/article/${res.data.id}`)
  } finally {
    loading.value = false
  }
}

// 校验标题和内容已填写，否则 AI 调用没有意义
function ensureTitleContent() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请先填写标题和内容')
    return false
  }
  return true
}

async function handleGenerateSummary() {
  if (!ensureTitleContent()) return
  summaryLoading.value = true
  try {
    const res = await generateSummary({ title: form.title, content: form.content })
    form.summary = res.data.summary
    ElMessage.success('摘要已生成')
  } catch (e) {
    // 拦截器已提示
  } finally {
    summaryLoading.value = false
  }
}

async function handleRecommendTags() {
  if (!ensureTitleContent()) return
  tagsLoading.value = true
  try {
    const res = await recommendTags({ title: form.title, content: form.content })
    if (!res.data.length) {
      ElMessage.warning('没有匹配的标签，请手动选择')
      return
    }
    form.tagIds = res.data.map(t => t.id)
    ElMessage.success('已推荐标签')
  } catch (e) {
    // 拦截器已提示
  } finally {
    tagsLoading.value = false
  }
}

onMounted(async () => {
  const [catRes, tagRes] = await Promise.all([getCategories(), getTags()])
  categories.value = catRes.data
  tags.value = tagRes.data
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
.category-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 6px;
}
</style>
