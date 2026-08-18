package com.nahid.product.mapper;

import com.nahid.product.dto.request.CreateProductRequestDto;
import com.nahid.product.dto.response.ProductResponseDto;
import com.nahid.product.dto.request.UpdateProductRequestDto;
import com.nahid.product.entity.Product;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Product toEntity(CreateProductRequestDto request);

    ProductResponseDto toResponse(Product product);

    List<ProductResponseDto> toResponseList(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProductFromRequest(UpdateProductRequestDto request, @MappingTarget Product product);




}
