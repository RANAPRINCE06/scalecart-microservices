package com.nahid.product.mapper;


import com.nahid.product.dto.response.CategoryResponseDto;
import com.nahid.product.dto.request.CreateCategoryRequestDto;
import com.nahid.product.entity.Category;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Category toEntity(CreateCategoryRequestDto request);

    CategoryResponseDto toResponse(Category category);

    List<CategoryResponseDto> toResponseList(List<Category> categories);
}