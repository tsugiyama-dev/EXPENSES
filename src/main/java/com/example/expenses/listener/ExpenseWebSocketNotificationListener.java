package com.example.expenses.listener;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.expenses.controller.NotificationWebSocketController;
import com.example.expenses.dto.NotificationMessage;
import com.example.expenses.dto.NotificationMessage.NotificationType;
import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.event.ExpenseRejectedEvent;
import com.example.expenses.event.ExpenseSubmittedEvent;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.ExpenseMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseWebSocketNotificationListener {

	private final NotificationWebSocketController wsController;
	private final ExpenseMapper expenseMapper;
	
	@EventListener
	public void handleSubmitted(ExpenseSubmittedEvent event) {
		
		var expense = expenseMapper.findById(event.getExpenseId());
		if(expense == null) throw new BusinessException("経費が見つかりません：" + event.getExpenseId());
		var msg = NotificationMessage.builder()
				.type(NotificationType.EXPENSE_SUBMITTED)
				.expenseId(event.getExpenseId())
				.title(expense.getTitle())
				.amount(expense.getAmount().toString())
				.applicantName("申請者ID：" + String.valueOf(expense.getApplicantId())) // 一時的に申請者名ではなくID
 				.message("経費申請 #" + event.getExpenseId() + "が提出されました")
				.timestamp(LocalDateTime.now())
				.build();
		
		log.debug("WebSocket broadcast: SUBMITTED expenseId={}", event.getExpenseId());
		wsController.broadcastNotification(msg);
	}
	
	@EventListener
	public void handleApproved(ExpenseApprovedEvent event) {
		
		var expense = expenseMapper.findById(event.getExpenseId());
		if(expense == null) throw new BusinessException("経費が見つかりません：" + event.getExpenseId());
		
		var msg = NotificationMessage.builder()
				.type(NotificationType.EXPENSE_APPROVED)
				.expenseId(event.getExpenseId())
				.title(expense.getTitle())
				.amount(expense.getAmount().toString())
				.applicantName("申請者ID：" + String.valueOf(expense.getApplicantId())) // 一時的に申請者名ではなくID
				.message("経費申請 #" + event.getExpenseId() + "が承認されました")
				.timestamp(LocalDateTime.now())
				.build();
		
		log.debug("WebSocket personal: APPROVED expenseId={}, applicantId={}", event.getExpenseId(), event.getApplicantId());
		wsController.sendNotificationToUser(event.getApplicantId(), msg);
	}
	
	@EventListener
	public void handleRejected(ExpenseRejectedEvent event) {
		
		var expense = expenseMapper.findById(event.getExpenseId());
		if(expense == null) throw new BusinessException("経費が見つかりません：" + event.getExpenseId());
		
		var msg = NotificationMessage.builder()
				.type(NotificationType.EXPENSE_REJECTED)
				.expenseId(event.getExpenseId())
				.title(expense.getTitle())
				.amount(expense.getAmount().toString())
				.applicantName("申請者ID：" + String.valueOf(expense.getApplicantId())) // 一時的に申請者名ではなくID
				.message("経費申請 #" + event.getExpenseId() + "が却下されました")
				.timestamp(LocalDateTime.now())
				.build();
		
		log.debug("WebSocket personal: REJECTED expenseId={}, applicantId={}", event.getExpenseId(), event.getApplicantId());
		wsController.sendNotificationToUser(event.getApplicantId(), msg);
	}
}
