package com.example.expenses.batch.decider;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;


public class ImportCountDecider implements JobExecutionDecider {

	@Override
	public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
		
		long writeCount = stepExecution.getWriteCount();
		
		System.out.println("ImportCountDecider: writeCount = " + writeCount);
		
		if (writeCount == 0) {
			// 0 → エラー
			return new FlowExecutionStatus("ZERO");
		} else if (writeCount < 100) {
			// 1～99 →警告
			return new FlowExecutionStatus("LOW");
		} else {
			// 100以上 → 正常
			return new FlowExecutionStatus("HIGH");
		}
	}

}
