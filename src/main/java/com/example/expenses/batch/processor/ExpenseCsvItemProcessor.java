package com.example.expenses.batch.processor;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.expenses.batch.dto.ExpenseCsvRow;
import com.example.expenses.domain.Expense;

@Component
public class ExpenseCsvItemProcessor implements ItemProcessor<ExpenseCsvRow, Expense> {

	@Override
	public 
	@Nullable Expense process(ExpenseCsvRow item) throws Exception {
		
		if(item.getTitle() == null || item.getTitle().isBlank()) {
		    return null; // タイトルが空の場合はスキップ
//		    throw new NullPointerException(); // タイトルが空の場合はスキップ
		}
		
		if(item.getApplicantId() == null) {
			return null; // 申請者IDが無効な場合はスキップ
		}
		
		BigDecimal amount;
		try {
			amount = new BigDecimal(item.getAmount());
			if(amount.compareTo(BigDecimal.ZERO) <= 0) {
				return null; // 金額が正の数でない場合はスキップ
//				throw new IllegalArgumentException(); // 金額が正の数でない場合はスキップ
			}
		} catch (NumberFormatException e) {
			return null; // 金額が数値でない場合はスキップ
//			throw new IllegalArgumentException(); // 金額が数値でない場合はスキップ
		}
		

		return Expense.create(
				item.getApplicantId(),
				item.getTitle(),
				amount,
				item.getCurrency()
		);
	}

	
}
