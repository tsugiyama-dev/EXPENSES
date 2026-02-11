package com.example.expenses.controller;


import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.expenses.config.LoginUser;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.domain.ExpensesSort;
import com.example.expenses.domain.Role;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.dto.request.RejectRequest;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.dto.response.PaginationResponse;
import com.example.expenses.service.ExpenseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/expenses")
@Slf4j
public class ExpenseViewController {

	private final ExpenseService expenseService;

	
	@ModelAttribute(name = "statuses")
	public ExpenseStatus[] status() {
		return ExpenseStatus.values();
	}
	
	
	@PostMapping("/approve/{expenseId}")
	public String approve(@PathVariable long expenseId,
			@RequestParam(required = false)int version,
			RedirectAttributes redirect,
			@AuthenticationPrincipal LoginUser user) {
		
		expenseService.approve(expenseId, version, user.getUserId());
		redirect.addFlashAttribute("message", "承認しました");
		
		return "redirect:/expenses/list";
		
	}
	
	@PostMapping("/reject/{expenseId}")
	public String reject(@PathVariable long expenseId,
			@ModelAttribute("searchRequest") @Valid ExpenseSearchCriteria criteria,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "5") int pageSize,
			@RequestParam int version,
			@ModelAttribute@Valid RejectRequest request,
			Model model,
			BindingResult result,
			RedirectAttributes redirect,
			@AuthenticationPrincipal LoginUser user) {
		
//		if(result.hasErrors()) {
//			model.addAttribute("rejRequest", request);
//			return "expenses/detail";
//		}
		
		PaginationResponse<ExpenseResponse> expenses = expenseService.search(criteria, page, pageSize,
				user.getUserId(),conversionType(user.getRoles()));
		
		expenseService.reject(expenseId, request.getReason(), version, user.getUserId());
		redirect.addAttribute("rejectRequest", new RejectRequest());
		redirect.addAttribute("criteria", criteria);
		redirect.addAttribute("expense", expenses);
		
		return "redirect:/expenses/list";
	}
	

	@GetMapping("/list")
	public String init(
			@ModelAttribute("searchRequest") ExpenseSearchCriteria criteria,
			@RequestParam(defaultValue="1") int page,
			@RequestParam(defaultValue="5") int pageSize,
			@AuthenticationPrincipal LoginUser user,
			Model model) {
		

		PaginationResponse<ExpenseResponse> expenses = expenseService.search(
				criteria,
				page,
				pageSize,
				user.getUserId(),
				conversionType(user.getRoles()));

		
		model.addAttribute("rejectRequest", new RejectRequest());
		model.addAttribute("criteria", criteria);
		model.addAttribute("expense", expenses);
		model.addAttribute("username", displayName(user.getUsername()));
		model.addAttribute("actorId", user.getUserId());
		model.addAttribute("roles", conversionType(user.getRoles()));
		return  "expenses/detail";
	}
	
	@PostMapping("/submit/{expenseId}")
	public String submit(
			@PathVariable long expenseId,
			@AuthenticationPrincipal LoginUser user) {
		expenseService.submit(expenseId);
		
		return "redirect:/expenses/list";
	}
	
	@ModelAttribute("sorts")
	public List<String> sort() {
		List<String> sortList = new ArrayList<>();
			
		for(var value : ExpensesSort.values()) {
			sortList.add(value.toString().toLowerCase() + "," + "ASC");
			sortList.add(value.toString().toLowerCase() + "," + "DESC");
		}
		return sortList;
	}
	
	@GetMapping("/search")
	public String search(
			@ModelAttribute("criteria") @Valid ExpenseSearchCriteria criteria,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "5") int pageSize,
			@AuthenticationPrincipal LoginUser user,
			Model model)
			{
		

		PaginationResponse<ExpenseResponse> result = expenseService.search(criteria, page, pageSize,
				user.getUserId(),conversionType(user.getRoles()));
		
		model.addAttribute("actorId", user.getUserId());
		model.addAttribute("expense", result);
		model.addAttribute("rejectRequest", new RejectRequest());
		model.addAttribute("username", displayName(user.getUsername()));
		model.addAttribute("roles", conversionType(user.getRoles()));
		return "expenses/detail";
		
	}
	
	@GetMapping("/csv")
	public ResponseEntity<byte[]> csv(
			@ModelAttribute ExpenseSearchCriteria criteria,
			@AuthenticationPrincipal LoginUser user) {
		
		byte[] csv = expenseService.getCsv(criteria);
		log.info("検索条件：{} ",criteria);
		HttpHeaders header   =  new HttpHeaders();
		
		header.setContentType(MediaType.parseMediaType("text/csv"));
		header.setContentDisposition(ContentDisposition.attachment().filename("Expenses.csv").build());
		header.setContentLength(csv.length);

		return ResponseEntity.ok().headers(header).body(csv);
	}
	
	private String displayName(String username) {
		return username.substring(0, username.indexOf("@"));
	}
	
	private List<String> conversionType(List<Role> roles) {
		return roles.stream().map(r-> r.getRole()).toList();
	}

}
