package com.example.expenses.domain;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Role {

	private Long id;
	private Long userId;
	private String role;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
