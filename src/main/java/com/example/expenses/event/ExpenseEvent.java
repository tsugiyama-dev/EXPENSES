package com.example.expenses.event;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * 経費申請に関連するイベントの基底クラス
 * すべてのイベントで共通の情報を保持
 */
@Getter
abstract public class ExpenseEvent {
	private final Long expenseId;
	private final Long actorId;
	private final String traceId;
	private final LocalDateTime occurredAt;
	
	protected ExpenseEvent(Long expenseId, Long actorId, String traceId) {
		this.expenseId = expenseId;
		this.actorId = actorId;
		this.traceId = traceId;
		this.occurredAt = LocalDateTime.now();
	}
	
	

	
}
