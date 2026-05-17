package com.example.expenses.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka トピックの定義。
 * アプリ起動時にトピックが存在しない場合は自動作成される。
 *
 * partitions=3: 3つのパーティションでメッセージを並列処理可能にする
 * replicas=1:   学習環境はシングルブローカーのため1
 */
@Configuration
public class KafkaConfig {

    @Bean
    NewTopic expenseEventsTopic() {
        return TopicBuilder.name(ExpenseTopics.EXPENSE_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
