package com.example.expenses.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWebSocketSubscriber {

	private final SimpMessagingTemplate messagingTemplate;
	
	public void onMessage(RedisNotificationMessage message) {
		log.debug("Redis subscribe: destination= {}, expenseId= {}",
				  message.getDestination(),
				  message.getPayload().getExpenseId());
		messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
	}
	
}
