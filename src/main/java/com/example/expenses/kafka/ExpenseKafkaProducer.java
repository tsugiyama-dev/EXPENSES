package com.example.expenses.kafka;


import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseKafkaProducer {

	private final KafkaTemplate<String, ExpenseEventMessage> kafkaTemplate;
	
	public void publish(ExpenseEventMessage message) {
		// key expenseId
		String key = String.valueOf(message.getExpenseId());
		kafkaTemplate.send(ExpenseTopics.EXPENSE_EVENT, key, message)
		.whenComplete((result, ex) -> {
			if(ex != null) {
				log.error("Kafka publish failed expenseId={}",
						message.getExpenseId(), ex);
			}else {
				log.debug("Kafka publish OK topic={}, offset={}, expenseId={}",
						ExpenseTopics.EXPENSE_EVENT,
						result.getRecordMetadata().offset(),
						message.getExpenseId());
			}
		});
	}
}
