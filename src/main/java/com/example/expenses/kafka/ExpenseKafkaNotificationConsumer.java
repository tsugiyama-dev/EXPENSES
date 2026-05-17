package com.example.expenses.kafka;

import java.time.LocalDateTime;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.expenses.domain.Expense;
import com.example.expenses.dto.NotificationMessage;
import com.example.expenses.dto.NotificationMessage.NotificationType;
import com.example.expenses.notification.NotificationService;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.UserMapper;
import com.example.expenses.websocket.RedisWebSocketPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseKafkaNotificationConsumer {

	private final NotificationService notificationService;
	private final RedisWebSocketPublisher webSocketPublisher;
	private final ExpenseMapper expenseMapper;
	private final UserMapper userMapper;

	@KafkaListener(topics = ExpenseTopics.EXPENSE_EVENT, groupId = "expenses-notification")
	public void consume(ExpenseEventMessage message) {
		try {
			switch (message.getEventType()) {
			case SUBMITTED -> handleSubmitted(message);
			case APPROVED -> handleApproved(message);
			case REJECTED -> handleRejected(message);
			}
		} catch (Exception ex) {
			log.error("Kafka notification handling failed. type={}, expenseId={}",
					message.getEventType(), message.getExpenseId(), ex);
		}
	}

	private void handleSubmitted(ExpenseEventMessage message) {
		String approverEmail = userMapper.findAnyApproverEmail();
		notificationService.notifySubmitted(approverEmail, message.getExpenseId(), message.getTraceId());

		Expense expense = expenseMapper.findById(message.getExpenseId());
		if (expense == null) {
			log.warn("通知スキップ. 経費がみつかりません: {}", message.getExpenseId());
			return;
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
		if (expense == null) {
			log.warn("通知スキップ. 経費がみつかりません: {}", message.getExpenseId());
			return;
		}

		webSocketPublisher.sendToUser(message.getApplicantId(), buildMessage(
				NotificationType.EXPENSE_APPROVED,
				expense,
				applicantEmail,
				"承認された経費 #" + message.getExpenseId()));
	}

	private void handleRejected(ExpenseEventMessage message) {
		String applicantEmail = userMapper.findEmailById(message.getApplicantId());
		notificationService.notifyRejected(applicantEmail, message.getExpenseId(), message.getReason(), message.getTraceId());

		Expense expense = expenseMapper.findById(message.getExpenseId());
		if (expense == null) {
			log.warn("通知スキップ. 経費がみつかりません: {}", message.getExpenseId());
			return;
		}

		webSocketPublisher.sendToUser(message.getApplicantId(), buildMessage(
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
