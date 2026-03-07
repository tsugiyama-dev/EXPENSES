package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import com.example.expenses.domain.Expense;
import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.dto.request.ExpenseCreateRequest;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceExceptionTest {

	@Mock
	private ExpenseMapper expenseMapper;
	
	@Mock
	private ExpenseAuditLogMapper auditLogMapper;
	
	@Mock
	private AuthenticationContext authenticationContext;
	
	@InjectMocks
	private ExpenseService expenseService;
	
	
	@Test
	@DisplayName("ユーザー認証に失敗した場合の例外テスト")
	void ユーザー認証に失敗して例外をスロー() {
		
		//Given
		ExpenseCreateRequest request = new ExpenseCreateRequest(
				"経費"
				,new BigDecimal(1000)
				,"JPY"
				);
		
		when(authenticationContext.getCurrentUserId()).thenThrow(new IllegalStateException("未認証のユーザーです"));
		
		//then
		assertThatThrownBy(() -> expenseService.create(request))
		   .isInstanceOf(IllegalStateException.class)
		   .hasMessage("未認証のユーザーです");
			
		verify(expenseMapper, never()).insert(any(Expense.class));
		verify(auditLogMapper, never()).insert(any(ExpenseAuditLog.class));
		
	}
	
	@Test
	@DisplayName("authenticationContextがNullを返す")
	void authenticationContextがNullを返す()  {


		   ExpenseCreateRequest request = new ExpenseCreateRequest(
		            "出張費", new BigDecimal("10000"), "JPY"
		        );
		when(authenticationContext.getCurrentUserId()).thenReturn(null);
//		when(authenticationContext.getCurrentUserId()).thenThrow(new NullPointerException("認証情報を取得できませんでした"));
		
		assertThatThrownBy(() -> expenseService.create(request))
//		assertThatThrownBy(() -> expenseService.create(any(ExpenseCreateRequest.class)))
		.isInstanceOf(IllegalArgumentException.class);
		
		assertThat(request).isEqualTo(request);
	
	}
	
	@Test
	@DisplayName("経費の登録に失敗した場合の例外処理")
	void DBのインサートに失敗した場合の例外処理 () {
		
		//Given
		ExpenseCreateRequest request = new ExpenseCreateRequest(
				"経費"
				,new BigDecimal(1000)
				,"JPY"
				);

		doAnswer(invocation -> {
			throw new DataAccessResourceFailureException("DBで例外発生");
		}).
		when(expenseMapper).insert(any(Expense.class));
		
		//when & then
		assertThatThrownBy(() -> expenseService.create(request))
		.isInstanceOf(DataAccessResourceFailureException.class)
		.hasMessage("DBで例外発生");
		
		//メソッドが呼ばれていることを確認
		verify(authenticationContext,times(1)).getCurrentUserId();
		verify(expenseMapper, times(1)).insert(any(Expense.class));
		//メソッドが呼ばれていないことを確認
		verify(auditLogMapper, never()).insert(any(ExpenseAuditLog.class));
	}
	
	@Test
	@DisplayName("ログの登録に失敗した場合の例外処理")
	void DBにログの登録に失敗した場合の例外処理 () {
		//Given
		ExpenseCreateRequest request = new ExpenseCreateRequest(
				"経費"
				,new BigDecimal(1000)
				,"JPY"
				);
		
		doAnswer(invocation -> {
			invocation.getArgument(0);
			throw new DataAccessResourceFailureException ("logの登録に失敗しました");
		}).
		when(auditLogMapper).insert(any(ExpenseAuditLog.class));
		
		assertThatThrownBy(() -> expenseService.create(request))
		.isInstanceOf(DataAccessException.class).hasMessage("logの登録に失敗しました");
		
		verify(authenticationContext).getCurrentUserId();
		
		
	}
	
	
}
