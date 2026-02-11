package com.example.expenses.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class UserRegisterRequest {
	private Long id;
	@NotBlank
	private String email;
	@NotBlank
	private String password;
	@NotBlank
	private String role;
}
