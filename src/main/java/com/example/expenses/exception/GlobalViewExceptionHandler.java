package com.example.expenses.exception;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@ControllerAdvice(annotations = Controller.class)
public class GlobalViewExceptionHandler {

	@ExceptionHandler(exception = BusinessException.class)
	public String conflictHandler(BusinessException e, RedirectAttributes redirect) {
		
		redirect.addFlashAttribute("alert",
				e.getMessage() + "。または承認/否決されています。\n"
						+ "最新の状態を読み込みました");
		
		return "redirect:/expenses/list";
	}
	
	@ExceptionHandler(exception = MethodArgumentNotValidException.class)
	@ResponseBody
	public List<WebErrorResponse> validationError(BindingResult br,MethodArgumentNotValidException ex) {
		

		List<WebErrorResponse> errors = br.getFieldErrors().stream().map(
						e -> new WebErrorResponse(e.getField(), ex.getMessage())).toList();
		
		return errors;
	}
	
	@ExceptionHandler(exception = NullPointerException.class)
	public String loginError(
			NullPointerException e,
			Model model) {
		
		if(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
			model.addAttribute("errorMessage", "認証エラーが発生しました。再度ログインしてください");
			return "error/401";
		};
		model.addAttribute("errorMessage", e.getMessage());
		return "error/500";
	}
}
