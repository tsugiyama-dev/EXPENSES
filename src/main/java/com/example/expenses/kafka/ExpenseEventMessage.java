package com.example.expenses.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseEventMessage {

	public enum EventType {
		
		SUBMITTED, APPROVED, REJECTED
	}
	
	private EventType eventType;
	private Long expenseId;
	private Long actorId;
	private String reason;
	
}
