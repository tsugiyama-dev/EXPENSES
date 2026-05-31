package com.example.expenses.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.domain.PendingNotification;
import com.example.expenses.repository.PendingNotificationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PendingNotificationService {

	private final PendingNotificationMapper mapper;

	@Transactional
	public void save(Long userId, String type, Long expenseId, String message) {
		mapper.insert(PendingNotification.create(userId, type, expenseId, message));
	}

	public List<PendingNotification> getUnread(Long userId) {
		return mapper.findUnreadByUserId(userId);
	}

	@Transactional
	public void markAllRead(Long userId) {
		mapper.markAllReadByUserId(userId);
	}
}
