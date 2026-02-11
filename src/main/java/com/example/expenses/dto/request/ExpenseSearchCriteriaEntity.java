package com.example.expenses.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseSearchCriteriaEntity {

	private Long applicantId;
	private String status;
	private String title;
	private BigDecimal amountMin;
	private BigDecimal amountMax;
	private LocalDate submittedFrom;
	private LocalDate submittedTo;
	
	
}
