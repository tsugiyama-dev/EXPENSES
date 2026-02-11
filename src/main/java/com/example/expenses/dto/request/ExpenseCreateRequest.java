package com.example.expenses.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExpenseCreateRequest(
		@NotBlank(message = "titleは必須です") String title,
		@NotNull @Positive(message = "金額は１以上で入力してください") BigDecimal amount,
		@NotBlank String currency) {

}
