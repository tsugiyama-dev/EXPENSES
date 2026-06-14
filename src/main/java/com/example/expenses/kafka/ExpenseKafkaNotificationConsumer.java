package com.example.expenses.kafka;

import java.time.LocalDateTime;
import java.util.Objects;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.example.expenses.domain.Expense;
import com.example.expenses.dto.NotificationMessage;
import com.example.expenses.dto.NotificationMessage.NotificationType;
import com.example.expenses.notification.NotificationService;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.UserMapper;
import com.example.expenses.service.PendingNotificationService;
import com.example.expenses.websocket.RedisWebSocketPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 経費イベントを受け取って通知を送る Consumer。
 *
 * 【以前の問題点】
 *   consume() が try-catch で例外を丸呑みしていた。
 *   → 例外が起きてもオフセットがコミットされ、メッセージは再処理されない。
 *   → 通知が完全にサイレント消滅する。
 *
 * 【変更後】
 *   @RetryableTopic でリトライ + DLT（Dead Letter Topic）を使う。
 *   1. 例外が発生 → Spring Kafka が自動でリトライトピックへ転送
 *   2. attempts=4 回（初回 + 3 リトライ）失敗 → expense-events.DLT へ退避
 *   3. @DltHandler で DLT を受け取ってエラー記録
 *
 * 【なぜ try-catch を外すか】
 *   @RetryableTopic はメソッドから例外が投げられることを前提に動く。
 *   try-catch で握りつぶすと Kafka のリトライ機構に例外が届かず DLT に行かない。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseKafkaNotificationConsumer {

	private final NotificationService notificationService;
	private final RedisWebSocketPublisher webSocketPublisher;
	private final ExpenseMapper expenseMapper;
	private final UserMapper userMapper;
	private final PendingNotificationService pendingNotificationService;

	/*
	 * attempts=4 : 初回 1回 + リトライ 3回（合計 4回試みる）
	 * backoff    : 1s → 2s → 4s（multiplier=2 の指数バックオフ）
	 * suffixingStrategy : リトライトピック名を -0, -1, -2 の連番にする
	 *
	 * 自動生成されるトピック：
	 *   expense-events-retry-0  （1回目リトライ）
	 *   expense-events-retry-1  （2回目リトライ）
	 *   expense-events-retry-2  （3回目リトライ）
	 *   expense-events.DLT      （全リトライ失敗後の退避先）
	 */
	@RetryableTopic(
		attempts = "4",
		backOff = @BackOff(delay = 1000, multiplier = 2),
		topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
	)
	@KafkaListener(topics = ExpenseTopics.EXPENSE_EVENT, groupId = "expenses-notification")
	public void consume(ExpenseEventMessage message) {
		switch (message.getEventType()) {
		case SUBMITTED -> handleSubmitted(message);
		case APPROVED -> handleApproved(message);
		case REJECTED -> handleRejected(message);
		}
	}

	/*
	 * 4回すべて失敗したメッセージが DLT から届く。
	 * 本番では：アラート送信・管理画面への記録・手動再処理キューへの積み直し等を行う。
	 * ここでは ERROR ログに記録するだけ（学習用の最小実装）。
	 */
	@DltHandler
	public void handleDlt(
			ConsumerRecord<String, ExpenseEventMessage> record,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
			@Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
		ExpenseEventMessage message = record.value();
		log.error("[DLT] 通知処理が最終的に失敗しました。手動確認が必要です。 "
				+ "topic={}, type={}, expenseId={}, cause={}",
				topic, message.getEventType(), message.getExpenseId(), exceptionMessage);
	}

	private void handleSubmitted(ExpenseEventMessage message) {
		String approverEmail = userMapper.findAnyApproverEmail();
		notificationService.notifySubmitted(approverEmail, message.getExpenseId(), message.getTraceId());

		Expense expense = expenseMapper.findById(message.getExpenseId());
		if (Objects.isNull(expense)) {
			log.warn("通知スキップ. 経費がみつかりません: {}", message.getExpenseId());
			return;
		}

		Long approverId = userMapper.findAnyApproverId();
		if (Objects.nonNull(approverId)) {
			pendingNotificationService.save(
					approverId,
					NotificationType.EXPENSE_SUBMITTED.name(),
					message.getExpenseId(),
					"提出された経費 #" + message.getExpenseId(),
					expense.getTitle(),
					expense.getAmount());
		}

		String applicantEmail = userMapper.findEmailById(expense.getApplicantId());
		webSocketPublisher.broadcast(buildMessage(
				NotificationType.EXPENSE_SUBMITTED,
				expense,
				applicantEmail,
				"提出された経費 #" + message.getExpenseId()));
	}

	private void handleApproved(ExpenseEventMessage message) {
		String applicantEmail = userMapper.findEmailById(message.getApplicantId());
		notificationService.notifyApproved(applicantEmail, message.getExpenseId(), message.getTraceId());

		Expense expense = expenseMapper.findById(message.getExpenseId());
		if (Objects.isNull(expense)) {
			log.warn("通知スキップ. 経費がみつかりません: {}", message.getExpenseId());
			return;
		}

		pendingNotificationService.save(
				message.getApplicantId(),
				NotificationType.EXPENSE_APPROVED.name(),
				message.getExpenseId(),
				"承認された経費 #" + message.getExpenseId(),
				expense.getTitle(),
				expense.getAmount());

		webSocketPublisher.sendToUser(buildMessage(
				NotificationType.EXPENSE_APPROVED,
				expense,
				applicantEmail,
				"承認された経費 #" + message.getExpenseId()));
	}

	private void handleRejected(ExpenseEventMessage message) {
		String applicantEmail = userMapper.findEmailById(message.getApplicantId());
		notificationService.notifyRejected(applicantEmail, message.getExpenseId(), message.getReason(), message.getTraceId());

		Expense expense = expenseMapper.findById(message.getExpenseId());
		if (Objects.isNull(expense)) {
			log.warn("通知スキップ. 経費がみつかりません: {}", message.getExpenseId());
			return;
		}

		pendingNotificationService.save(
				message.getApplicantId(),
				NotificationType.EXPENSE_REJECTED.name(),
				message.getExpenseId(),
				"却下された経費 #" + message.getExpenseId(),
				expense.getTitle(),
				expense.getAmount());

		webSocketPublisher.sendToUser(buildMessage(
				NotificationType.EXPENSE_REJECTED,
				expense,
				applicantEmail,
				"却下された経費 #" + message.getExpenseId()));
	}

	private NotificationMessage buildMessage(
			NotificationType type,
			Expense expense,
			String applicantEmail,
			String text) {
		return NotificationMessage.builder()
				.type(type)
				.expenseId(expense.getId())
				.title(expense.getTitle())
				.amount(expense.getAmount().toString())
				.applicantEmail(applicantEmail)
				.message(text)
				.timestamp(LocalDateTime.now())
				.build();
	}
}
