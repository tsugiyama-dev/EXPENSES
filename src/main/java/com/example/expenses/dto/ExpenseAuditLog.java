package com.example.expenses.dto;

import java.time.LocalDateTime;

import com.example.expenses.domain.ExpenseStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExpenseAuditLog {

	private Long id;
	private Long expenseId;
	private Long actorId;
	private String action;
	private String beforeStatus;
	private String afterStatus;
	private String note;
	private String traceId;
	private LocalDateTime createdAt;
	
	public static ExpenseAuditLog create(Long expenseId, Long actorId, String traceId) {
		return new ExpenseAuditLog(
				null,
				expenseId,
				actorId,
				ExpenseStatus.CREATE.toString(), //action
				null, //beforeStatus
				ExpenseStatus.DRAFT.toString(), //afterStatus
				null, //note
				traceId,
				LocalDateTime.now());
	}
	
	public static ExpenseAuditLog createDraft(Long expenseId, Long actorId, String traceId) {
		
		return new ExpenseAuditLog(
				null,
				expenseId,
				actorId,
				ExpenseStatus.SUBMIT.toString(), //action
				ExpenseStatus.DRAFT.toString(), //beforeStatus
				ExpenseStatus.SUBMITTED.toString(), //afterStatus
				null, //note
				traceId,
				LocalDateTime.now()); //createdAt
		
	}
	public static ExpenseAuditLog createApprove(Long expenseId, Long actorId, String traceId) {
		
		return new ExpenseAuditLog(
				null,
				expenseId, //expenseId
				actorId,
				"APPROVE", //action
				"SUBMITTED",// beforeStatus
				"APPROVED", // afterStatus
				"", //note
				traceId,
				null //createdAt
				);
	}
	
	public static ExpenseAuditLog createReject(Long expenseId, Long actorId, String traceId, String reason) {
		
		return new ExpenseAuditLog(
				null,
				expenseId,
				actorId,
				"APPROVE",
				"SUBMITTED",
				"REJECTED",
				reason,
				traceId,
				null);
	}
	
	
}


