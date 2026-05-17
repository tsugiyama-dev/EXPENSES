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
		EXPENSE_SUBMITTED,
		EXPENSE_APPROVED,
		EXPENSE_REJECTED
	}

	private NotificationType type;
	private Long expenseId;
	private String applicantEmail;
	private String approverName;
	private String title;
	private String amount;
	private String message;
	private LocalDateTime timestamp;
}
