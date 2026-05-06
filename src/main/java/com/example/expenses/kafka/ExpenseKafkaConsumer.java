package com.example.expenses.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpenseKafkaConsumer {

	@KafkaListener(topics = ExpenseTopics.EXPENSE_EVENT, groupId = "expenses-audit-log")
	public void consume(ExpenseEventMessage message) {
		log.info("Kafka audit event: type={}, expenseId={}, actorId={}, applicantId={}",
				message.getEventType(), message.getExpenseId(), message.getActorId(), message.getApplicantId());

		switch (message.getEventType()) {
		case SUBMITTED -> handleSubmitted(message);
		case APPROVED -> handleApproved(message);
		case REJECTED -> handleRejected(message);
		}
	}

	private void handleSubmitted(ExpenseEventMessage message) {
		log.info("[Kafka audit] expense #{} was submitted", message.getExpenseId());
	}

	private void handleApproved(ExpenseEventMessage message) {
		log.info("[Kafka audit] expense #{} was approved by actorId={} for applicantId={}",
				message.getExpenseId(), message.getActorId(), message.getApplicantId());
	}

	private void handleRejected(ExpenseEventMessage message) {
		log.info("[Kafka audit] expense #{} was rejected by actorId={}. reason={}, applicantId={}",
				message.getExpenseId(), message.getActorId(), message.getReason(), message.getApplicantId());
	}
}
