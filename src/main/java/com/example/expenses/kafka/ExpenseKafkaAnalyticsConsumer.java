package com.example.expenses.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpenseKafkaAnalyticsConsumer {

	
	private final Counter submittedCounter;
	private final Counter approvedCounter;
	private final Counter rejectedCounter;
	
	public ExpenseKafkaAnalyticsConsumer(MeterRegistry meterRegistry) {
		this.submittedCounter = Counter.builder("expense.event.count")
				.tag("type", "SUBMITTED")
				.description("経費提出イベントの受信件数")
				.register(meterRegistry);
		
		this.approvedCounter = Counter.builder("expense.event.count")
				.tag("type", "APPROVED")
				.description("経費承認イベントの受信件数")
				.register(meterRegistry);
		
		this.rejectedCounter = Counter.builder("expense.event.count")
				.tag("type", "REJECTED")
				.description("経費却下イベントの受信件数")
				.register(meterRegistry);
		
	}
	@KafkaListener(topics = ExpenseTopics.EXPENSE_EVENT, groupId = "expenses-analytics")
	public void consume(ExpenseEventMessage message) {
		switch (message.getEventType()) {
		case SUBMITTED ->  {
			submittedCounter.increment();
		    log.info("[Kafka analytics] count submitted expenseId={}", message.getExpenseId());
		}
		case APPROVED -> {
			approvedCounter.increment();
            log.info("[Kafka analytics] count approved expenseId={}", message.getExpenseId());
		}
		case REJECTED -> {
			rejectedCounter.increment();
			log.info("[Kafka analytics] count rejected expenseId={}, reason={}",message.getExpenseId(), message.getReason());
		}
		}
	}
}
