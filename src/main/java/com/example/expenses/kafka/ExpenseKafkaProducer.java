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
	 * 送信成功/失敗をメトリクス化する
	 *  失敗カウンタ（expense.kafka.publish{result=failure})を監視・アラートに使う想定。
	 *  ログだけだと「失敗していたことに誰も気づかない」ため、数値として可視化する
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
				.description("Kafka へのイベント送信失敗数(リトライ尽き)")
				.register(meterRegistry);
	}
	
	public void publish(ExpenseEventMessage message) {
		// パーティション分散とキー単位の順序保証のため、キーにexpenseIdを使う
		String key = String.valueOf(message.getExpenseId());
		kafkaTemplate.send(ExpenseTopics.EXPENSE_EVENT, key, message)
		.whenComplete((result, ex) -> {
			if(ex != null) {
				/*
				 * ここに来るのは、冪等プロデューサーが
				 * delivery.timeout.ms の間リトライを続けても
				 * 送信成功を確認できなかった場合。
				 *
				 * 【重要】
				 * この publish は
				 * @TransactionalEventListener(AFTER_COMMIT)
				 * から呼ばれるため、
				 * DBトランザクションは既にコミット済み。
				 *
				 * そのため、ここで例外を送出しても
				 * DB更新をロールバックすることはできない。
				 *
				 * 結果として
				 *   DB更新成功
				 *   Kafka送信失敗（または成功未確認）
				 * という不整合状態が発生しうる。
				 *
				 * 本実装では最低限、
				 * ERRORログ出力と失敗メトリクス記録により
				 * 検知可能な状態を保証する。
				 *
				 * 消失リスクをさらに下げるには、
				 * Transactional Outbox パターン
				 * （業務データとイベントを同一DBトランザクションで保存し、
				 *  別コンポーネントがKafkaへ中継する方式）
				 * の採用が必要。
				 *
				 * 本アプリでは運用コストとのトレードオフを考慮し、
				 * 検知可能性を優先する設計とした。
				 */
				publishFailure.increment();
				log.error("Kafka publish failed expenseId={}, error={}",
						message.getExpenseId(), ex);
			}else {
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
