package com.example.expenses.dto.response;

import java.util.List;

public record PaginationResponse<T>(
		List<T> items,
		int currentPage,
		int pageSize,
		int total,
		int totalPages,
		List<Integer> pageNumbers) {

}
