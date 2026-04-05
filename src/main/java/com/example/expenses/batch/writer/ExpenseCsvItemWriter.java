package com.example.expenses.batch.writer;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.example.expenses.domain.Expense;
import com.example.expenses.repository.ExpenseMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExpenseCsvItemWriter implements ItemWriter<Expense> {

	private final ExpenseMapper expenseMapper;

	@Override
	public void write(Chunk<? extends Expense> chunk) throws Exception {
		
		chunk.getItems().forEach(expense -> {
			expenseMapper.insert(expense);
		});
		
		
	}
	
}
