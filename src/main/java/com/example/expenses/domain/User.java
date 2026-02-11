package com.example.expenses.domain;

import java.util.List;

import lombok.Data;
@Data
public class User {
	

		private Long id;
		private String email;
		private String password;
		private List<Role> roles;
}
