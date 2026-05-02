package com.example.expenses.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka トピック "expense-events" を購読する Consumer。
 *
 * group-id "expenses-app" を持つ Consumer Group の一員として動作する。
 * 複数インスタンスを起動した場合、同一グループ内でパーティションが分散される
 * （各メッセージはグループ内の1インスタンスだけが処理する）。
 *
 * ← Redis Pub/Sub との違い:
 *    Redis: 全インスタンスが同じメッセージを受け取る（ブロードキャスト）
 *    Kafka: Consumer Group 内で分散処理（1メッセージ = 1インスタンスが処理）
 */
@Component
@Slf4j
public class ExpenseKafkaConsumer {

    @KafkaListener(topics = ExpenseTopics.EXPENSE_EVENTS, groupId = "expenses-app")
    public void consume(ExpenseEventMessage message) {
        log.info("Kafka consume: type={}, expenseId={}, actorId={}",
                message.getEventType(), message.getExpenseId(), message.getActorId());

        switch (message.getEventType()) {
            case SUBMITTED -> handleSubmitted(message);
            case APPROVED  -> handleApproved(message);
            case REJECTED  -> handleRejected(message);
        }
    }

    private void handleSubmitted(ExpenseEventMessage message) {
        log.info("[Kafka] 経費申請 #{} が提出されました", message.getExpenseId());
        // TODO: ここにメール送信・WebSocket通知などを移植する
    }

    private void handleApproved(ExpenseEventMessage message) {
        log.info("[Kafka] 経費申請 #{} が承認されました（申請者ID: {}）",
                message.getExpenseId(), message.getActorId());
        // TODO: 申請者へメール送信
    }

    private void handleRejected(ExpenseEventMessage message) {
        log.info("[Kafka] 経費申請 #{} が却下されました: {} （申請者ID: {}）",
                message.getExpenseId(), message.getReason(), message.getActorId());
        // TODO: 申請者へメール送信
    }
}
