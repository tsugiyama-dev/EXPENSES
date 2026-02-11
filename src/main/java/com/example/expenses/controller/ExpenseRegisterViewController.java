package com.example.expenses.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.expenses.dto.request.ExpenseCreateRequest;
import com.example.expenses.service.ExpenseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/expenses/register/expense")
public class ExpenseRegisterViewController {

	private final ExpenseService expenseService;
	
	@GetMapping
	public String form(@ModelAttribute("request") ExpenseCreateRequest req,
			@RequestParam("actorId")Long id,
			Model model) {
		
		model.addAttribute("actorId", id);
		return 
			"expenses/form/register/expense";
	}
	
	@PostMapping
	public String register(@ModelAttribute("request")@Valid ExpenseCreateRequest req,
			@RequestParam Long actorId,
			RedirectAttributes redirectAttribute) {
	
		expenseService.create(req);
		
		redirectAttribute.addFlashAttribute("message", "経費の登録が完了しました");
		return "redirect:/expenses/list";
	}
	
}
