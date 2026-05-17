package com.example.expenses.websocket;

import com.example.expenses.dto.NotificationMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisNotificationMessage {

	// WebSocket送信先 /topic/notification, /queue/1/notification 
	private String destination;
	
	private NotificationMessage payload;
}
