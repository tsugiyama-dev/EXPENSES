package com.example.expenses.batch.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.expenses.batch.decider.ImportCountDecider;
import com.example.expenses.batch.dto.ExpenseCsvRow;
import com.example.expenses.batch.processor.ExpenseCsvItemProcessor;
import com.example.expenses.batch.tasklet.ErrorNotificationTasklet;
import com.example.expenses.batch.tasklet.ReportTasklet;
import com.example.expenses.batch.tasklet.WarningTasklet;
import com.example.expenses.batch.writer.ExpenseCsvItemWriter;
import com.example.expenses.domain.Expense;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ConditionalFlowBatchConfiguration {

	private final FlatFileItemReader<ExpenseCsvRow> expenseCsvReader;
	private final ExpenseCsvItemProcessor expenseCsvProcessor;
	private final ExpenseCsvItemWriter expenseCsvWriter;
	
	@Bean
	Step importStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager) {
		return new StepBuilder("importStep", jobRepository)
				.<ExpenseCsvRow, Expense>chunk(100)
				.reader(expenseCsvReader)
				.processor(expenseCsvProcessor)
				.writer(expenseCsvWriter)
				.build();
	}
	
	@Bean
	JobExecutionDecider importCountDecider() {
		return new ImportCountDecider();
	}
	
	@Bean
	Step reportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("reportStep", jobRepository)
				.tasklet(new ReportTasklet(), transactionManager)
				.build();
	}
	
	@Bean
	Step warningStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("warningStep", jobRepository)
				.tasklet(new WarningTasklet(), transactionManager)
				.build();		
	}
	@Bean
	Step errorNotificationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		return new StepBuilder("errorNotificationStep", jobRepository)
				.tasklet(new ErrorNotificationTasklet(), transactionManager)
				.build();
	}
	
	@Bean
	Job conditionalFlowJob(JobRepository  jobRepository,
			Step importStep,
			JobExecutionDecider importCountDecider,
			Step reportStep,
			Step warningStep,
			Step errorNotificationStep) {
		
		return new JobBuilder("conditionalFlowJob", jobRepository)
				.start(importStep)   // 1. インポート実行
				.next(importCountDecider).on("HIGH").to(reportStep) //2. 100件以上 →レポート
				.from(importCountDecider).on("LOW").to(warningStep) //3. 1-99件 → 警告
				.from(importCountDecider).on("ZERO").to(errorNotificationStep) //4. 0件 → エラー
				.end()
				.build();
	}
			
}
