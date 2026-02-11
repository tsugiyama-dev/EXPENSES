package com.example.expenses.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.expenses.dto.request.UserRegisterRequest;
import com.example.expenses.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class UserRegisterViewController {

	private final UserService userService;
	
	@GetMapping
	public String form(@ModelAttribute("request") UserRegisterRequest req) {
		
		return "form/register";
	}
	
	@PostMapping("/form/submit")
	public String register(@ModelAttribute("request") @Valid UserRegisterRequest req,
			RedirectAttributes redirect) {
		
		userService.userRegister(req);
		redirect.addFlashAttribute("message", "ユーザー情報の登録が完了しました");
	    return "redirect:/login";
	}
}
