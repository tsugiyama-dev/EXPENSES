package com.example.expenses.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.expenses.config.LoginUser;
import com.example.expenses.domain.PendingNotification;
import com.example.expenses.service.PendingNotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class PendingNotificationController {

	private final PendingNotificationService service;

	/** ログイン中ユーザーの未読通知を返す */
	@GetMapping("/unread")
	public ResponseEntity<List<PendingNotification>> getUnread(
			@AuthenticationPrincipal LoginUser loginUser) {
		return ResponseEntity.ok(service.getUnread(loginUser.getUserId()));
	}

	/** 未読をすべて既読にする */
	@PutMapping("/read")
	public ResponseEntity<Void> markAllRead(
			@AuthenticationPrincipal LoginUser loginUser) {
		service.markAllRead(loginUser.getUserId());
		return ResponseEntity.noContent().build();
	}
}
