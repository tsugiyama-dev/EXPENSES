package com.example.expenses.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class RejRequest {

	@NotBlank
	private String reason;
}
