package com.example.expenses.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpenseKafkaConsumer {

	@KafkaListener(topics = ExpenseTopics.EXPENSE_EVENT, groupId = "expenses-app")
	public void consume(ExpenseEventMessage message) {
		log.info("Kafka consume: type={}, expenseId={}, actorId={}",
				message.getEventType(), message.getExpenseId(),message.getActorId());
		
		
		switch(message.getEventType()) {
		case SUBMITTED -> handleSubmitted(message);
		case APPROVED -> handleApproved(message);
		case REJECTED -> handleRejected(message);
		}
	}
	
	private void handleSubmitted(ExpenseEventMessage message) {
		log.info("[[Kafka] 経費申請 #{}が提出されました",message.getExpenseId());
		
		// TODO メール送信WebSocket通知などを移植
	}
	
	private void handleApproved(ExpenseEventMessage message) {
		log.info("[Kafka] 経費申請 #{} が承認されました（申請者ID: {})",
				message.getExpenseId(), message.getActorId());
		// todo 申請者へメール送信
	}
	
	private void handleRejected(ExpenseEventMessage message) {
		log.info("[Kafka] 経費申請 #{} が却下されました: {} (申請者ID:{})",
				message.getExpenseId(), message.getReason(), message.getActorId());
		
		// todo 個々にメール送信WebSocket通知を移植する
	}
}
