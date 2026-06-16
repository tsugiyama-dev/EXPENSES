package com.example.expenses.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/*
 * Redis をキャッシュとして使う設定。
 *
 * 【なぜ RedisConfig（Pub/Sub 用）と分けるか】
 *   RedisConfig の RedisTemplate は「NotificationMessage を Pub/Sub チャネルに流す」専用。
 *   キャッシュは RedisCacheManager が管理し、内部で別の接続ロジックを持つ。
 *   同じ RedisConnectionFactory を共有しつつ、用途ごとに Bean を分けることで
 *   「Pub/Sub の設定変更がキャッシュに影響する」「キャッシュの TTL 設定が Pub/Sub を壊す」
 *   といった意図しない干渉を防げる。
 *
 * 【@EnableCaching】
 *   これがないと @Cacheable / @CacheEvict アノテーションが完全に無視される。
 *   Spring の AOP プロキシがキャッシュ処理を差し込むのに必要。
 */
@Configuration
@EnableCaching
public class CacheConfig {

	/*
	 * RedisCacheManager：@Cacheable / @CacheEvict の実際の読み書きを担う。
	 *
	 * TTL を 10 分に設定：
	 *   - 10 分後にキャッシュが自動で消える（Redis の EXPIRE コマンドで管理）
	 *   - アプリを再起動してもキャッシュが残る（Redis はインメモリ DB なので）
	 *   - TTL を設けないと承認・却下後も古いデータが永遠に返る可能性がある
	 *
	 * シリアライザに GenericJackson2JsonRedisSerializer を使う理由：
	 *   JacksonJsonRedisSerializer<Expense> にすると型が固定されるが、
	 *   GenericJackson2JsonRedisSerializer は JSON に型情報(@class)を埋め込むので
	 *   キャッシュに複数の型が混在しても逆シリアライズできる。
	 */
	@Bean
	RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.serializeKeysWith(
						RedisSerializationContext.SerializationPair.fromSerializer(
								new StringRedisSerializer()))
				.serializeValuesWith(
						RedisSerializationContext.SerializationPair.fromSerializer(
								new GenericJackson2JsonRedisSerializer()));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(config)
				.build();
	}
}
