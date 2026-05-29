package com.example.expenses.kafka;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.event.ExpenseRejectedEvent;
import com.example.expenses.event.ExpenseSubmittedEvent;
import com.example.expenses.kafka.ExpenseEventMessage.EventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @EventListenerはNG！
 * トランザクション実行中にイベントが発火する
 * イベント処理後にDBがロールバックしてもKafkaにはメッセージが届いている
 * 
 * @TransactionalEventListener(phase = AFTER_COOMIT)を使う！
 * DB トランザクションが正常にコミットされた後にのみイベントが発火する
 * 
 * phase の選択肢
 *     AFTER_COMMIT: トランザクションが正常にコミットされた後にイベントが発火
 *     AFTER_ROLLBACK: トランザクションがロールバックされた後にイベントが発火
 *     BEFORE_COMMIT: トランザクションがコミットされる前にイベントが発火
 *     AFTER_COMPLETION: コミット・ロールバックのどちらでもイベントが発火
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ExpenseKafkaBridgeListener {

	private final ExpenseKafkaProducer producer;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
