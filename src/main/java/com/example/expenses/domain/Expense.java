package com.example.expenses.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Expense {

	private Long id;
	private Long applicantId;
	private String title;
	private BigDecimal amount;
	private String currency;
	private ExpenseStatus status;
	private LocalDateTime submittedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private int version;
	
	
}
