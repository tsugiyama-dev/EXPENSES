package com.example.expenses.websocket;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/*
 * STOMP の「クライアント → サーバー」方向（Inbound チャネル）のフレームを
 * 横取りして、認証・認可をかけるインターセプター。
 *
 * 【なぜ必要か — HTTP の Spring Security だけでは足りない理由】
 *   SecurityConfig の authorizeHttpRequests は「HTTP リクエスト」にしか効かない。
 *   WebSocket は最初のハンドシェイク（HTTP GET /ws）こそ Security フィルタを通るが、
 *   接続が確立したあとにやり取りされる STOMP フレーム
 *   （CONNECT / SUBSCRIBE / SEND）は HTTP リクエストではないため、
 *   通常の Security 設定では認可の判断ができない。
 *   そこで Inbound チャネルに ChannelInterceptor を挟み、
 *   フレーム単位で「誰が」「どこを購読・送信しようとしているか」を検査する。
 *
 * 【preSend を使う理由】
 *   preSend はメッセージがチャネルに流れる「前」に呼ばれる。
 *   ここで例外を投げると、そのフレームはブローカーに到達せず拒否される
 *   （クライアントには STOMP ERROR フレームが返る）。
 *
 * 【Principal はどこから来るのか】
 *   ハンドシェイク時、ログイン済みなら HttpSession の Authentication が
 *   WebSocket セッションの Principal として自動で引き継がれる
 *   （Spring の DefaultHandshakeHandler の働き）。
 *   そのため CONNECT 以降のフレームでも accessor.getUser() で取り出せる。
 */
@Component
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private static final String ROLE_APPROVER = "ROLE_APPROVER";

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {

		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		StompCommand command = accessor.getCommand();

		// ハートビートなどコマンドを持たないフレームはそのまま通す
		if (command == null) {
			return message;
		}

		switch (command) {
		case CONNECT -> authenticateConnect(accessor);
		case SUBSCRIBE -> authorizeSubscribe(accessor);
		default -> {
			// SEND / DISCONNECT / UNSUBSCRIBE などは今回は素通し
			// （必要なら SEND にも同様の認可を足せる）
		}
		}

		return message;
	}

	/*
	 * CONNECT: 認証済みかどうかだけを確認する。
	 *   未ログイン（匿名）の場合 Principal は null になるので接続を拒否する。
	 *   ここで弾くことで「ログインしていないブラウザが WebSocket を開く」のを防ぐ。
	 */
	private void authenticateConnect(StompHeaderAccessor accessor) {
		Principal user = accessor.getUser();
		if (user == null) {
			log.warn("[WS] 未認証の CONNECT を拒否しました");
			throw new IllegalArgumentException("WebSocket 接続には認証が必要です");
		}
		log.debug("[WS] CONNECT 認証 OK: user={}", user.getName());
	}

	/*
	 * SUBSCRIBE: 購読先（destination）ごとに認可する。
	 *
	 *   - /user/**  : Spring が「Principal 名」を使って /user/{name}/... に解決するため、
	 *                 そもそも自分宛のキューしか届かない。よって追加チェック不要で素通し。
	 *
	 *   - /topic/** : このアプリでは「提出された経費」のブロードキャスト用。
	 *                 承認者が新規提出を把握するための通知なので、
	 *                 ROLE_APPROVER を持つユーザーだけに購読を許可する。
	 *                 （一般申請者は自分宛の承認・却下通知を /user/queue で受け取れば十分）
	 */
	private void authorizeSubscribe(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		if (destination == null) {
			return;
		}

		if (destination.startsWith("/topic/")) {
			Authentication auth = asAuthentication(accessor.getUser());

			boolean isApprover = auth != null && auth.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.anyMatch(ROLE_APPROVER::equals);

			if (!isApprover) {
				log.warn("[WS] 権限不足の SUBSCRIBE を拒否: user={}, destination={}",
						auth == null ? "anonymous" : auth.getName(), destination);
				throw new IllegalArgumentException("このトピックの購読には承認者権限が必要です");
			}
		}
		log.debug("[WS] SUBSCRIBE 認可 OK: destination={}", destination);
	}

	/*
	 * accessor.getUser() は Principal 型で返るが、Spring Security 経由なら
	 * 実体は Authentication（UsernamePasswordAuthenticationToken 等）。
	 * 権限（getAuthorities）を見たいので安全にキャストする。
	 */
	private Authentication asAuthentication(Principal principal) {
		return (principal instanceof Authentication auth) ? auth : null;
	}
}
