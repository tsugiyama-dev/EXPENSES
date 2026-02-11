package com.example.expenses.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;

public record ExpenseResponse(
		Long id,
		Long applicantId,
		String title,
		BigDecimal amount,
		String currency, 
		ExpenseStatus status, 
		LocalDateTime submittedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		int version) {
	public static ExpenseResponse toResponse(Expense expense) {
		return new ExpenseResponse(
				expense.getId(),
				expense.getApplicantId(),
				expense.getTitle(),
				expense.getAmount(),
				expense.getCurrency(),
				expense.getStatus(),
				expense.getSubmittedAt(),
				expense.getCreatedAt(),
				expense.getUpdatedAt(),
				expense.getVersion());
	}
	public static List<ExpenseResponse> toListResponse(List<Expense> expense) {
		return expense.stream().map(r -> toResponse(r)) .toList();
	}

}
