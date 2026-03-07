package com.example.expenses.event;

import lombok.Getter;

/**
 * 経費申請が承認されたことを表すイベントクラス
 */
@Getter
public class ExpenseApprovedEvent extends ExpenseEvent {

	private final Long approverId; //承認者のID
	private final Long applicantId; //申請者のID
	
	
	public ExpenseApprovedEvent(
			Long expenseId, Long approverId,
			Long applicantId, String traceId) {
		
		super(expenseId, approverId, traceId);
		this.approverId = approverId;
		this.applicantId = applicantId;
		
	}

}
