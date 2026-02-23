package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.expenses.domain.Expense;
import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.dto.request.ExpenseCreateRequest;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;

//@ExtendWith(MockitoExtension.class)
//@WebMvcTest(ExpenseController.class)


/**
 * ExpenseServiceのユニットテスト
 * モックを使用してデータベースなしでテスト可能
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

	@Mock
	private ExpenseMapper expenseMapper;
	@Mock
	private ExpenseAuditLogMapper auditLogMapper;
	@Mock
	private AuthenticationContext authenticationContext;
	
	@InjectMocks
	private ExpenseService expenseService;
	
	@Test
	void expenseCreate_Success() {
		// テストコードをここに記述
		Long expectedUserId = 123L;
		ExpenseCreateRequest request = new ExpenseCreateRequest(
				"出張費",
				new BigDecimal("10000"),
				"JPY"
				);
		// モックの振る舞いを定義
		when(authenticationContext.getCurrentUserId()).thenReturn(expectedUserId);
		
		// expenseMapperのinsertメソッドが呼ばれたときに、引数のExpenseオブジェクトにIDを設定するようにする
		doAnswer(invocation -> {
			Expense expense = invocation.getArgument(0);
			expense = Expense.create(expectedUserId,expense.getTitle(), expense.getAmount(), expense.getCurrency()); // モックのinsertメソッドが呼ばれたときにIDを設定
//			expense..setId(expectedUserId); // モックのinsertメソッドが呼ばれたときにIDを設定
			return null;
		}).when(expenseMapper).insert(any(Expense.class));
		
		// テスト対象のメソッドを呼び出す
		ExpenseResponse response = expenseService.create(request);
		
		// 結果を検証
		assertThat(response).isNotNull();
		assertThat(response.applicantId()).isEqualTo(expectedUserId);
		assertThat(response.title()).isEqualTo("出張費");
		assertThat(response.amount()).isEqualTo(new BigDecimal("10000"));
		
		//methodが呼び出されたかを検証
		verify(authenticationContext).getCurrentUserId();
		verify(expenseMapper).insert(any(Expense.class));
		verify(auditLogMapper).insert(any(ExpenseAuditLog.class));
		
		
	}
	
	
	@Test
	void expenseSearch_other_than_approver_only_owners_expenses_get() { //other than～  ～以外
		
		Long userId = 123L;
		
		when(authenticationContext.getCurrentUserId()).thenReturn(userId);
		when(authenticationContext.isApprover()).thenReturn(false);
		
		ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
				null, null, null,null,null,null,null,null
				);
		// テスト対象のメソッドを呼び出す
		expenseService.search(criteria, 1, 10);
		
		verify(expenseMapper).search(
				argThat(c -> c.getApplicantId().equals(userId))
				,anyString()
				,anyString()
				,anyInt()
				,anyInt()
		);

	}
	@Test
	void expenseSearch_approver_all_expenses_get() { 
		
		Long approverId = 456L;
		
		when(authenticationContext.getCurrentUserId()).thenReturn(approverId);
		when(authenticationContext.isApprover()).thenReturn(true);
		
		ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
				null ,null , null,null,null,null,null,null
				);
		// テスト対象のメソッドを呼び出す
		expenseService.search(criteria, 1, 10);
		
		verify(expenseMapper).search(
				//applicantIdでフィルタされていないことを検証
				argThat(c -> c.getApplicantId() == null)
				,anyString()
				,anyString()
				,anyInt()
				,anyInt()
				);
		
	}

}
