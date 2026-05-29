package com.example.expenses.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/*
 * Kafka Analytics Consumer：イベント種別ごとの受信件数を Prometheus メトリクスとして記録する。
 *
 * 【groupId = "expenses-analytics" のポイント】
 *   Kafka は groupId が異なるコンシューマーに同じメッセージを届ける。
 *   "expenses-local"（通知用）と "expenses-analytics"（分析用）が
 *   独立して同じトピックを消費できる。
 *
 * 【Micrometer Counter の使い方】
 *   Counter.builder("メトリクス名")
 *          .tag("キー", "値")     ← 多次元ラベル（Prometheus の label に対応）
 *          .register(meterRegistry)
 *   → /actuator/prometheus で以下のような形式で出力される：
 *     expense_event_count_total{type="SUBMITTED"} 3.0
 *     expense_event_count_total{type="APPROVED"}  1.0
 *     expense_event_count_total{type="REJECTED"}  1.0
 *
 * 【なぜイベント種別を tag にするか】
 *   メトリクス名を "expense_submitted_total" / "expense_approved_total" と分けることもできるが、
 *   tag にまとめると Grafana で type="SUBMITTED" / "APPROVED" / "REJECTED" を
 *   1つのグラフに重ねて表示しやすくなる。
 */
@Component
@Slf4j
public class ExpenseKafkaAnalyticsConsumer {

	private final Counter submittedCounter;
	private final Counter approvedCounter;
	private final Counter rejectedCounter;

	/*
	 * コンストラクタで Counter を MeterRegistry に登録する。
	 * Bean 生成時に1度だけ登録され、以降は increment() で加算するだけ。
	 * @Bean で別クラスに切り出す方法もあるが、使う場所に置くのがシンプル。
	 */
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
		case SUBMITTED -> {
			submittedCounter.increment();
			log.info("[analytics] SUBMITTED expenseId={}", message.getExpenseId());
		}
		case APPROVED -> {
			approvedCounter.increment();
			log.info("[analytics] APPROVED expenseId={}", message.getExpenseId());
		}
		case REJECTED -> {
			rejectedCounter.increment();
			log.info("[analytics] REJECTED expenseId={}, reason={}", message.getExpenseId(), message.getReason());
		}
		}
	}
}
