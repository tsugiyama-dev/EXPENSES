package com.example.expenses.kafka;


import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpenseKafkaProducer {

	private final KafkaTemplate<String, ExpenseEventMessage> kafkaTemplate;

	/*
	 * 送信成功/失敗をメトリクス化する。
	 *   失敗カウンタ（expense.kafka.publish{result=failure}）を監視・アラートに使う想定。
	 *   ログだけだと「失敗していたことに誰も気づかない」ため、数値として可視化する。
	 */
	private final Counter publishSuccess;
	private final Counter publishFailure;

	public ExpenseKafkaProducer(
			KafkaTemplate<String, ExpenseEventMessage> kafkaTemplate,
			MeterRegistry meterRegistry) {
		this.kafkaTemplate = kafkaTemplate;
		this.publishSuccess = Counter.builder("expense.kafka.publish")
				.tag("result", "success")
				.description("Kafka へのイベント送信成功数")
				.register(meterRegistry);
		this.publishFailure = Counter.builder("expense.kafka.publish")
				.tag("result", "failure")
				.description("Kafka へのイベント送信失敗数（リトライ尽き）")
				.register(meterRegistry);
	}

	public void publish(ExpenseEventMessage message) {
		// パーティション分散とキー単位の順序保証のため、キーに expenseId を使う
		String key = String.valueOf(message.getExpenseId());

		kafkaTemplate.send(ExpenseTopics.EXPENSE_EVENT, key, message)
		.whenComplete((result, ex) -> {
			if (ex != null) {
				/*
				 * ここに来るのは「冪等プロデューサが delivery.timeout.ms の間
				 * リトライし尽くしてなお失敗」した最終失敗。
				 *
				 * 【正直な限界】
				 *   この publish は @TransactionalEventListener(AFTER_COMMIT) から
				 *   呼ばれる。つまり DB はすでにコミット済みで、ここで例外を投げても
				 *   ロールバックはできない。＝この時点での失敗はイベント消失を意味する。
				 *   そのため最低限「ERROR ログ＋失敗カウンタ」で検知可能にしている。
				 *
				 *   消失を completely になくすには Transactional Outbox パターン
				 *   （イベントを業務データと同一トランザクションで outbox テーブルに保存し、
				 *    別プロセスが確実に Kafka へ中継する）が必要。
				 *   本アプリの規模では過剰と判断し、ここでは「検知できる状態」までを担保する。
				 */
				publishFailure.increment();
				log.error("Kafka publish failed (event lost risk) topic={}, expenseId={}, type={}",
						ExpenseTopics.EXPENSE_EVENT, message.getExpenseId(), message.getEventType(), ex);
			} else {
				publishSuccess.increment();
				log.debug("Kafka publish OK topic={}, partition={}, offset={}, expenseId={}",
						ExpenseTopics.EXPENSE_EVENT,
						result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset(),
						message.getExpenseId());
			}
		});
	}
}
