package com.example.expenses.listener;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.expenses.dto.NotificationMessage;
import com.example.expenses.dto.NotificationMessage.NotificationType;
import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.event.ExpenseRejectedEvent;
import com.example.expenses.event.ExpenseSubmittedEvent;
import com.example.expenses.websocket.RedisWebSocketPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 経費イベントを Redis 経由で WebSocket 通知へ変換するリスナー。
 * Redis を挟むことで複数インスタンスでもすべてのクライアントに通知が届く。
 * - 提出：全員へブロードキャスト
 * - 承認/却下：申請者個人へ送信
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseWebSocketNotificationListener {

    private final RedisWebSocketPublisher publisher;

    @EventListener
    public void handleSubmitted(ExpenseSubmittedEvent event) {
        var msg = NotificationMessage.builder()
                .type(NotificationType.EXPENSE_SUBMITTED)
                .expenseId(event.getExpenseId())
                .message("経費申請 #" + event.getExpenseId() + " が提出されました")
                .timestamp(LocalDateTime.now())
                .build();

        log.debug("Redis publish: SUBMITTED expenseId={}", event.getExpenseId());
        publisher.broadcast(msg);
    }

    @EventListener
    public void handleApproved(ExpenseApprovedEvent event) {
        var msg = NotificationMessage.builder()
                .type(NotificationType.EXPENSE_APPROVED)
                .expenseId(event.getExpenseId())
                .message("経費申請 #" + event.getExpenseId() + " が承認されました")
                .timestamp(LocalDateTime.now())
                .build();

        log.debug("Redis publish: APPROVED expenseId={}, applicantId={}", event.getExpenseId(), event.getApplicantId());
        publisher.sendToUser(event.getApplicantId(), msg);
    }

    @EventListener
    public void handleRejected(ExpenseRejectedEvent event) {
        var msg = NotificationMessage.builder()
                .type(NotificationType.EXPENSE_REJECTED)
                .expenseId(event.getExpenseId())
                .message("経費申請 #" + event.getExpenseId() + " が却下されました: " + event.getReason())
                .timestamp(LocalDateTime.now())
                .build();

        log.debug("Redis publish: REJECTED expenseId={}, applicantId={}", event.getExpenseId(), event.getApplicantId());
        publisher.sendToUser(event.getApplicantId(), msg);
    }
}
