package com.example.expenses.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * RedisNotificationMessage を JSON でやり取りする RedisTemplate。
     * デフォルトの JdkSerializationRedisSerializer だと可読性が低いため
     * Jackson2JsonRedisSerializer を使用する。
     */
    @Bean
    RedisTemplate<String, RedisNotificationMessage> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        var template = new RedisTemplate<String, RedisNotificationMessage>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(RedisNotificationMessage.class));
        return template;
    }

    /**
     * Redis から受信したメッセージを RedisWebSocketSubscriber.onMessage() に渡すアダプター。
     */
    @Bean
    MessageListenerAdapter messageListenerAdapter(RedisWebSocketSubscriber subscriber) {
        var adapter = new MessageListenerAdapter(subscriber, "onMessage");
        adapter.setSerializer(new Jackson2JsonRedisSerializer<>(RedisNotificationMessage.class));
        return adapter;
    }

    /**
     * Redis チャンネル "ws-notifications" を購読するリスナーコンテナ。
     * アプリ起動時に自動的に subscribe を開始する。
     */
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
