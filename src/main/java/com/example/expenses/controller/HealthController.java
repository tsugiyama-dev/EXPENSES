package com.example.expenses.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

	@GetMapping
	public String health() {
		
		return "OK";
	}

	@GetMapping("/debug/validate")
	public String debug() {
		return "";
	}
}
