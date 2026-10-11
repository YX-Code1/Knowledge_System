package com.knowledge.base.service.impl;

import com.knowledge.base.ai.event.CategoryChildAddedEvent;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.dto.CategoryRequest;
import com.knowledge.base.entity.Category;
import com.knowledge.base.mapper.CategoryMapper;
import com.knowledge.base.service.CategoryService;
import com.knowledge.base.vo.CategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    private ApplicationEventPublisher eventPublisher;

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
    public CategoryVO createCategory(CategoryRequest request) {
        // 父分类存在性校验（0 表示顶级分类，无需校验）
        if (request.getParentId() != null && request.getParentId() != 0
                && categoryMapper.selectById(request.getParentId()) == null) {
            throw new BusinessException("父分类不存在");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        categoryMapper.insert(category);

        // 父分类下新增子分类后，事务提交后异步触发 AI 自动重分类（失败自动降级为手动提醒）；
        // 事件发布失败不影响分类创建
        if (category.getParentId() != null && category.getParentId() != 0) {
            try {
                eventPublisher.publishEvent(new CategoryChildAddedEvent(category.getParentId(), category.getName()));
            } catch (Exception e) {
                // 事件发布失败不影响分类创建
            }
        }
        return CategoryVO.from(category);
    }

    @Override
    @Transactional
    public CategoryVO updateCategory(Long id, CategoryRequest request) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) throw new BusinessException("分类不存在");
        Long oldParentId = existing.getParentId();
        existing.setName(request.getName());
        existing.setParentId(request.getParentId());
        if (request.getSort() != null) {
            existing.setSort(request.getSort());
        }
        categoryMapper.updateById(existing);

        // 分类被移动到别的父分类下时，同样触发 AI 自动重分类（事务提交后异步执行）
        if (request.getParentId() != null && request.getParentId() != 0
                && !request.getParentId().equals(oldParentId)) {
            try {
                eventPublisher.publishEvent(new CategoryChildAddedEvent(request.getParentId(), existing.getName()));
            } catch (Exception e) {
                // 事件发布失败不影响分类更新
            }
        }
        return CategoryVO.from(existing);
    }

    @Override
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }
}
