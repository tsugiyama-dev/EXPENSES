package com.example.expenses.batch.dto;

import lombok.Data;

@Data
public class ExpenseCsvRow {

	private Long applicantId;
	private String title;
	private String amount;
	
	private String currency;
	
}
