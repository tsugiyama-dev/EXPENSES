package com.example.expenses.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 経費イベントを Kafka トピックへ publish する。
 *
 * KafkaTemplate は RedisTemplate と同じ発想:
 *   send(topic, message) → Kafka ブローカーへ非同期送信
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseKafkaProducer {

    private final KafkaTemplate<String, ExpenseEventMessage> kafkaTemplate;

    public void publish(ExpenseEventMessage message) {
        // キーに expenseId を使うことで同一経費の順序を保証
        String key = String.valueOf(message.getExpenseId());
        kafkaTemplate.send(ExpenseTopics.EXPENSE_EVENTS, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish failed: expenseId={}", message.getExpenseId(), ex);
                    } else {
                        log.debug("Kafka publish OK: topic={}, offset={}, expenseId={}",
                                ExpenseTopics.EXPENSE_EVENTS,
                                result.getRecordMetadata().offset(),
                                message.getExpenseId());
                    }
                });
    }
}
