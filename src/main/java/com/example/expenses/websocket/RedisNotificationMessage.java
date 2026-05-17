package com.example.expenses.websocket;

import com.example.expenses.dto.NotificationMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis チャンネルを経由する通知メッセージのラッパー。
 * destination に送り先（/topic/… または /queue/…）を持たせることで
 * Subscriber 側がそのまま SimpMessagingTemplate に転送できる。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisNotificationMessage {

    /** WebSocket の送信先（例: /topic/notifications, /queue/1/notifications） */
    private String destination;

    /** 実際に WebSocket クライアントへ届けるペイロード */
    private NotificationMessage payload;
}
