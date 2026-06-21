package com.example.expenses.kafka;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
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

@Configuration
@EnableKafka
public class KafkaConfig {

	@Bean
	ProducerFactory<String, ExpenseEventMessage> expenseEventProducerFactory(
			KafkaProperties kafkaProperties) {
		
		// application.propertiesから設定情報取得
		Map<String, Object> props = kafkaProperties.buildProducerProperties();

		return new DefaultKafkaProducerFactory<>(props);
	}

	@Bean
	KafkaTemplate<String, ExpenseEventMessage> kafkaTemplate(
			ProducerFactory<String, ExpenseEventMessage> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	ConsumerFactory<String, ExpenseEventMessage> expenseEventConsumerFactory(
			KafkaProperties kafkaProperties) {
		
		// application.properties から設定情報を取得
		Map<String, Object> props = kafkaProperties.buildConsumerProperties();
		// 以下は追加設定
		props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, ExpenseEventMessage.class.getName());
		props.put(ProducerConfig.ACKS_CONFIG, "all" ); // Brokerが複数の場合、すべてのBrokerが応答を返す必要がある。
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Brokerに保存されたけど応答中にエラーとなり再送されたときに重複して登録されないようにする
		props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // 返事を待たずに送ってよい件数
		props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
		
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
