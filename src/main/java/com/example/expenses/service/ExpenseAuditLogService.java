package com.example.expenses.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.dto.response.ExpenseAuditLogResponse;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.util.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseAuditLogService {

	private final ExpenseAuditLogMapper auditLogMapper;
	private final String PERMITTED_ROLE = "ROLE_APPROVER";
	

	public List<ExpenseAuditLogResponse> getLogs(Long expenseId) {
		
		var expense = auditLogMapper.findByExpenseId(expenseId);
		
		if(expense.size() == 0) {
			throw new NoSuchElementException("Not Found ExpenseId: " + expenseId);
		}
		
		boolean isOwner = Objects.equals(expense.get(0).getActorId(),CurrentUser.actorId()); 

	
		boolean isApprover = CurrentUser.actorRole().stream().filter(r -> Objects.equals(PERMITTED_ROLE, r)).toList().size() > 0;
		
		if(!isOwner && !isApprover) throw new BusinessException(
				"","本人以外ログの取得はできません");
		
		
		return ExpenseAuditLogResponse.toResponse(
				auditLogMapper.findByExpenseId(expenseId));
	}
	
	public List<ExpenseAuditLog> getOwnerLogs(Long actorId) {
		
		if(CurrentUser.is403(actorId)) throw new BusinessException("","本人以外ログの取得はできません");
		return auditLogMapper.findByActorId(actorId);
	}
	
}
