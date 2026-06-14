package com.example.expenses.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationMessage {

	public enum NotificationType {
		EXPENSE_SUBMITTED,
		EXPENSE_APPROVED,
		EXPENSE_REJECTED
	}

	private NotificationType type;
	private Long expenseId;
	private String applicantEmail;
	private String approverName;
	private String title;
	private String amount;
	private String message;
	private LocalDateTime timestamp;
	
	/*
	 * Redis Pub/Sub 経由でメッセージを転送する際の WebSocket 送信先。
	 * "/topic/notifications"（ブロードキャスト）か
	 * "/queue/notifications"（個人あて）が入る。
	 *
	 * 以前は RedisNotificationMessage というラッパークラスで
	 * payload（通知内容）と destination（宛先）を別々に持っていたが、
	 * このフィールドを追加することでラッパーが不要になり、クラス数が減る。
	 *
	 * @JsonInclude(NON_NULL) を付けているので、最終的に WebSocket で
	 * クライアントへ送る直前に Subscriber 側で null に戻せば、
	 * 内部ルーティング情報をクライアントに渡さずに済む。
	 */
	private String destination;
}
