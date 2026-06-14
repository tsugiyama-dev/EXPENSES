package com.example.expenses.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.expenses.dto.NotificationMessage;

/*
 * Redis Pub/Sub の配線設定。
 *
 * 【以前との変更点】
 *   以前: RedisNotificationMessage（ラッパー）を Redis で転送していた
 *   今回: NotificationMessage に destination フィールドを追加したため
 *         ラッパークラスが不要になり、この設定もシンプルになった
 *
 * 【チャネル名の管理】
 *   以前: RedisWebSocketPublisher に static final String CHANNEL = "ws-notifications" が埋まっていた
 *   今回: application.properties の app.redis.ws-channel から取得する
 *         → 環境ごと（dev/prod）に変えたい場合や将来のリネームが設定だけで済む
 */
@Configuration
public class RedisConfig {

	@Value("${app.redis.ws-channel}")
	private String wsChannel;
	
	/**
	 * NotificationMessage を JSON でやり取りする RedisTemplate。
	 * ラッパークラスを廃止したので型パラメータが NotificationMessage に変わった。
	 */
	@Bean
	RedisTemplate<String, NotificationMessage> redisTemplate(
			RedisConnectionFactory connectionFactory) {
		
		var template = new RedisTemplate<String, NotificationMessage>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new JacksonJsonRedisSerializer<>(NotificationMessage.class));
		
		return template;
	}
	
	
	
	@Bean
	MessageListenerAdapter messageListenerAdapter(RedisWebSocketSubscriber subscriber) {
		var adapter = new MessageListenerAdapter(subscriber, "onMessage");
		adapter.setSerializer(new JacksonJsonRedisSerializer<>(NotificationMessage.class));
		return adapter;
	}
	
	@Bean
	RedisMessageListenerContainer redisMessageListenerContainer(
			RedisConnectionFactory connectionFactory,
			MessageListenerAdapter messageListenerAdapter) {
		
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(messageListenerAdapter, new ChannelTopic(wsChannel));
		
		return container;
	}
	
}
