package com.knowledge.base.service;

import com.knowledge.base.entity.Category;
import com.knowledge.base.vo.CategoryVO;

import java.util.List;

public interface CategoryService {
    List<CategoryVO> getAllCategories();
    CategoryVO createCategory(Category category);
    CategoryVO updateCategory(Long id, Category category);
    void deleteCategory(Long id);
}
