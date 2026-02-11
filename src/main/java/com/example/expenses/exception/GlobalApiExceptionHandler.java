package com.example.expenses.exception;

import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.example.expenses.config.TraceIdFilter;
import com.example.expenses.exception.ApiErrorResponse.Detail;

@RestControllerAdvice(annotations = RestController.class)
public class GlobalApiExceptionHandler {

	private String traceId() {
		String tid = MDC.get(TraceIdFilter.TRACE_ID_KEY);
		return tid == null ? "" : tid;
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> commoneError(MethodArgumentNotValidException e) {
		
		var details = e.getBindingResult().getFieldErrors().stream().map(this::toDetail).toList();
		
		ApiErrorResponse response = new ApiErrorResponse(
				"VALIDATION_ERROR",
				"入力値が不正です",
			     details,
				"");
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	
	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiErrorResponse> commoneError(NoHandlerFoundException e) {
		
	
		var detail = new Detail(e.getRequestURL(),e.getMessage());
		ApiErrorResponse response = new ApiErrorResponse(
				"REQUREST_NOT_FOUND",
				"リクエストが見つかりません",
				List.of(detail),
				traceId());
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> commoneError(Exception e) {
		
		var details = new Detail("",e.getMessage());
		ApiErrorResponse response = new ApiErrorResponse(
				"INTERNAL_SERVER_ERROR",
				"サーバーエラーが発生しました",
				List.<Detail>of(details),
				"");
		
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
	
	
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException e) {
		ApiErrorResponse body = new ApiErrorResponse(
				e.getCode(),
				e.getMessage(),
				List.of(new ApiErrorResponse.Detail("", "")),
				traceId());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}
	
	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFond(NoSuchElementException e) {
		ApiErrorResponse body = new ApiErrorResponse(
				"NOT_FOUND",
				"対象データが見つかりません",
				List.of(new Detail("", e.getMessage())),
				traceId());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
				
	}

	private Detail toDetail(FieldError fe) {
		String reason = fe.getDefaultMessage();
		String field =  fe.getField();
		return new Detail(field, reason);
	}
}
