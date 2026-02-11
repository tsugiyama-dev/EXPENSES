package com.example.expenses.exception;

import java.util.List;

public record ApiErrorResponse(String code, String message, List<Detail> details, String traceId) {
	
	public record Detail(String field, String message) {}

}
