package com.example.expenses.event;

import lombok.Getter;

/**
 * 経費が却下されたときに発行されるイベント
 */
@Getter
public class ExpenseRejectedEvent extends ExpenseEvent {

	private final Long rejectorId; //却下者のID
	private final Long applicantId;//申請者のID
	private final String reason;//却下理由
	
	public ExpenseRejectedEvent(
			Long expenseId, Long rejectorId, String traceId,
			Long applicantId, String reason) {
		super(expenseId, rejectorId, traceId);
		this.rejectorId = rejectorId;
		this.applicantId = applicantId;
		this.reason = reason;
		
	
	}

}
