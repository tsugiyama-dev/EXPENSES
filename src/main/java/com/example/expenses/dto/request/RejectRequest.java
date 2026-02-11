package com.example.expenses.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RejectRequest {
	
		
		@NotBlank(message = "却下理由は必須です")
		@Size(max = 100, message = "却下理由は100文字以内です")
		String reason ;

}
