<template>
  <div class="sidebar-content">
    <div class="sidebar-card">
      <!-- 分类和标签合并为一个卡片，用 Tab 切换，避免侧栏过长 -->
      <div class="sidebar-tabs">
        <div
          class="sidebar-tab"
          :class="{ active: activePanel === 'category' }"
          @click="activePanel = 'category'"
        >
          文章分类
        </div>
        <div
          class="sidebar-tab"
          :class="{ active: activePanel === 'tag' }"
          @click="activePanel = 'tag'"
        >
          热门标签
        </div>
      </div>

      <div class="sidebar-panels">
        <div v-show="activePanel === 'category'" class="category-list">
          <!-- 清除分类筛选的入口，避免用户只能再点一次当前分类 -->
          <div
            class="category-item"
            :class="{ active: !activeCategory }"
            @click="$emit('category-select', null)"
          >
            <span class="cat-arrow-placeholder"></span>
            <span class="category-name">全部文章</span>
          </div>
          <div
            v-for="cat in categoryTree"
            :key="cat.id"
            class="category-item"
            :class="{ active: !cat.hasChildren && activeCategory === cat.id, group: cat.hasChildren }"
            :style="{ paddingLeft: (8 + cat.depth * 16) + 'px' }"
            :title="cat.hasChildren ? '点击展开/收起子分类' : '点击筛选该分类'"
            @click="cat.hasChildren ? $emit('toggle-expand', cat.id) : $emit('category-select', cat.id)"
          >
            <span
              v-if="cat.hasChildren"
              class="cat-arrow"
              :class="{ expanded: cat.expanded }"
              @click.stop="$emit('toggle-expand', cat.id)"
            >
              <el-icon><ArrowRight /></el-icon>
            </span>
            <span v-else class="cat-arrow-placeholder"></span>
            <span class="category-name">{{ cat.name }}</span>
            <span class="category-count">{{ cat.articleCount || 0 }}</span>
          </div>
        </div>

        <div v-show="activePanel === 'tag'" class="tag-panel">
          <div class="tag-cloud">
            <el-tag
              v-for="tag in visibleTags"
              :key="tag.id"
              :type="activeTag === tag.id ? 'primary' : 'info'"
              class="tag-item"
              @click="$emit('tag-click', tag.id)"
            >
              {{ tag.name }}<span v-if="tag.articleCount > 0" class="tag-count">{{ tag.articleCount }}</span>
            </el-tag>
          </div>
          <div v-if="tags.length > tagLimit" class="tag-toggle" @click="$emit('toggle-show-all')">
            {{ showAllTags ? '收起' : `全部标签 (${tags.length})` }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// 当前展示的面板：分类 / 标签
const activePanel = ref('category')

defineProps({
  // 已按层级展开/折叠好的分类列表（含 depth、hasChildren、expanded）
  categoryTree: { type: Array, default: () => [] },
  activeCategory: { type: [Number, String], default: null },
  tags: { type: Array, default: () => [] },
  visibleTags: { type: Array, default: () => [] },
  activeTag: { type: [Number, String], default: null },
  showAllTags: { type: Boolean, default: false },
  tagLimit: { type: Number, default: 10 }
})

defineEmits(['category-select', 'toggle-expand', 'tag-click', 'toggle-show-all'])
</script>

<style scoped>
.sidebar-content {
  width: 100%;
}
.sidebar-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.sidebar-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  margin-bottom: 14px;
  background: #f5f7fa;
  border-radius: 10px;
}
.sidebar-tab {
  flex: 1;
  padding: 7px 0;
  text-align: center;
  font-size: 14px;
  color: #606266;
  border-radius: 7px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
}
.sidebar-tab:hover {
  color: #409eff;
}
.sidebar-tab.active {
  background: #fff;
  color: #409eff;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.sidebar-panels {
  min-height: 200px;
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
/* 父分类只是分组：点击展开/收起，不参与筛选 */
.category-item.group {
  font-weight: 500;
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
</style>
