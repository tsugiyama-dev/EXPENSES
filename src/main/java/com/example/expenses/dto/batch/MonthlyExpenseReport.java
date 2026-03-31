package com.example.expenses.dto.batch;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import com.example.expenses.domain.ExpenseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyExpenseReport {

	private YearMonth targetMonth;
	
	private int totalCount;
	
	private BigDecimal totalAmount;
	
	private Map<ExpenseStatus, StatusSummary> statusSummaries;
	
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StatusSummary {
		
		private int  count;
		
		private BigDecimal amount;
		
		private double percentage;
	}
}
