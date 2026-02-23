package com.example.expenses.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.expenses.domain.Category;

public record CategoryResponse(
		Long id, 
		String name,
		String description,
		String color,
		String icon,
		boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
	
	public static CategoryResponse fromDomain(Category category) {
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				category.getDescription(),
				category.getColor(),
				category.getIcon(),
				category.isActive(),
				category.getCreatedAt(),
				category.getUpdatedAt()
				);
	}
	
	public static List<CategoryResponse> fromDomainList(List<Category> categories) {
		return categories.stream()
				.map(CategoryResponse::fromDomain)
				.toList();
	}

}
