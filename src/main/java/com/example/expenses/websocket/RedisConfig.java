package com.example.expenses.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

	
	/**
	 * RedisNotificationMessage をJSONでやり取りするRedisTemplate
	 * @param connectionFactory
	 * @return
	 */
	@Bean
	RedisTemplate<String, RedisNotificationMessage> redisTemplate(
			RedisConnectionFactory connectionFactory) {
		
		var template = new RedisTemplate<String, RedisNotificationMessage>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new JacksonJsonRedisSerializer<>(RedisNotificationMessage.class));
		
		return template;
	}
	
	
	
	@Bean
	MessageListenerAdapter messageListenerAdapter(RedisWebSocketSubscriber subscriber) {
		var adapter = new MessageListenerAdapter(subscriber, "onMessage");
		adapter.setSerializer(new JacksonJsonRedisSerializer<>(RedisNotificationMessage.class));
		return adapter;
	}
	
	@Bean
	RedisMessageListenerContainer redisMessageListenerContainer(
			RedisConnectionFactory connectionFactory,
			MessageListenerAdapter messageListenerAdapter) {
		
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(messageListenerAdapter,
				new ChannelTopic(RedisWebSocketPublisher.CHANNEL));
		
		return container;
	}
	
}
