package com.example.expenses.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.example.expenses.domain.Expense;
import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.dto.response.PagedResponse;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.util.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseViewService {

	private final ExpenseMapper expenseMapper;
	private final ExpenseAuditLogMapper expenseLogMapper;
	

	public Expense getDetails(Long expenseId) {

		Expense expense = expenseMapper.findById(expenseId);
		if(expense == null) {
			throw new NoSuchElementException("Not Found expenseId：" + expenseId);
		}

		if(CurrentUser.is403(expense.getApplicantId())) {
			throw new BusinessException("403 Forbidden","本人以外取得できません");
		}

		return expense;
	}
	
	public List<ExpenseAuditLog> getLogs(Long expenseId) {

		Expense ex = expenseMapper.findById(expenseId);
				
		if(CurrentUser.is403(ex.getApplicantId())) {
			throw new BusinessException("403 Forbidden","本人以外取得できません");
		}
		return expenseLogMapper.findByExpenseId(expenseId);
	}
	
	
	public PagedResponse<ExpenseResponse> getOwnerExpenses(
			long actorId, 
			long loginUserId,
			List<String> actorRoles,
		    int page, int pageSize)  { 
	
		
		boolean isApprover = actorRoles.stream().filter(
				r -> r.contains("ROLE_APPROVER")).
				toList().size() > 0;
		
		if(!isApprover && actorId != loginUserId) { 
			throw new BusinessException("403 Forbidden", "本人以外取得できません");
		}
		var criteria = new ExpenseSearchCriteria(
				loginUserId, null, null, null, null, null, null, null);

		var entity = ExpenseSearchCriteria.toEntity(criteria);
		
		int cnt = (int)expenseMapper.count(entity);
		
		int totalPage = (int)Math.ceil(cnt / pageSize);
		
		int offset = page * pageSize;
		
		List<Expense> result = expenseMapper.search(entity, "created_at", "DESC", pageSize, offset);

		List<ExpenseResponse> res = ExpenseResponse.toListResponse(result);
		PagedResponse<ExpenseResponse> paged = 
				new PagedResponse<>(res, page, pageSize, cnt , totalPage);
		return paged;
	}
	
}
