package com.example.expenses.kafka;

//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class ExpenseKafkaBridgeListener {
//
//	private final ExpenseKafkaProducer producer;
//
//	@EventListener
//	public void onSubmitted(ExpenseSubmittedEvent event) {
//		var msg = new ExpenseEventMessage(
//				EventType.SUBMITTED,
//				event.getExpenseId(),
//				event.getActorId(),
//				event.getApplicantId(),
//				null,
//				event.getTraceId());
//
//		log.debug("Bridge => Kafka SUBMITTED expenseId={}", event.getExpenseId());
//		producer.publish(msg);
//	}
//
//	@EventListener
//	public void onApproved(ExpenseApprovedEvent event) {
//		var msg = new ExpenseEventMessage(
//				EventType.APPROVED,
//				event.getExpenseId(),
//				event.getApproverId(),
//				event.getApplicantId(),
//				null,
//				event.getTraceId());
//
//		log.debug("Bridge => Kafka APPROVED expenseId={}", event.getExpenseId());
//		producer.publish(msg);
//	}
//
//	@EventListener
//	public void onRejected(ExpenseRejectedEvent event) {
//		var msg = new ExpenseEventMessage(
//				EventType.REJECTED,
//				event.getExpenseId(),
//				event.getRejectorId(),
//				event.getApplicantId(),
//				event.getReason(),
//				event.getTraceId());
//
//		log.debug("Bridge => Kafka REJECTED expenseId={}", event.getExpenseId());
//		producer.publish(msg);
//	}
//}
