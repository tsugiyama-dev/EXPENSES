package com.example.expenses.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.notification.NotificationService;
import com.example.expenses.repository.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 経費イベントを監視してメール通知を送信するリスナー
 * -@EventListenerを使用してイベントを購読
 * -@ayncを使用して非同期に処理
 * -通知失敗してもメイン処理に影響を与えないようにする
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseNotificationListener {

	private final NotificationService notificationService;//通知サービスの依存注入
	private final UserMapper userMapper;//ユーザーマッパーの依存注入
	
	/**
	 * 経費が承認されたときの通知
	 */
	@EventListener
	public void handleExpenseApproved(ExpenseApprovedEvent event) {
		try {
			
			log.info("承認イベント受信: expenseId={}, approverId={},", event.getExpenseId(), event.getApproverId());
			
			String applicantEmail = userMapper.findEmailById(event.getApplicantId());
			
			//通知サービスを呼び出して承認通知メールを送信
			notificationService.notifyApproved(
					applicantEmail,
					event.getExpenseId(),
					event.getTraceId());
			
			log.info("承認通知メール送信完了：expenseId={}", event.getExpenseId());
		}catch (Exception e) {
			log.error("承認通知メール送信失敗: expenseId={}, approverId={}, error={}", event.getExpenseId(),event.getApplicantId(), e.getMessage());
			
		}
	}
}
