package com.example.expenses.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.expenses.dto.NotificationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWebSocketPublisher {

	@Value("${app.redis.ws-channel}")
	private String channel;
	
	private final RedisTemplate<String, NotificationMessage> redisTemplate;
	
	/** /topic/notifications へブロードキャスト */
	public void broadcast(NotificationMessage message) {
		publish("/topic/notifications", message);
	}
	
	/** /queue/notifications への個人あて送信 */
	public void sendToUser(NotificationMessage message) {
		publish("/queue/notifications", message);
		
	}
	
	private void publish(String destination, NotificationMessage message) {
		message.setDestination(destination);
		log.debug("Redis publish: destination={}, expenseId={}", destination, message.getExpenseId());
		redisTemplate.convertAndSend(channel, message);
	}

		

}
