package com.example.expenses.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpenseKafkaAnalyticsConsumer {

	@KafkaListener(topics = ExpenseTopics.EXPENSE_EVENT, groupId = "expenses-analytics")
	public void consume(ExpenseEventMessage message) {
		switch (message.getEventType()) {
		case SUBMITTED -> log.info("[Kafka analytics] count submitted expenseId={}", message.getExpenseId());
		case APPROVED -> log.info("[Kafka analytics] count approved expenseId={}", message.getExpenseId());
		case REJECTED -> log.info("[Kafka analytics] count rejected expenseId={}, reason={}",
				message.getExpenseId(), message.getReason());
		}
	}
}
