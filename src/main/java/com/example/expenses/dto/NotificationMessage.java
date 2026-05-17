package com.example.expenses.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationMessage {

	public enum NotificationType {
		EXPENSE_SUBMITTED, //経費申請
		EXPENSE_APPROVED, //承認
		EXPENSE_REJECTED // 却下
	}
	
	private NotificationType type;
	private Long expenseId;
	private String applicantName;
	private String approverName;
	private String title;
	private String amount;
	private String message;
	private LocalDateTime timestamp;
}
