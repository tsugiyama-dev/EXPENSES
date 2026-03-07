package com.example.expenses.listener;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.event.ExpenseRejectedEvent;
import com.example.expenses.event.ExpenseSubmittedEvent;
import com.example.expenses.notification.NotificationService;
import com.example.expenses.repository.UserMapper;

/**
 * ExpenseNotificationListener の非同期処理テスト
 *
 * <p>このテストクラスでは、@Async アノテーションが正しく動作し、
 * メール通知が非同期で実行されることを検証します。
 *
 * <p>テストポイント:
 * <ul>
 *   <li>イベントを発行したら、非同期でメール送信が実行されること</li>
 *   <li>メール送信が失敗しても、例外がスローされないこと</li>
 *   <li>非同期で実行されることを確認（timeout を使用）</li>
 * </ul>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.mail.from=test@example.com"
})
class ExpenseNotificationListenerTest {

    @Autowired
    private ExpenseNotificationListener listener;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private UserMapper userMapper;

    @Test
    @DisplayName("経費が承認されたら、非同期でメール通知が送信される")
    void testHandleExpenseApproved_shouldSendEmailAsynchronously() {
        // Arrange
        Long expenseId = 1L;
        Long applicantId = 100L;
        Long approverId = 200L;
        String traceId = "trace-123";
        String applicantEmail = "applicant@example.com";

        when(userMapper.findEmailById(applicantId)).thenReturn(applicantEmail);

        ExpenseApprovedEvent event = new ExpenseApprovedEvent(
            expenseId,
            approverId,
            applicantId,
            traceId
        );

        // Act
        listener.handleExpenseApproved(event);

        // Assert
        // 非同期処理なので、timeout を使用して最大 5 秒待機
        verify(notificationService, timeout(5000).times(1))
            .notifyApproved(eq(applicantEmail), eq(expenseId), eq(traceId));
    }

    @Test
    @DisplayName("経費が却下されたら、非同期でメール通知が送信される")
    void testHandleExpenseReject_shouldSendEmailAsynchronously() {
        // Arrange
        Long expenseId = 2L;
        Long applicantId = 100L;
        Long approverId = 200L;
        String reason = "予算超過";
        String traceId = "trace-456";
        String applicantEmail = "applicant@example.com";

        when(userMapper.findEmailById(applicantId)).thenReturn(applicantEmail);

        ExpenseRejectedEvent event = new ExpenseRejectedEvent(
            expenseId,
            approverId,  // rejectorId
            traceId,
            applicantId,
            reason
        );

        // Act
        listener.handleExpenseReject(event);

        // Assert
        // 非同期処理なので、timeout を使用して最大 5 秒待機
        verify(notificationService, timeout(5000).times(1))
            .notifyRejected(eq(applicantEmail), eq(expenseId), eq(reason), eq(traceId));
    }

    @Test
    @DisplayName("経費が提出されたら、非同期でメール通知が送信される")
    void testHandleExpenseSubmitted_shouldSendEmailAsynchronously() {
        // Arrange
        Long expenseId = 3L;
        Long applicantId = 100L;
        String traceId = "trace-789";
        String approverEmail = "approver@example.com";

        when(userMapper.findAnyApproverEmail()).thenReturn(approverEmail);

        ExpenseSubmittedEvent event = new ExpenseSubmittedEvent(
            expenseId,
            applicantId,
            traceId
        );

        // Act
        listener.handleExpenseSubmitted(event);

        // Assert
        // 非同期処理なので、timeout を使用して最大 5 秒待機
        verify(notificationService, timeout(5000).times(1))
            .notifySubmitted(eq(approverEmail), eq(expenseId), eq(traceId));
    }

    @Test
    @DisplayName("メール送信が失敗しても、例外がスローされない")
    void testHandleExpenseApproved_whenEmailFails_shouldNotThrowException() {
        // Arrange
        Long expenseId = 4L;
        Long applicantId = 100L;
        Long approverId = 200L;
        String traceId = "trace-999";

        when(userMapper.findEmailById(applicantId))
            .thenThrow(new RuntimeException("Database error"));

        ExpenseApprovedEvent event = new ExpenseApprovedEvent(
            expenseId,
            approverId,
            applicantId,
            traceId
        );

        // Act & Assert
        // 例外がスローされないことを確認（非同期なので、待機が必要）
        listener.handleExpenseApproved(event);

        // エラーログが出力されるまで待機
        verify(userMapper, timeout(5000).times(1)).findEmailById(applicantId);
    }
}
