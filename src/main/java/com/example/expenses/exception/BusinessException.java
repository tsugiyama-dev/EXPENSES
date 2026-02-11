package com.example.expenses.exception;


public class BusinessException extends RuntimeException {

	private final String code;
	private String traceId;
	
	public BusinessException(String code, String message) {
		super(message);
		this.code = code;
	}
	
	public BusinessException(String code, String message, String traceId) {
		super(message);
		this.code = code;
		this.traceId = traceId;
	}
	
	public String getCode() {return code;}
	public String getTraceId() {return traceId;}
}
	
