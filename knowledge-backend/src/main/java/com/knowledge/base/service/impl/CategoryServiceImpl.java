package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.entity.Category;
import com.knowledge.base.mapper.CategoryMapper;
import com.knowledge.base.service.CategoryService;
import com.knowledge.base.service.NotificationService;
import com.knowledge.base.vo.CategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    public List<CategoryVO> getAllCategories() {
        List<Category> categories = categoryMapper.selectList(null);
        // 按名称字母排序（中文按拼音）
        categories.sort(java.util.Comparator.comparing(
                Category::getName, java.text.Collator.getInstance(java.util.Locale.CHINA)));

        // 每个分类直接挂载的文章数
        Map<Long, Long> directCount = new HashMap<>();
        for (Map<String, Object> row : categoryMapper.countArticlesByCategory()) {
            directCount.put(toLong(row.get("categoryId")), toLong(row.get("cnt")));
        }

        // 构建子分类映射
        Map<Long, List<Category>> children = new HashMap<>();
        for (Category c : categories) {
            children.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
        }

        // 自底向上聚合：分类文章数 = 直接挂载数 + 所有子分类文章数
        Map<Long, Long> memo = new HashMap<>();
        return categories.stream().map(c -> {
            CategoryVO vo = CategoryVO.from(c);
            vo.setArticleCount(aggregateCount(c, children, directCount, memo));
            return vo;
        }).toList();
    }

    private long aggregateCount(Category c, Map<Long, List<Category>> children,
                                Map<Long, Long> directCount, Map<Long, Long> memo) {
        if (memo.containsKey(c.getId())) {
            return memo.get(c.getId());
        }
        long total = directCount.getOrDefault(c.getId(), 0L);
        for (Category child : children.getOrDefault(c.getId(), List.of())) {
            total += aggregateCount(child, children, directCount, memo);
        }
        memo.put(c.getId(), total);
        return total;
    }

    private long toLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    @Override
    @Transactional
    public CategoryVO createCategory(Category category) {
        // 父分类存在性校验（0 表示顶级分类，无需校验）
        if (category.getParentId() != null && category.getParentId() != 0
                && categoryMapper.selectById(category.getParentId()) == null) {
            throw new RuntimeException("父分类不存在");
        }
        categoryMapper.insert(category);

        // 父分类下新增子分类后，通知该分类下文章的作者修改分类；
        // 通知失败不影响分类创建
        if (category.getParentId() != null && category.getParentId() != 0) {
            try {
                notificationService.notifyCategoryAddedChild(category.getParentId(), category.getName());
            } catch (Exception e) {
                // 通知失败不影响分类创建
            }
        }
        return CategoryVO.from(category);
    }

    @Override
    @Transactional
    public CategoryVO updateCategory(Long id, Category category) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) throw new RuntimeException("分类不存在");
        Long oldParentId = existing.getParentId();
        existing.setName(category.getName());
        existing.setParentId(category.getParentId());
        if (category.getSort() != null) {
            existing.setSort(category.getSort());
        }
        categoryMapper.updateById(existing);

        // 分类被移动到别的父分类下时，同样触发该父分类下文章作者的通知
        if (category.getParentId() != null && category.getParentId() != 0
                && !category.getParentId().equals(oldParentId)) {
            try {
                notificationService.notifyCategoryAddedChild(category.getParentId(), existing.getName());
            } catch (Exception e) {
                // 通知失败不影响分类更新
            }
        }
        return CategoryVO.from(existing);
    }

    @Override
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }
}
