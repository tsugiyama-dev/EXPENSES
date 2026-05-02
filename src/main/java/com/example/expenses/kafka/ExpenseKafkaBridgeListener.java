package com.example.expenses.kafka;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.expenses.event.ExpenseApprovedEvent;
import com.example.expenses.event.ExpenseRejectedEvent;
import com.example.expenses.event.ExpenseSubmittedEvent;
import com.example.expenses.kafka.ExpenseEventMessage.EventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SpringEvent を Kafka トピックへ転送するブリッジ。
 *
 * 現時点では SpringEvent → Kafka → Consumer という二段構えになっているが、
 * 将来的には ExpenseService が直接 KafkaTemplate.send() を呼ぶ形に移行する。
 * このクラスはその移行前の学習用ステップ。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseKafkaBridgeListener {

    private final ExpenseKafkaProducer producer;

    @EventListener
    public void onSubmitted(ExpenseSubmittedEvent event) {
        var msg = new ExpenseEventMessage(EventType.SUBMITTED, event.getExpenseId(), event.getActorId(), null);
        log.debug("Bridge → Kafka: SUBMITTED expenseId={}", event.getExpenseId());
        producer.publish(msg);
    }

    @EventListener
    public void onApproved(ExpenseApprovedEvent event) {
        var msg = new ExpenseEventMessage(EventType.APPROVED, event.getExpenseId(), event.getApplicantId(), null);
        log.debug("Bridge → Kafka: APPROVED expenseId={}", event.getExpenseId());
        producer.publish(msg);
    }

    @EventListener
    public void onRejected(ExpenseRejectedEvent event) {
        var msg = new ExpenseEventMessage(EventType.REJECTED, event.getExpenseId(), event.getApplicantId(), event.getReason());
        log.debug("Bridge → Kafka: REJECTED expenseId={}", event.getExpenseId());
        producer.publish(msg);
    }
}
