package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * ExpenseServiceのユニットテスト
 * モックを使用してＤＢなしでテスト
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
	@DisplayName("正常系：経費を作成できる")
	void 経費を作成できる() {
		//Given ユーザーIDとリクエストデータ
		Long expectedUserId = 123L;
		ExpenseCreateRequest request = new ExpenseCreateRequest(
				"出張費",
				new BigDecimal("10000"),
				"JPY"
				);
		// モックの振る舞いを定義
		when(authenticationContext.getCurrentUserId()).thenReturn(expectedUserId);
	
		doAnswer(invocation -> {
			Expense expense = invocation.getArgument(0);
			return null;
		}).
		when(expenseMapper).insert(any(Expense.class));
		
		doAnswer(inv -> {
			ExpenseAuditLog arg = inv.getArgument(0);

			return null;
		}).
		when(auditLogMapper).insert(any(ExpenseAuditLog.class));
		
		
		//  when テスト対象のメソッドを呼び出す
		ExpenseResponse response = expenseService.create(request);
		
		// then 正しい値が返される
		assertThat(response).isNotNull();
		assertThat(response.applicantId()).isEqualTo(expectedUserId);
		assertThat(response.title()).isEqualTo("出張費");
		assertThat(response.amount()).isEqualTo(new BigDecimal("10000"));
		
		// then 必要なメソッドが呼び出されたか
		verify(authenticationContext).getCurrentUserId();
		verify(expenseMapper).insert(any(Expense.class));
		verify(auditLogMapper).insert(any(ExpenseAuditLog.class));

	}
	
	@Nested
	@DisplayName("経費検索")
	class SearchTest {
		
		@Test
		@DisplayName("一般ユーザー：自分の経費のみ取得できる")
		void 自分の経費の未取得できる() {
			
			//Given 一般ユーザー
			Long userId = 123L;
			
			ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
					null, null, null,null,null,null,null,null
					);
			//モックの振る舞いを定義
			when(authenticationContext.getCurrentUserId()).thenReturn(userId);
			when(authenticationContext.isApprover()).thenReturn(false);
			
			//when テスト対象のメソッドを呼び出す
			expenseService.search(criteria, 1, 10);
			
			//then 必要なメソッドが呼ばれたか
			verify(expenseMapper).search(
					argThat(c -> c.getApplicantId().equals(userId))
					,anyString()
					,anyString()
					,anyInt()
					,anyInt()
					);
			
		}
		@Test
		@DisplayName("承認者：すべての経費を取得できる")
		void 承認者はすべての経費を取得できる() { 
			
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

}
