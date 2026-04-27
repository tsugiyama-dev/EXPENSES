package com.example.expenses.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis チャンネルから通知を受け取り、自インスタンスの
 * WebSocket クライアントへ転送する。
 *
 * 全サーバーインスタンスがこのクラスを持つので、
 * どのインスタンスにユーザーが接続していても通知が届く。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWebSocketSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis からメッセージを受信したとき呼ばれる。
     * MessageListenerAdapter の defaultListenerMethod に対応。
     */
    public void onMessage(RedisNotificationMessage message) {
        log.debug("Redis subscribe: destination={}, expenseId={}",
                message.getDestination(), message.getPayload().getExpenseId());
        messagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
    }
}
