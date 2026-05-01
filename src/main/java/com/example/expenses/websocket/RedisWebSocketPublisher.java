package com.example.expenses.websocket;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.expenses.dto.NotificationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWebSocketPublisher {

	static final String CHANNEL = "ws-notifications";
	
	private final RedisTemplate<String, RedisNotificationMessage> redisTemplate;
	
	/** /topic/notifications へブロードキャスト */
	public void broadcast(NotificationMessage message) {
		publish("/topic/notifications", message);
	}
	
	/** /queue/{userId}/notifications への個人あて送信 */
	public void sendToUser(Long userId, NotificationMessage message) {
		publish("/queue/" + userId + "/notifications", message);
		
	}
	
	private void publish(String destination, NotificationMessage message) {
		var wrapper = new RedisNotificationMessage(destination, message);
		log.debug("Redis publish: destination={}, expenseId={}", destination, message.getExpenseId());
		redisTemplate.convertAndSend(CHANNEL, wrapper);
	}
}
