package com.example.expenses.batch.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Job csvImportJob;
	private final JobOperator jobOperator;
	
	@GetMapping("/execute")
	public ResponseEntity<String> executeBatchJob() {
		
		JobParameters jobParameters = new JobParametersBuilder()
				.addLong("executionTime", System.currentTimeMillis())
				.toJobParameters();
		
			JobExecution jobExecution = null;
		
		try {
			jobExecution = jobOperator.start(csvImportJob, jobParameters);
		} catch (Exception e) {
			logger.error("バッチジョブの実行エラーが発生", e);
		
		}
		
		return ResponseEntity.ok().body(jobExecution.getStatus().toString());
	}
	
}
