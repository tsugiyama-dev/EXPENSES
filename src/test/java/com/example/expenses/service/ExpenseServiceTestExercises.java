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
			
			Long userId = 123L;
			Long expenseId = 123L;
			Expense current = new Expense(
					expenseId, userId,null,null,null,ExpenseStatus.DRAFT
					,null,null,null,0);
			Expense saved = new Expense(
					expenseId, userId,null,null,null,ExpenseStatus.SUBMITTED
					,null,null,null,0);
			
			ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
			//Given
			
			//when
			when(authenticationContext.getCurrentUserId()).thenReturn(userId);
			when(expenseMapper.findById(expenseId))
				.thenReturn(current)
				.thenReturn(saved);
			when(expenseMapper.submitDraft(expenseId)).thenReturn(1);
			
			ExpenseResponse res = expenseService.submit(expenseId);


			//Then
			verify(expenseMapper).submitDraft(captor.capture());
			verify(auditLogMapper).insert(any(ExpenseAuditLog.class));
			
	
			assertEquals(123L, captor.getValue());
			assertEquals(0, res.version());
			assertEquals(ExpenseStatus.SUBMITTED, res.status());
		}

		@Nested
		@DisplayName("異常系：経費提出のテスト")
		class SubmitExceptionTest{
			
			@Test
			@DisplayName("経費が存在しない場合NoSuchElementExceptionをスロー")
			void 経費が存在しない場合NoSuchElementExceptionをスロー() {
				
				//Given
				Long userId = 123L;
				Long invalidExpenseId = 999L;
				
				//when
				when(expenseMapper.findById(invalidExpenseId)).thenReturn(null);
				when(authenticationContext.getCurrentUserId()).thenReturn(userId);
				

				//then
				assertThatThrownBy(() -> expenseService.submit(invalidExpenseId))
				.isInstanceOf(NoSuchElementException.class);
				
				verify(expenseMapper, never()).submitDraft(invalidExpenseId);
				verify(auditLogMapper,never()).insert(any(ExpenseAuditLog.class)); 
				verify(publisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
			}
			
			@DisplayName("本人以外が提出した場合にBusinessExceptionをスロー")
			@Test
			void 本人以外が提出した場合にBusinessExceptionをスロー() {
				
				//Given
				Long userId = 123L;
				Long ownerId = 456L;
				Long expenseId = 789L;
				
				Expense expense = new Expense(null,ownerId,null,null,null,null,null,null,null,null);
				
				//when
				when(expenseMapper.findById(expenseId)).thenReturn(expense);
				when(authenticationContext.getCurrentUserId()).thenReturn(userId);
				
				//Then
				
				assertThatThrownBy(() -> expenseService.submit(expenseId))
				.isInstanceOf(BusinessException.class);
				
				verify(expenseMapper, never()).submitDraft(anyLong());
				verify(auditLogMapper,never()).insert(any(ExpenseAuditLog.class)); 
				verify(publisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
				
				
			}
			@DisplayName("既に提出済みの場合にIllegalStateExceptionをスロー")
			@Test
			void すでに提出済みの場合にIllegalStateExceptionをスロー() {
				
				//Given
				Long userId = 123L;
				Long expenseId = 789L;
				
				Expense expense = new Expense(null, userId, null, null, null, ExpenseStatus.SUBMITTED, null, null, null, null);
				
				
				//when
				when(expenseMapper.findById(expenseId)).thenReturn(expense);
				when(authenticationContext.getCurrentUserId()).thenReturn(userId);
				
				//Then
				
				assertThatThrownBy(() -> expenseService.submit(expenseId))
				.isInstanceOf(BusinessException.class);
				
				verify(expenseMapper, never()).submitDraft(anyLong());
				verify(auditLogMapper,never()).insert(any(ExpenseAuditLog.class)); 
				verify(publisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
				
				
			}
		}
}
