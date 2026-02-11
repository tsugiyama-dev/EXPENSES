package com.example.expenses.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public record ExpenseSearchCriteria(
		Long applicantId,
		String status,
		String title,
		String sort,
		BigDecimal amountMin,
		BigDecimal amountMax,
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate submittedFrom,
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate submittedTo) {
	
	public static ExpenseSearchCriteriaEntity toEntity(ExpenseSearchCriteria criteria) {
		return new ExpenseSearchCriteriaEntity(
				criteria.applicantId(),
				criteria.status(),
				criteria.title(),
				criteria.amountMin(),
				criteria.amountMax(),
				criteria.submittedFrom(),
				criteria.submittedTo());
	}

}
