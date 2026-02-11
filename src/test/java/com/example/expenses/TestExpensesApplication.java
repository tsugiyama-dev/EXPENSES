package com.example.expenses;

import org.springframework.boot.SpringApplication;

public class TestExpensesApplication {

	public static void main(String[] args) {
		SpringApplication.from(ExpensesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
