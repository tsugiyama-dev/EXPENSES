package com.example.expenses.config;


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

@Component
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private static final String ROLE_APPROVER = "ROLE_APPROVER";
	
	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		
		StompHeaderAccessor accessor =  StompHeaderAccessor.wrap(message);
		StompCommand command = accessor.getCommand();
		
		if (command == null) {
			return message;
		}
		
		switch(command) {
		case CONNECT -> authenticateConnect(accessor);
		case SUBSCRIBE -> authorizeSubscribe(accessor);
		default -> {
			
		}
		}
		return message;
	}
	
	/*
	 * CONNECT: 認証済かどうかだけを確認する
	 *   未ログイン（匿名）の場合 Principal は null になるので接続を拒否する。
	 *   ここで弾くことで「ログインしていないブラウザがWebSocket を開く」のを防ぐ
	 */
	
	private void authenticateConnect(StompHeaderAccessor accessor) {
		
		Principal user = accessor.getUser();
		if(user == null) {
			log.warn("[WS] 未認証のCONNECT を拒否しました");
			throw new IllegalArgumentException("WebSocket 　接続には認証が必要です");
		}
		log.debug("[WS] CONNECT 認証 OK: user={}", user.getName());
	}
	
	/*
	 * 
	 * SUBSCRIBE: 購読先（destination)ごとに認可する
	 * 
	 *  - /user/** : spring が 「Principal 名」を使って/user/{name}/...に解決するため、
	 *  そもそも自分あてのキューしか届かない。よって追加チェック不要で素通し。
	 *  
	 *  -/topic/** : このアプリでは「提出された経費」のブロードキャスト用。
	 *               承認者が新規提出を把握するための通知なので、
	 *               ROLE_APPROVER を持つユーザーだけに購読を許可する。
	 *               (一般申請者は自分あての承認・却下通知を/user/queue で受け取れば十分）
	 */
	private void authorizeSubscribe(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		if(destination == null) {
			return;
		}
		
		if (destination.startsWith("/topic/")) {
			Authentication auth = asAuthentication(accessor.getUser());
			
			boolean isApprover = auth != null && auth.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.anyMatch(ROLE_APPROVER::equals);
			
			if(!isApprover) {
				log.warn("[WS] 権限不足の SUBSCRIBE を拒否： USER={}、destination={}",
						auth == null ? "anonymous" : auth.getName(), destination);
				throw new IllegalArgumentException("このトピックの購読には承認者権限が必要です");
			}
			log.debug("[WS] SUBSCRIBE 認可 OK: destination={}", destination);
		}
	}

	
	/*
	 * accessor.getUser() はPrincipal 型で返るが、Spring Security 経由なら
	 * 実態はAuthentication （UsernamePasswordAuthenticationToken 等）。
	 * 権限(getAuthorities)を見たいので安全にキャストする。
	 * 
	 */
	private Authentication asAuthentication(Principal principal) {
		return (principal instanceof Authentication auth) ? auth : null;
	}
}
