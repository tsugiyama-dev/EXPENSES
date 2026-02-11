package com.example.expenses.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.expenses.dto.request.UserRegisterRequest;
import com.example.expenses.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class UserRegisterController {

	private final UserService userService;
	@PostMapping
	public ResponseEntity<String> register(
			@Valid @RequestBody UserRegisterRequest req) {
	
		userService.userRegister(req);
		
		return ResponseEntity.ok().body("登録完了");
	}
}
