package com.knowledge.base.service;

import com.knowledge.base.dto.CategoryRequest;
import com.knowledge.base.vo.CategoryVO;

import java.util.List;

public interface CategoryService {
    List<CategoryVO> getAllCategories();
    CategoryVO createCategory(CategoryRequest request);
    CategoryVO updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
}
