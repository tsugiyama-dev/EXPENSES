package com.example.expenses.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingNotification {

	private Long id;
	private Long userId;
	private String type;
	private Long expenseId;
	private String message;
	private boolean read;
	private LocalDateTime createdAt;

	public static PendingNotification create(Long userId, String type, Long expenseId, String message) {
		return new PendingNotification(null, userId, type, expenseId, message, false, null);
	}
}
