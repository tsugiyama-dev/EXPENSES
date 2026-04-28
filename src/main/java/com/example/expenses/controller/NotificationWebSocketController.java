package com.example.expenses.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.expenses.dto.NotificationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
 @RequiredArgsConstructor
 @Slf4j
public class NotificationWebSocketController {

	private final SimpMessagingTemplate messagingTemplate;
	
	
	@MessageMapping("/notify")
	@SendTo("/topic/notifications")
	public NotificationMessage sendNotification(NotificationMessage message) {
		log.info("Notification received* {}", message);
		return message;
	}
	
	public void sendNotificationToUser(Long userId, NotificationMessage message) {

		String destination = "/queue/" + userId + "/notifications";
		messagingTemplate.convertAndSendToUser(message.getApplicantName(), destination, message);
		log.info("Notification sent to user {}: {}", userId, message);;
		
	}
	public void broadcastNotification(NotificationMessage message) {
		messagingTemplate.convertAndSend("/topic/notifications", message);
		log.info("Notification broadcasted: {}", message);
		
	}
}
