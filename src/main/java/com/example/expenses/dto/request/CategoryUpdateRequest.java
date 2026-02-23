package com.example.expenses.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


/**
 * カテゴリ更新リクエストDTO
 */
public record CategoryUpdateRequest(
		@NotBlank(message = "カテゴリ名は必須です")
		@Size(max = 100, message = "カテゴリ名は100文字以内です")
		String name,
		
		@Size(max = 500, message = "説明は500文字以内です")
		String description,
		
		@Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "カラーコードは＃で始まる6桁の16進数で指定してください")
		String color,
		
		@Size(max = 50, message = "アイコン名は50文字以内です")
		String icon) {
}
