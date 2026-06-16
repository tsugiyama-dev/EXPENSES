package com.example.expenses.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

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
	 *   - TTL を設けないと承認・却下後も古いデータが永遠に残る可能性がある
	 *
	 * 【シリアライザの選定 — ここが一番ハマるポイント】
	 *   Spring Boot 4 は Jackson 3（tools.jackson パッケージ）に移行した。
	 *   旧 GenericJackson2JsonRedisSerializer は deprecated（将来削除）なので
	 *   Jackson 3 版の GenericJacksonJsonRedisSerializer を使う。
	 *
	 *   さらに 2 つの設定が必須（どちらも欠けると実行時に失敗する）：
	 *
	 *   1) enableDefaultTyping(...)
	 *      JSON に型情報 "@class" を埋め込む。
	 *      これが無いと逆シリアライズ時に Expense ではなく LinkedHashMap になり、
	 *      @Cacheable の戻り値で ClassCastException が発生する。
	 *      ただし無制限の型を許すと攻撃の足がかりになるため、
	 *      PolymorphicTypeValidator で「自分のパッケージ」と「java.*」だけに限定する。
	 *
	 *   2) enableSpringCacheNullValueSupport()
	 *      Spring Cache は「DBに無かった」結果も NullValue として保存する。
	 *      この設定でその NullValue を正しくシリアライズできる。
	 *
	 *   なお Jackson 3 は LocalDateTime をデフォルトで扱える。
	 *   （Jackson 2 では JavaTimeModule を登録しないと
	 *    "Java 8 date/time type not supported by default" で落ちた）
	 */
	@Bean
	RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

		var typeValidator = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType("com.example.expenses.")
				.allowIfSubType("java.")
				.build();

		var serializer = GenericJacksonJsonRedisSerializer.builder()
				.enableDefaultTyping(typeValidator)
				.enableSpringCacheNullValueSupport()
				.build();

		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.serializeKeysWith(
						RedisSerializationContext.SerializationPair.fromSerializer(
								new StringRedisSerializer()))
				.serializeValuesWith(
						RedisSerializationContext.SerializationPair.fromSerializer(serializer));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(config)
				/*
				 * transactionAware():
				 *   @CacheEvict は通常メソッド本体の直後に実行されるが、@Transactional の
				 *   コミットは「メソッドを抜けたあと」。この間に別スレッドが getExpense() を
				 *   呼ぶと、まだコミットされていない＝古い行を読んでキャッシュし直す恐れがある。
				 *   transactionAware() を付けると、キャッシュ削除はトランザクションの
				 *   コミット完了後まで遅延される（ロールバック時は削除しない）。
				 */
				.transactionAware()
				.build();
	}
}
