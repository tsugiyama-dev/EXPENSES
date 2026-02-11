package com.example.expenses.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.expenses.dto.ExpenseAuditLog;

public record ExpenseAuditLogResponse(
		Long id,
		Long expnseId,
		Long actorId,
		String action,
		String beforeStatus,
		String afterStatus,
		String note,
		String traceId,
		LocalDateTime createdAt) {
	
	public static List<ExpenseAuditLogResponse> toResponse(List<ExpenseAuditLog> logs) {
		
		return logs.stream().map(log -> new ExpenseAuditLogResponse(
				log.getId(),
				log.getExpenseId(),
				log.getActorId(),
				log.getAction(),
				log.getBeforeStatus(),
				log.getAfterStatus(),
				log.getNote(),
				log.getTraceId(),
				log.getCreatedAt())).toList();
				
	}

}
