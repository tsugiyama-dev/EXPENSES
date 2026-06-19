package com.example.expenses.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

@Configuration
@EnableKafka
public class KafkaConfig {

	@Bean
	ProducerFactory<String, ExpenseEventMessage> expenseEventProducerFactory(
			@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

		/*
		 * 【信頼性設定 — ここはコードで指定しないと効かない】
		 *   この ProducerFactory を Bean 定義した時点で Spring Boot の Kafka 自動設定は
		 *   バックオフする。つまり application.properties の spring.kafka.producer.* は
		 *   この Producer には反映されない。信頼性設定は必ずこの props マップに入れる。
		 *
		 * acks=all：
		 *   リーダーだけでなく、すべての同期レプリカ（ISR）が書き込みを確認するまで
		 *   送信成功としない。acks=1（既定）だとリーダー書き込み直後に成功扱いになり、
		 *   その直後にリーダーが落ちるとメッセージが失われる。経費の確定イベントを
		 *   落とさないため最も強い保証を選ぶ。
		 *
		 * enable.idempotence=true：
		 *   冪等プロデューサを有効化。プロデューサが内部リトライしても
		 *   ブローカー側で重複を排除し、「ちょうど1回」書き込む（送信経路の重複防止）。
		 *   有効化すると acks=all / retries>0 / max.in.flight<=5 が前提となるため、
		 *   矛盾しないよう明示的にそろえておく。
		 *   ※ これは「プロデューサ→ブローカー間」の重複防止であって、
		 *     コンシューマ側の重複処理（リトライ起因）は別途冪等化が必要（タスク4）。
		 *
		 * max.in.flight.requests.per.connection=5：
		 *   冪等プロデューサで順序保証を保てる上限。これを超えると順序が崩れうる。
		 *
		 * delivery.timeout.ms=120000：
		 *   送信開始から成功/失敗が確定するまでの上限（リトライ込み）。
		 *   この時間内はブローカー一時障害でも自動リトライし続ける。
		 */
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
		props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);

		return new DefaultKafkaProducerFactory<>(props);
	}

	@Bean
	KafkaTemplate<String, ExpenseEventMessage> kafkaTemplate(
			ProducerFactory<String, ExpenseEventMessage> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	ConsumerFactory<String, ExpenseEventMessage> expenseEventConsumerFactory(
			@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
			@Value("${spring.kafka.consumer.group-id:expenses-app}") String groupId) {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
		props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.example.expenses.kafka");
		props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, ExpenseEventMessage.class.getName());
		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	ConcurrentKafkaListenerContainerFactory<String, ExpenseEventMessage> kafkaListenerContainerFactory(
			ConsumerFactory<String, ExpenseEventMessage> consumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, ExpenseEventMessage> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		return factory;
	}
}
