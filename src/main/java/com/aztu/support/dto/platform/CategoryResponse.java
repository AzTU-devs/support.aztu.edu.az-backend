package com.aztu.support.dto.platform;

import com.aztu.support.domain.Category;

public record CategoryResponse(
        Long id,
        Long platformId,
        String platformName,
        String name,
        String description,
        boolean active) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getPlatform().getId(),
                category.getPlatform().getName(),
                category.getName(),
                category.getDescription(),
                category.isActive());
    }
}
