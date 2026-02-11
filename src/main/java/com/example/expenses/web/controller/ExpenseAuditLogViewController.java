package com.example.expenses.web.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.expenses.config.LoginUser;
import com.example.expenses.dto.ExpenseAuditLog;
import com.example.expenses.service.ExpenseAuditLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/expenses/log")
public class ExpenseAuditLogViewController {

	private final ExpenseAuditLogService expenseAuditLogService;
	
	@GetMapping("/list")
	public String show(Model model,
			@AuthenticationPrincipal LoginUser user) {
		
		if(user.getUserId() == null) {
			
		}
		List<ExpenseAuditLog> logs = expenseAuditLogService.getOwnerLogs(user.getUserId());
		
		model.addAttribute("expenseLogs",logs);
		return "expenses/ExpenseAuditLog";
	}
}
