package com.example.expenses.event;

import lombok.Getter;

/**
 * 経費が提出されたときに発行されるイベント
 */
@Getter
public class ExpenseSubmittedEvent extends ExpenseEvent {

	private final Long applicantId;//申請者のID
	
	public ExpenseSubmittedEvent(
			Long expenseId, Long applicantId, String traceId) {
		super(expenseId, applicantId, traceId);
		this.applicantId = applicantId;
		
	}

}
