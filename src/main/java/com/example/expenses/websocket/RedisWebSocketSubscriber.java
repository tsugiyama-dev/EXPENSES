package com.example.expenses.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.example.expenses.dto.NotificationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/*
 * Redis Pub/Sub の受信（Subscriber）→ WebSocket へ転送。
 *
 * 【以前との変更点】
 *   以前: 引数が RedisNotificationMessage（ラッパー）。
 *         message.getDestination() / message.getPayload() と2段階でアクセスしていた。
 *   今回: 引数が NotificationMessage に統一。
 *         message.getDestination() で宛先を取り出し、message 自体を WebSocket に流す。
 *
 * 【destination を null に戻す理由】
 *   destination は「サーバー内部のルーティング情報」であって、
 *   ブラウザ（クライアント）には不要。
 *   ラッパーを廃止して payload に destination を持たせた副作用として、
 *   そのまま送るとクライアントの JSON に destination が混ざってしまう。
 *   宛先をローカル変数に退避してから message.setDestination(null) することで、
 *   @JsonInclude(NON_NULL) と組み合わせてクライアントには含めない。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWebSocketSubscriber {

	private final SimpMessagingTemplate messagingTemplate;
	
	public void onMessage(NotificationMessage message) {
		
		String destination = message.getDestination();
		// クライアントにルーティング情報を送らないようにする
		message.setDestination(null);
		log.debug("Redis subscribe: destination= {}, expenseId= {}",
				  destination,
				  message.getExpenseId());
		
		if(message.getDestination().startsWith("/topic")) {
			
			// 全体向けブロードキャスト
			messagingTemplate.convertAndSend(destination, message);
		}else {
			//　個人あて(/queue/notifications) -> /user/{email}/queue/notifications へ転送
			messagingTemplate.convertAndSendToUser(
					message.getApplicantEmail(),
					message.getDestination(), 
					message);
			
		}
	}
	
}
