package com.example.expenses.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.example.expenses.dto.NotificationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * WebSocket クライアントからのメッセージを受け取るコントローラー。
 *
 * 【役割の整理】
 *   サーバー → クライアントへの通知 : RedisWebSocketPublisher → RedisWebSocketSubscriber が担う
 *   クライアント → サーバーへのメッセージ : このクラスが @MessageMapping で受け取る
 *
 * 【削除したメソッド】
 *   sendNotificationToUser() / broadcastNotification() は
 *   RedisWebSocketPublisher に役割を統一したため未使用だった。
 *   残しておくと「通知の送信経路が2つあるのか」と誤解を招くため削除。
 *   （SimpMessagingTemplate の依存もこのクラスから不要になった）
 */

@Controller
 @RequiredArgsConstructor
 @Slf4j
public class NotificationWebSocketController {
	
	@MessageMapping("/notify")
	@SendTo("/topic/notifications")
	public NotificationMessage sendNotification(NotificationMessage message) {
		log.info("WebSocket message received: {}", message);
		return message;
	}
}
