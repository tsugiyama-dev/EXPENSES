package com.example.expenses.dto.response;

import java.util.List;

public record PagedResponse<T>(
		List<T> items,
		int currentPage,
		int pageSize,
		int total,
		int totalPages) {


}
