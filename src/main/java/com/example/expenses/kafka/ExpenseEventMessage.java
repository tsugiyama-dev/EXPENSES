package com.example.expenses.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka トピック "expense-events" に流すイベントメッセージ。
 * SpringEvent とは独立した、Kafka 専用のシリアライズ可能な DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseEventMessage {

    public enum EventType {
        SUBMITTED, APPROVED, REJECTED
    }

    private EventType eventType;
    private Long expenseId;
    private Long actorId;
    private String reason;
}
