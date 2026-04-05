package com.example.expenses.batch.writer;

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
		
//		doAnswer()(expenseMapper.insert(expense1)).thenReturn(1);
		
	}

}
