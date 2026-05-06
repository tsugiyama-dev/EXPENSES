package com.example.expenses.kafka;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.event.ExpenseRejectedEvent;
import com.example.expenses.event.ExpenseSubmittedEvent;
import com.example.expenses.kafka.ExpenseEventMessage.EventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExpenseKafkaBridgeListener {

	private final ExpenseKafkaProducer producer;

	@EventListener
	public void onSubmitted(ExpenseSubmittedEvent event) {
		var msg = new ExpenseEventMessage(
				EventType.SUBMITTED,
				event.getExpenseId(),
				event.getActorId(),
				event.getApplicantId(),
				null,
				event.getTraceId());

		log.debug("Bridge => Kafka SUBMITTED expenseId={}", event.getExpenseId());
		producer.publish(msg);
	}

	@EventListener
	public void onApproved(ExpenseApprovedEvent event) {
		var msg = new ExpenseEventMessage(
				EventType.APPROVED,
				event.getExpenseId(),
				event.getApproverId(),
				event.getApplicantId(),
				null,
				event.getTraceId());

		log.debug("Bridge => Kafka APPROVED expenseId={}", event.getExpenseId());
		producer.publish(msg);
	}

	@EventListener
	public void onRejected(ExpenseRejectedEvent event) {
		var msg = new ExpenseEventMessage(
				EventType.REJECTED,
				event.getExpenseId(),
				event.getRejectorId(),
				event.getApplicantId(),
				event.getReason(),
				event.getTraceId());

		log.debug("Bridge => Kafka REJECTED expenseId={}", event.getExpenseId());
		producer.publish(msg);
	}
}
