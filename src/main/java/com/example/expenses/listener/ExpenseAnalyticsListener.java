package com.example.expenses.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.event.ExpenseRejectedEvent;
import com.example.expenses.event.ExpenseSubmittedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 経費分析のリスナー
 * -経費イベントを監視して分析処理を実行する
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseAnalyticsListener {

	//分析サービス（実装予定）
	//private final ExpenseAnalyticsService analyticsService;
	
	
	/**
	 * 承認イベントを記録
	 */
	@EventListener
	public void onExpenseApproved(ExpenseApprovedEvent event) {
		log.info("経費分析: 承認イベント受信: expenseId={}, approverId={}", event.getExpenseId(), event.getApproverId());
		
		//分析サービスを呼び出して承認イベントを記録
		//分析データを記録
		//承認までの時間
		//承認者別の承認数
		//時間帯別の承認パターン
		//analyticsService.recordApproval(event);
		
		log.info("経費分析: 承認イベント記録完了: expenseId={}", event.getExpenseId());
		
	}
	@EventListener
	public void onExpenseRejected(ExpenseRejectedEvent event) {
		log.info("経費分析: 却下イベント受信: expenseId={}, rejectorId={}",
				event.getExpenseId(), event.getRejectorId());
		
		//却下理由の分析
		//却下率の計算
		//却下理由のカテゴリ分類
		//analyticsService.recordRejection(event);
		
		log.info("経費分析: 却下イベント記録完了: expenseId={}", event.getExpenseId());
		
	}
	
	@EventListener
	public void onExpenseSubmitted(ExpenseSubmittedEvent event) {
		log.info("経費分析: 提出イベント受信: expenseId={}, applicantId={}",
				event.getExpenseId(), event.getApplicantId());
		
		//提出パターンの分析
		//-ユーザ別提出頻度
		//-提出時間帯の傾向
		//analyticsService.recordSubmission(event);
		
		log.info("経費分析: 提出イベント記録完了: expenseId={}", event.getExpenseId());
		
	}
	
	
}
