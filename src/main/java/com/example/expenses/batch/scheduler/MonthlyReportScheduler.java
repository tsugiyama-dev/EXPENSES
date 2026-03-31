package com.example.expenses.batch.scheduler;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

	private final TaskExecutorJobOperator jobOperator;
	private final Job monthlyExpenseReportJob;
	private final Logger logger = LoggerFactory.getLogger(MonthlyReportScheduler.class);
	
	public void startMonthlyReportJob()  {
		
		JobParameters jobParameters = new JobParametersBuilder()
				.addLocalDateTime("executionTime", LocalDateTime.now())
				.toJobParameters();
		try {
			JobExecution jobExecution = jobOperator.start(monthlyExpenseReportJob, jobParameters);
			
	
			 // ジョブの実行結果をログに出力
	        logger.info("月次経費レポートジョブのスケジュール実行完了: status={}, exitCode={}",
	              jobExecution.getStatus(),
	              jobExecution.getExitStatus().getExitCode());
	        
		} catch (Exception e) {
			logger.error("月次経費レポートジョブのスケジュール実行エラーが発生", e);
			
		}
	
		
		
	}
}
