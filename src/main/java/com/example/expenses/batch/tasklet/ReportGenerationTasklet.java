package com.example.expenses.batch.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.expenses.dto.batch.MonthlyExpenseReport;

@Component
public class ReportGenerationTasklet implements Tasklet {

	private static final Logger logger = LoggerFactory.getLogger(ReportGenerationTasklet.class);
	
	@Value("${batch.report.output-dir}")
	private String outputDir;
	
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		
		logger.info("レポート生成タスクレット開始");
		
		MonthlyExpenseReport report = (MonthlyExpenseReport) chunkContext.getStepContext()
				.getStepExecution()
				.getJobExecution()
				.getExecutionContext()
				.get("monthlyReport");
		
		if(report == null) {
			throw new IllegalStateException(" 月次レポートが見つかりません");
		}
		
		return null;
	}

}
