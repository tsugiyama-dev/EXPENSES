package com.example.expenses.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.expenses.config.LoginUser;
import com.example.expenses.dto.request.ExpenseCreateRequest;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.dto.request.RejectRequest;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.dto.response.PaginationResponse;
import com.example.expenses.service.ExpenseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

	private final ExpenseService expenseService;
	
	@PostMapping
	public ResponseEntity<ExpenseResponse> create(
			@Valid @RequestBody ExpenseCreateRequest request) {
		ExpenseResponse res = expenseService.create(request);
		
		URI location = URI.create("/expenses/" + res.id());
		return ResponseEntity.created(location).body(res);
	}
	
	@PostMapping("/{id}/submit")
	public ResponseEntity<ExpenseResponse> submit(@PathVariable Long id) {
		return ResponseEntity.ok(expenseService.submit(id));
	}
	
	@GetMapping
	public ResponseEntity<PaginationResponse<ExpenseResponse>> search(
			@RequestParam(required = false)Long applicantId, 
			@RequestParam(required = false)String status, 
			@RequestParam(required = false)String title, 
			@RequestParam(required = false)BigDecimal amountMin, 
			@RequestParam(required = false)BigDecimal amountMax,
			@RequestParam(required = false)LocalDate submittedFrom,
			@RequestParam(required = false)LocalDate submittedTo,
			@RequestParam(required = false)String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		
		ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
				applicantId,
				status,
				title,
				sort,
				amountMin,
				amountMax,
				submittedFrom,
				submittedTo);
		var user = (LoginUser)SecurityContextHolder.getContext().getAuthentication();
		return ResponseEntity.ok().body(
				expenseService.search(criteria, page, size, user.getUserId(),
				user.getRoles().stream().map(r -> r.getRole()).toList()));
	}
	
	@PostMapping("/{expenseId}/approve")
	public ResponseEntity<ExpenseResponse> approve(
			@PathVariable Long expenseId,
			@RequestParam int version) {
		return ResponseEntity.ok().body(expenseService.approve(expenseId, version));
		
	}
	
	@PostMapping("/{id}/reject")
	public ResponseEntity<ExpenseResponse> reject(
			@PathVariable Long id,
			@RequestParam int version,
			@RequestBody @Valid RejectRequest req) {
		return ResponseEntity.ok().body(expenseService.reject(id, req.getReason(), version));
	}
}
