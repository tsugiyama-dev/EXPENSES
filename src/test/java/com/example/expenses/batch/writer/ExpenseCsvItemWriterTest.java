package com.example.expenses.batch.writer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import com.example.expenses.domain.Expense;
import com.example.expenses.repository.ExpenseMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseCsvItemWriterのユニットテスト")
class ExpenseCsvItemWriterTest {

	@Mock
	private ExpenseMapper expenseMapper;
	@InjectMocks
	private ExpenseCsvItemWriter writer;
	
	@Test
	@DisplayName("chunkに含まれるexpenseが正しく挿入される")
	void chunkに含まれるexpenseが正しく挿入される() {
		
		Expense expense1 = Expense.create(1L, "交通費", new BigDecimal(1000), "JPY");
		Expense expense2 = Expense.create(2L, "食費", new BigDecimal(2000), "JPY");
		Expense expense3 = Expense.create(3L, "宿泊費", new BigDecimal(3000), "JPY");
		Chunk<Expense> chunk = new Chunk<>(List.of(expense1, expense2, expense3));
		
		try {
			writer.write(chunk);
		} catch (Exception e) {
		
			e.printStackTrace();
		}
		verify(expenseMapper, times(1)).insert(expense1);
		verify(expenseMapper, times(1)).insert(expense2);
		verify(expenseMapper, times(1)).insert(expense3);
		
	}
	
	@Test
	@DisplayName("Chunkが空の場合、何も実行されない")
	void Chunkが空の場合何も実行されない()throws Exception {
		
		Chunk<Expense> chunk = new Chunk<>();
		
		writer.write(chunk);
				
		verify(expenseMapper, never()).insert(any(Expense.class));
	}

}
