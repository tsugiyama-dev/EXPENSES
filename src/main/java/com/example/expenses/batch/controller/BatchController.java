package com.example.expenses.batch.controller;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.expenses.repository.ExpenseMapper;

@RestController
@RequestMapping("/api/batch")

public class BatchController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Qualifier("csvImportJob")
	private final Job csvImportJob;
	@Qualifier("csvExportJob")
	private final Job csvExportJob;
	@Qualifier("pagingExportJob")
	private final Job pagingExportJob;

	private final ExpenseMapper expenseMapper;
	
	private final JobLauncher jobLauncher;
	

	public BatchController(Job csvImportJob, Job csvExportJob, Job pagingExportJob, JobLauncher jobLauncher, ExpenseMapper expenseMapper) {
		this.csvImportJob = csvImportJob;
		this.csvExportJob = csvExportJob;
		this.pagingExportJob = pagingExportJob;
		this.expenseMapper = expenseMapper;
		this.jobLauncher = jobLauncher;
	}


	@GetMapping("/execute")
	public ResponseEntity<Map<String, String>> executeBatchJob() {
		
		Long maxId = expenseMapper.findMaxId();
		String outputFile = "src/main/resources/csv/export/expenses_"
				+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")).toString()
				+ ".csv";
		
		JobParameters param = new JobParametersBuilder()
				.addLong("executionTime", System.currentTimeMillis())
				.addLong("maxId", maxId)
				.addString("outputFile", outputFile)
				.addLong("pageSize", 1000L)
				.toJobParameters();
		
			JobExecution jobExecution = null;
		
		try {
			jobExecution = jobLauncher.run(pagingExportJob, param);
		
			return ResponseEntity.accepted().body(Map.of(
					"status", jobExecution.getStatus().toString(),
					"jobId", String.valueOf(jobExecution.getId())
					));
		} catch (Exception e) {
			logger.error("バッチジョブの実行エラーが発生", e);
		
			return ResponseEntity.internalServerError().body(
					Map.of("status", jobExecution.getStatus().toString(),
							"message", e.getMessage()));
		}
		
	}
	
//	@GetMapping("/export")
//	public ResponseEntity<String> executeCsvExportJob() {
//		try {
//			
////			String outputFile = "src/main/resources/csv/export/expenses_"
//			String outputFile = "src/main/resources/csv/sample"
////					+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
//					+ ".csv";
//
//			JobParameters jobParameters = new JobParametersBuilder()
//					.addString("csvExportJob", "executionTime=" + System.currentTimeMillis() + ", outputFile=" + outputFile)
//					.toJobParameters();
//			
//			
//			JobExecution jobExecution = jobOperator.start(csvExportJob, jobParameters);
//			
//			return ResponseEntity.ok().body("CSV Export started: " + jobExecution.getId());
//		}catch(Exception e) {
//			logger.error("CSV Export failed", e);
//			return ResponseEntity.internalServerError().body("Export failed: " + e.getMessage());
//		}
//	}

	
}
