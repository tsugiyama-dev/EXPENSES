package com.example.expenses.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.example.expenses.websocket.StompAuthChannelInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				.setAllowedOriginPatterns("*")
				.withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {

		registry.setApplicationDestinationPrefixes("/app");
		registry.enableSimpleBroker("/topic", "/queue");

		// 2026-06-08 追記: ユーザ宛の送信先のプレフィックスを明示的に設定
		registry.setUserDestinationPrefix("/user");
	}

	/*
	 * 【テーマ8】クライアント → サーバー方向（Inbound）のチャネルに
	 * 認証・認可インターセプターを差し込む。
	 *
	 *   configureClientInboundChannel:
	 *     ブラウザから飛んでくる STOMP フレーム（CONNECT / SUBSCRIBE / SEND）が
	 *     通る「入口」のチャネル。ここに interceptor を登録すると、
	 *     全フレームが StompAuthChannelInterceptor.preSend() を経由する。
	 *
	 *   （サーバー → クライアント方向を制御したい場合は
	 *     configureClientOutboundChannel を使うが、今回は入口だけで十分）
	 */
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}
}
