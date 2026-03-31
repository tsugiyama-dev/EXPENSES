package com.example.expenses.batch.tasklet;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;

import com.example.expenses.dto.batch.MonthlyExpenseReport;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EmailNotificationTasklet implements Tasklet {

	private static final Logger logger = LoggerFactory.getLogger(EmailNotificationTasklet.class);
	private final JavaMailSender mailSender;
	
	@Value("${app.mail.from}")
	private String fromEmail;
	@Value("${batch.notification.admin-email}")
	private String adminEmail;
	
	@Override
	public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		
		logger.info("メール通知タスクレット開始");
		
		// レポート情報を取得
		MonthlyExpenseReport report = (MonthlyExpenseReport) chunkContext.getStepContext()
		.getStepExecution().getJobExecution().getExecutionContext().get("monthylyReport");
		
		String reportFilePath = (String) chunkContext.getStepContext()
				.getStepExecution().getJobExecution().getExecutionContext().getString("reportFilePath");
		
		if(report == null || reportFilePath == null ) {
			throw new IllegalStateException("レポート情報が見つかりません");
		}
		
		
		
		return null;
	}

}
