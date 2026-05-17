package com.example.expenses.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMetrics {

    private final Counter createdCounter;
    private final Counter submittedCounter;
    private final Counter approvedCounter;
    private final Counter rejectedCounter;

    public ExpenseMetrics(MeterRegistry registry) {
        this.createdCounter   = Counter.builder("expense.created.total")
                .description("経費申請の作成件数")
                .register(registry);
        this.submittedCounter = Counter.builder("expense.submitted.total")
                .description("経費申請の提出件数")
                .register(registry);
        this.approvedCounter  = Counter.builder("expense.approved.total")
                .description("経費申請の承認件数")
                .register(registry);
        this.rejectedCounter  = Counter.builder("expense.rejected.total")
                .description("経費申請の却下件数")
                .register(registry);
    }

    public void incrementCreated()   { createdCounter.increment(); }
    public void incrementSubmitted() { submittedCounter.increment(); }
    public void incrementApproved()  { approvedCounter.increment(); }
    public void incrementRejected()  { rejectedCounter.increment(); }
}
