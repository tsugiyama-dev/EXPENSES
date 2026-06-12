package com.example.expenses.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingNotification {

	private Long id;
	private Long userId;
	private String type;
	private Long expenseId;
	private String message;
	private String title;
	private BigDecimal amount;
	private boolean read;
	private LocalDateTime createdAt;
	
	public static PendingNotification create(Long userId, String type, Long expenseId, String message,
			String title, BigDecimal amount) {
		return new PendingNotification(null, userId, type, expenseId, message, title, amount,  false, null);
	}
}
