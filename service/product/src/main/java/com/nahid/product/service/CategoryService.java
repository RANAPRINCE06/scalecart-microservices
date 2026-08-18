package com.nahid.product.service;



import com.nahid.product.dto.response.CategoryResponseDto;
import com.nahid.product.dto.request.CreateCategoryRequestDto;

import java.util.List;

public interface CategoryService {

    CategoryResponseDto createCategory(CreateCategoryRequestDto request);

    CategoryResponseDto getCategoryById(Long id);

    List<CategoryResponseDto> getAllCategories();

    List<CategoryResponseDto> getActiveCategories();



    void deleteCategory(Long id);
}