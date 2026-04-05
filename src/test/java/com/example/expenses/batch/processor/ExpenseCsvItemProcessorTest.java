package com.example.expenses.batch.processor;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.expenses.batch.dto.ExpenseCsvRow;
import com.example.expenses.domain.Expense;

@DisplayName("ExpenseCsvItemProcessorのユニットテスト")
class ExpenseCsvItemProcessorTest {

	private ExpenseCsvItemProcessor processor;
	
	@BeforeEach
	void setUp() {
		processor = new ExpenseCsvItemProcessor();
	}
	@Test
	@DisplayName("正常なデータの場合にExpenseが返されること")
	void 正常なデータの場合Expenseが返される() {
		
		ExpenseCsvRow row = new ExpenseCsvRow();
		row.setApplicantId(1L);
		row.setTitle("交通費");
		row.setAmount("1000");
		row.setCurrency("JPY");
		
		Expense result = null;
		
		try {
			result = processor.process(row);
		} catch (Exception e) {
				e.printStackTrace();
		}
		
		assertThat(result.getAmount()).isEqualTo(new BigDecimal("1000"));
		assertThat(result.getApplicantId()).isEqualTo(1L);
		assertThat(result.getTitle()).isEqualTo("交通費");
		assertThat(result.getCurrency()).isEqualTo("JPY");
	}
	
	@Test
	@DisplayName("タイトルが空の場合にnullが返されること")
	void タイトルが空の場合Nullが返される () {
		ExpenseCsvRow row = new ExpenseCsvRow();
		row.setApplicantId(1L);
		row.setTitle("");
		row.setAmount("1000");
		row.setCurrency("JPY");
		
		Expense result = null;
		
		try {
			result = processor.process(row);
		} catch (Exception e) {
				e.printStackTrace();
		}
		
		assertThat(result).isNull();
		
	}
	@Test
	@DisplayName("金額が負の場合にNullが返される")
	void 金額が負の場合にNullが返される() {
		ExpenseCsvRow row = new ExpenseCsvRow();
		row.setApplicantId(1L);
		row.setTitle("交通費");
		row.setAmount("-1000");
		row.setCurrency("JPY");
		
		Expense result = null;
		
		try {
			result = processor.process(row);
		} catch (Exception e) {
		
		}
		
		assertThat(result).isNull();
		
	}
	
	@Test
	@DisplayName("金額が数値でない場合にNullが返される")
	void 金額が数値でない場合にNullが返される() {
		ExpenseCsvRow row = new ExpenseCsvRow();
		row.setApplicantId(1L);
		row.setTitle("交通費");
		row.setAmount("abc");
		row.setCurrency("JPY");
		
		Expense result = null;
		
		try {
			result = processor.process(row);
		} catch (Exception e) {
		
		}
		
		assertThat(result).isNull();
	}
	@Test
	@DisplayName("申請者IDがnullの場合にNullが返される")
	void 申請者IDがnullの場合にNullが返される() {
		ExpenseCsvRow row = new ExpenseCsvRow();
		row.setApplicantId(null);
		row.setTitle("交通費");
		row.setAmount("1000");
		row.setCurrency("JPY");
		
		Expense result = null;
		
		try {
			result = processor.process(row);
		} catch (Exception e) {
		
		}
		
		assertThat(result).isNull();
	}

}
