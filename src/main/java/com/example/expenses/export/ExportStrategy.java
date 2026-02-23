package com.example.expenses.export;

import java.util.List;

import com.example.expenses.domain.Expense;

public interface ExportStrategy {
	
	byte[] export(List<Expense> expenses);
	
	String getContentType();
	
	String getFileExtension();
	
	default boolean isSupported() {
		return true;
	}
}
