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
 * Spring Events → Kafka へのブリッジ。
 *
 * 【役割の分担】
 *   ExpenseService  : ビジネスロジック（DB 保存 + Spring Event 発行）
 *   このクラス      : インフラ連携（Kafka へのメッセージ送信）
 *
 * 【なぜ @TransactionalEventListener(phase = AFTER_COMMIT) を使うか】
 *
 *   @EventListener を使った場合（NG）：
 *     トランザクション実行中にイベントが発火する。
 *     → イベント処理後に DB がロールバックしても、Kafka にはすでにメッセージが届いている。
 *     → 「DB には存在しない経費の承認通知が届く」という不整合が起きる。
 *
 *   @TransactionalEventListener(phase = AFTER_COMMIT) を使った場合（OK）：
 *     DB トランザクションが正常にコミットされた後にのみイベントが発火する。
 *     → DB と Kafka のメッセージが常に整合する。
 *
 *   phase の選択肢：
 *     AFTER_COMMIT  (デフォルト) : コミット成功後に発火 ← 今回の使用箇所
 *     AFTER_ROLLBACK             : ロールバック後に発火（補償処理など）
 *     AFTER_COMPLETION           : コミット or ロールバックのどちらでも発火
 *     BEFORE_COMMIT              : コミット前に発火（バリデーション用途など）
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
