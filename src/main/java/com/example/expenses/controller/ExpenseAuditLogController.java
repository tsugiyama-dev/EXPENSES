package com.example.expenses.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.expenses.dto.response.ExpenseAuditLogResponse;
import com.example.expenses.service.ExpenseAuditLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/expenses")
public class ExpenseAuditLogController {

	private final ExpenseAuditLogService auditLogService;
	
	@GetMapping("/{id}/audit-logs")
	public ResponseEntity<List<ExpenseAuditLogResponse>> getLogs(
			@PathVariable("id") Long expenseId) {
		
		return ResponseEntity.ok().body(auditLogService.getLogs(expenseId));
	}
	
}
