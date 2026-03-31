package com.example.expenses.batch.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.expenses.batch.tasklet.DataAggregationTasklet;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableScheduling
public class BatchConfiguration {

	private final DataAggregationTasklet dataAggregationTasklet;
	private final ReportGenerationTasklet reportGenerationTasklet;
	private final EmailnotificationTasklet emailnotificationTasklet;
	
	@Bean
	public Job monthlyExpenseReportJob(JobRepository jobRepository,
			Step dataAggregationStep,
			Step reportGenerationStep,
			Step emailNotificationStep) {
		
		return new JobBuilder("monthlyExpenseReportJob", jobRepository)
				.start(dataAggregationStep)
				.next(reportGenerationStep)
				.next(emailNotificationStep)
				.build();
	}

    @Bean
    Step dataAggregationStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
		return new StepBuilder("dataAggregationStep", jobRepository)
				.tasklet(dataAggregationTasklet, transactionManager)
				.build();
	}
	
	@Bean
	Step reportGenerationStep(JobRepository jobRepository,
			PlatformTransactionManager transactionManager) {
		return new StepBuilder("reportGenerationStep", jobRepository)
				.tasklet(reportGenerationTasklet, transactionManager)
				.build();
		
	}
	
	@Bean
	Step emailNotificationStep(JobRepository jobRepository,
			PlatformTransactionManager transactionManager) {
		return new StepBuilder("emailNotificationStep", jobRepository)
				.tasklet(emailNotificationTasklet, transactionManager)
				.build();
	}
}
