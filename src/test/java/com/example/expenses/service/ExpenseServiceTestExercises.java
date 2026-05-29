package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.event.ExpenseSubmittedEvent;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("経費申請")
public class ExpenseServiceTestExercises {

	@InjectMocks
	ExpenseService expenseService;
	@Mock
	ExpenseAuditLogMapper auditLogMapper;
	@Mock
	AuthenticationContext authenticationContext;
	@Mock
	ExpenseMapper expenseMapper;
	@Mock
	ApplicationEventPublisher publisher;

		@Test
		@DisplayName("正常系：経費提出のテスト")
		void 経費の提出() {
			
			Long expenseId = 123L;
			Long userId = 123L;
			Expense current = new Expense(
					expenseId, userId,null,null,null,ExpenseStatus.DRAFT
					,null,null,null,0);
			Expense saved = new Expense(
					expenseId, userId,null,null,null,ExpenseStatus.SUBMITTED
					,null,null,null,0);
			
			// ArgumentCaptor：モックに渡された引数を後で取り出して検証
			ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
			
			when(expenseMapper.findById(expenseId))
				.thenReturn(current) // 1回目: ビジネスルール検証用
				.thenReturn(saved); // 2回目: 最終 findById（レスポンス返却用）
			when(expenseMapper.submitDraft(expenseId)).thenReturn(1);
			
			ExpenseResponse res = expenseService.submit(expenseId, userId);


			//Then
			verify(expenseMapper).submitDraft(captor.capture());
			assertEquals(123L, captor.getValue());

			// 監査ログが記録されたか
			verify(auditLogMapper).insert(any(ExpenseAuditLog.class));
			
			//SpringEventが発行されたか
			verify(publisher).publishEvent(any(ExpenseSubmittedEvent.class));
	
			//返却値の検証
			assertEquals(0, res.version());
			assertEquals(ExpenseStatus.SUBMITTED, res.status());
		}

		@Nested
		@DisplayName("異常系：経費提出のテスト")
		class SubmitExceptionTest{
			
			@Test
			@DisplayName("経費が存在しない場合NoSuchElementExceptionをスロー")
			void 経費が存在しない場合NoSuchElementExceptionをスロー() {
				
		
				Long userId = 123L;
				Long invalidExpenseId = 999L;
				
		
				when(expenseMapper.findById(invalidExpenseId)).thenReturn(null);
				
				//then
				assertThatThrownBy(() -> expenseService.submit(invalidExpenseId, userId))
				.isInstanceOf(NoSuchElementException.class);
				
				// DB 更新、監査ログ、イベント発行が一切呼ばれないことを確認
				verify(expenseMapper, never()).submitDraft(invalidExpenseId);
				verify(auditLogMapper,never()).insert(any(ExpenseAuditLog.class)); 
				verify(publisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
			}
			
			@DisplayName("本人以外が提出した場合にBusinessExceptionをスロー")
			@Test
			void 本人以外が提出した場合にBusinessExceptionをスロー() {
				
		
				Long userId = 123L;
				Long ownerId = 456L; // 申請者は別人
				Long expenseId = 789L;
				
				// status = DRAFT , applicantIdがuserIdとは異なる
				Expense expense = new Expense(null,ownerId,null,null,null,ExpenseStatus.DRAFT,null,null,null,null);
				
				when(expenseMapper.findById(expenseId)).thenReturn(expense);
				
				//assertJ Junit5の同様のメソッドはassertThrows()だが、例外の内容も検証したいのでassertThatThrownBy()を使用。
				//戻り値を受け取ればassertThrows()でも同様のことが可能。
				assertThatThrownBy(() -> expenseService.submit(expenseId, userId))
				.isInstanceOf(BusinessException.class);
				
				verify(expenseMapper, never()).submitDraft(anyLong());
				verify(auditLogMapper,never()).insert(any(ExpenseAuditLog.class)); 
				verify(publisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
			}
			@DisplayName("既に提出済みの場合にBusinessExceptionをスロー")
			@Test
			void すでに提出済みの場合にBusinessExceptionをスロー() {
				
				//Given
				Long userId = 123L;
				Long expenseId = 789L;
				
				Expense expense = new Expense(null, userId, null, null, null,
						ExpenseStatus.SUBMITTED, null, null, null, null);
				
				when(expenseMapper.findById(expenseId)).thenReturn(expense);
		
				assertThatThrownBy(() -> expenseService.submit(expenseId, userId))
				.isInstanceOf(BusinessException.class);
				
				verify(expenseMapper, never()).submitDraft(anyLong());
				verify(auditLogMapper,never()).insert(any(ExpenseAuditLog.class));
				verify(publisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
			}
		}
}
