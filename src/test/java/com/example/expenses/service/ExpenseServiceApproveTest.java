package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.notification.NotificationService;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.UserMapper;
import com.example.expenses.testutil.ExpenseTestBuilder;

/**
 * ExpenseService.approve() メソッドの単体テスト
 *
 * テストデータビルダーパターンを使用した例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService.approve() のテスト")
class ExpenseServiceApproveTest {

	@Mock
	private ExpenseMapper expenseMapper;

	@Mock
	private UserMapper userMapper;

	@Mock
	private ExpenseAuditLogMapper auditLogMapper;

	@Mock
	private NotificationService notificationService;

	@Mock
	private CreateCsvService createCsvService;

	@InjectMocks
	private ExpenseService expenseService;

	private Expense submittedExpense;
	private Expense approvedExpense;

	@BeforeEach
	void setUp() {
		// テストデータビルダーを使ってテストデータを準備
		submittedExpense = ExpenseTestBuilder.createSubmitted();

		approvedExpense = ExpenseTestBuilder.builder()
			.id(submittedExpense.getId())
			.applicantId(submittedExpense.getApplicantId())
			.title(submittedExpense.getTitle())
			.amount(submittedExpense.getAmount())
			.status(ExpenseStatus.APPROVED)
			.version(2)
			.build();
	}

	@Nested
	@DisplayName("正常系")
	class SuccessCase {

		@Test
		@DisplayName("提出済みの経費を承認できる")
		void 提出済みの経費を承認できる() {
			// Given
			long expenseId = submittedExpense.getId();
			int version = submittedExpense.getVersion();
			long actorId = 999L;

			given(expenseMapper.findById(expenseId))
				.willReturn(submittedExpense)    // 1回目: 提出済み
				.willReturn(approvedExpense);    // 2回目: 承認済み

			given(expenseMapper.approve(expenseId, version)).willReturn(1);
			given(userMapper.findEmailById(submittedExpense.getApplicantId()))
				.willReturn("applicant@example.com");

			// When
			ExpenseResponse result = expenseService.approve(expenseId, version, actorId);

			// Then
			assertThat(result.status()).isEqualTo(ExpenseStatus.APPROVED);
			assertThat(result.version()).isEqualTo(2);

			// モックの検証
			then(expenseMapper).should(times(2)).findById(expenseId);
			then(expenseMapper).should().approve(expenseId, version);
			then(auditLogMapper).should().insert(any());
			then(notificationService).should().notifyApproved(
				eq("applicant@example.com"),
				eq(expenseId),
				anyString()
			);
		}
	}

	@Nested
	@DisplayName("異常系")
	class ErrorCase {

		@Test
		@DisplayName("経費が存在しない場合はBusinessExceptionをスロー")
		void 経費が存在しない場合() {
			// Given
			long expenseId = 9999L;
			int version = 1;
			long actorId = 999L;

			given(expenseMapper.findById(expenseId)).willReturn(null);

			// When & Then
			assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("経費申請が見つかりません");

			// モックの検証
			then(expenseMapper).should().findById(expenseId);
			then(expenseMapper).should(never()).approve(anyLong(), anyInt());
		}

		@Test
		@DisplayName("ステータスがDRAFTの場合はBusinessExceptionをスロー")
		void ステータスがDRAFTの場合() {
			// Given
			Expense draftExpense = ExpenseTestBuilder.createDraft();
			long expenseId = draftExpense.getId();
			int version = draftExpense.getVersion();
			long actorId = 999L;

			given(expenseMapper.findById(expenseId)).willReturn(draftExpense);

			// When & Then
			assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("提出済み以外は承認できません");
		}

		@Test
		@DisplayName("ステータスがAPPROVEDの場合はBusinessExceptionをスロー")
		void ステータスがAPPROVEDの場合() {
			// Given
			Expense approvedExpense = ExpenseTestBuilder.createApproved();
			long expenseId = approvedExpense.getId();
			int version = approvedExpense.getVersion();
			long actorId = 999L;

			given(expenseMapper.findById(expenseId)).willReturn(approvedExpense);

			// When & Then
			assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("提出済み以外は承認できません");
		}

		@Test
		@DisplayName("ステータスがREJECTEDの場合はBusinessExceptionをスロー")
		void ステータスがREJECTEDの場合() {
			// Given
			Expense rejectedExpense = ExpenseTestBuilder.createRejected();
			long expenseId = rejectedExpense.getId();
			int version = rejectedExpense.getVersion();
			long actorId = 999L;

			given(expenseMapper.findById(expenseId)).willReturn(rejectedExpense);

			// When & Then
			assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("提出済み以外は承認できません");
		}

		@Test
		@DisplayName("バージョンが一致しない場合はBusinessExceptionをスロー")
		void バージョンが一致しない場合() {
			// Given
			long expenseId = submittedExpense.getId();
			int wrongVersion = 999;  // 不正なバージョン
			long actorId = 999L;

			given(expenseMapper.findById(expenseId)).willReturn(submittedExpense);

			// When & Then
			assertThatThrownBy(() -> expenseService.approve(expenseId, wrongVersion, actorId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("他のユーザに更新されています");
		}

		@Test
		@DisplayName("更新件数が0の場合はBusinessExceptionをスロー")
		void 更新件数が0の場合() {
			// Given
			long expenseId = submittedExpense.getId();
			int version = submittedExpense.getVersion();
			long actorId = 999L;

			given(expenseMapper.findById(expenseId)).willReturn(submittedExpense);
			given(expenseMapper.approve(expenseId, version)).willReturn(0);  // 更新失敗

			// When & Then
			assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("他のユーザに更新されています");
		}
	}

	@Nested
	@DisplayName("境界値")
	class BoundaryCase {

		@Test
		@DisplayName("メール送信が失敗しても処理は継続する")
		void メール送信失敗時の挙動() {
			// Given
			long expenseId = submittedExpense.getId();
			int version = submittedExpense.getVersion();
			long actorId = 999L;

			given(expenseMapper.findById(expenseId))
				.willReturn(submittedExpense)
				.willReturn(approvedExpense);
			given(expenseMapper.approve(expenseId, version)).willReturn(1);
			given(userMapper.findEmailById(submittedExpense.getApplicantId()))
				.willReturn("applicant@example.com");

			// メール送信で例外をスロー
			willThrow(new RuntimeException("Mail server error"))
				.given(notificationService).notifyApproved(anyString(), anyLong(), anyString());

			// When
			ExpenseResponse result = expenseService.approve(expenseId, version, actorId);

			// Then（例外が握りつぶされて処理は継続）
			assertThat(result.status()).isEqualTo(ExpenseStatus.APPROVED);

			// メール送信は試みられる
			then(notificationService).should().notifyApproved(anyString(), anyLong(), anyString());
		}

		@Test
		@DisplayName("バージョンが0の場合も正常に処理される")
		void バージョンが0の場合() {
			// Given
			Expense expense = ExpenseTestBuilder.builder()
				.status(ExpenseStatus.SUBMITTED)
				.version(0)  // バージョン0
				.build();

			long expenseId = expense.getId();
			int version = 0;
			long actorId = 999L;

			Expense approvedExpense = ExpenseTestBuilder.builder()
				.id(expense.getId())
				.status(ExpenseStatus.APPROVED)
				.version(1)
				.build();

			given(expenseMapper.findById(expenseId))
				.willReturn(expense)
				.willReturn(approvedExpense);
			given(expenseMapper.approve(expenseId, version)).willReturn(1);
			given(userMapper.findEmailById(expense.getApplicantId()))
				.willReturn("applicant@example.com");

			// When
			ExpenseResponse result = expenseService.approve(expenseId, version, actorId);

			// Then
			assertThat(result.status()).isEqualTo(ExpenseStatus.APPROVED);
			assertThat(result.version()).isEqualTo(1);
		}
	}
}
