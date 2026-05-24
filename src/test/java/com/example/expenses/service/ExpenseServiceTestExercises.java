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

/*
 * 【このテストクラスで確認すること】
 *
 * ExpenseService.submit() は：
 *   1. DB に submitDraft() を呼んで SUBMITTED に更新する
 *   2. 監査ログを insert する
 *   3. publishEvent(ExpenseSubmittedEvent) を呼ぶ（Kafka への橋渡しはBridgeListenerが担当）
 *
 * 【@InjectMocks の仕組み】
 * Mockito が @Mock フィールドの型を見て、ExpenseService のコンストラクタに
 * 対応するモックを自動的に注入する。
 * ExpenseService は ApplicationEventPublisher を依存として持つので、
 * @Mock ApplicationEventPublisher が注入される。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("経費申請サービス テスト")
public class ExpenseServiceTestExercises {

	@InjectMocks
	ExpenseService expenseService;

	@Mock
	ExpenseAuditLogMapper auditLogMapper;
	@Mock
	AuthenticationContext authenticationContext;
	@Mock
	ExpenseMapper expenseMapper;
	/*
	 * ApplicationEventPublisher をモック化する。
	 * publishEvent() が呼ばれたかどうかを verify() で確認できる。
	 * 実際には何もしない（Kafka には送らない）ので、単体テストが速い。
	 */
	@Mock
	ApplicationEventPublisher eventPublisher;

	@Test
	@DisplayName("正常系：経費提出のテスト")
	void 経費の提出() {

		Long userId = 123L;
		Long expenseId = 123L;
		Expense current = new Expense(
				expenseId, userId, null, null, null, ExpenseStatus.DRAFT,
				null, null, null, 0);
		Expense saved = new Expense(
				expenseId, userId, null, null, null, ExpenseStatus.SUBMITTED,
				null, null, null, 0);

		// ArgumentCaptor：モックに渡された引数を後で取り出して検証する道具
		ArgumentCaptor<Long> submitCaptor = ArgumentCaptor.forClass(Long.class);

		when(expenseMapper.findById(expenseId))
			.thenReturn(current)   // 1回目: ビジネスルール検証用
			.thenReturn(saved);    // 2回目: 最終 findById（レスポンス返却用）
		when(expenseMapper.submitDraft(expenseId)).thenReturn(1);

		// submit(expenseId, applicantId) — 申請者IDを明示的に渡す
		ExpenseResponse res = expenseService.submit(expenseId, userId);

		// DB の更新が正しい expenseId で呼ばれたか
		verify(expenseMapper).submitDraft(submitCaptor.capture());
		assertEquals(123L, submitCaptor.getValue());

		// 監査ログが記録されたか
		verify(auditLogMapper).insert(any(ExpenseAuditLog.class));

		// Spring Event が発行されたか（→ BridgeListener が Kafka へ送る）
		verify(eventPublisher).publishEvent(any(ExpenseSubmittedEvent.class));

		// 返却値の検証
		assertEquals(0, res.version());
		assertEquals(ExpenseStatus.SUBMITTED, res.status());
	}

	@Nested
	@DisplayName("異常系：経費提出のテスト")
	class SubmitExceptionTest {

		@Test
		@DisplayName("経費が存在しない場合 NoSuchElementException をスロー")
		void 経費が存在しない場合NoSuchElementExceptionをスロー() {

			Long userId = 123L;
			Long invalidExpenseId = 999L;

			when(expenseMapper.findById(invalidExpenseId)).thenReturn(null);

			assertThatThrownBy(() -> expenseService.submit(invalidExpenseId, userId))
				.isInstanceOf(NoSuchElementException.class);

			// DB 更新・監査ログ・イベント発行が一切呼ばれないことを確認
			verify(expenseMapper, never()).submitDraft(anyLong());
			verify(auditLogMapper, never()).insert(any(ExpenseAuditLog.class));
			verify(eventPublisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
		}

		@DisplayName("本人以外が提出した場合に BusinessException をスロー")
		@Test
		void 本人以外が提出した場合にBusinessExceptionをスロー() {

			Long userId = 123L;
			Long ownerId = 456L; // 申請者は別人
			Long expenseId = 789L;

			// status = DRAFT だが、applicantId が userId と異なる
			Expense expense = new Expense(null, ownerId, null, null, null,
					ExpenseStatus.DRAFT, null, null, null, null);

			when(expenseMapper.findById(expenseId)).thenReturn(expense);

			assertThatThrownBy(() -> expenseService.submit(expenseId, userId))
				.isInstanceOf(BusinessException.class);

			verify(expenseMapper, never()).submitDraft(anyLong());
			verify(auditLogMapper, never()).insert(any(ExpenseAuditLog.class));
			verify(eventPublisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
		}

		@DisplayName("既に提出済みの場合に BusinessException をスロー")
		@Test
		void すでに提出済みの場合にBusinessExceptionをスロー() {

			Long userId = 123L;
			Long expenseId = 789L;

			// status = SUBMITTED（再提出は許可されていない）
			Expense expense = new Expense(null, userId, null, null, null,
					ExpenseStatus.SUBMITTED, null, null, null, null);

			when(expenseMapper.findById(expenseId)).thenReturn(expense);

			assertThatThrownBy(() -> expenseService.submit(expenseId, userId))
				.isInstanceOf(BusinessException.class);

			verify(expenseMapper, never()).submitDraft(anyLong());
			verify(auditLogMapper, never()).insert(any(ExpenseAuditLog.class));
			verify(eventPublisher, never()).publishEvent(any(ExpenseSubmittedEvent.class));
		}
	}
}
