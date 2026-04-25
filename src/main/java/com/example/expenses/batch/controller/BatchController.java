package com.example.expenses.batch.controller;


import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.expenses.repository.ExpenseMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Qualifier("csvImportJob")
	private final Job csvImportJob;
	@Qualifier("csvExportJob")
	private final Job csvExportJob;
	@Qualifier("pagingExportJob")
	private final Job pagingExportJob;
	@Qualifier("parallelExportJob")
	private final Job parallelExportJob;
	@Qualifier("conditionalFlowJob")
	private final Job conditionalFlowJob;

	@Value("${file.input-dir}")
	private String inputdir;
	
	private final ExpenseMapper expenseMapper;
	
	private final JobOperator jobOperator;

//	public BatchController(Job csvImportJob, Job csvExportJob, Job pagingExportJob, JobLauncher jobLauncher, ExpenseMapper expenseMapper, Job parallelExportJob, JobOperator jobOperator) {
//		this.csvImportJob = csvImportJob;
//		this.csvExportJob = csvExportJob;
//		this.pagingExportJob = pagingExportJob;
//		this.parallelExportJob = parallelExportJob;
//		this.expenseMapper = expenseMapper;
//		this.jobLauncher = jobLauncher;
//		this.jobOperator = jobOperator;
//	}


	@GetMapping("/export")
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
			jobExecution = jobOperator.start(pagingExportJob, param);
		
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
	
	@GetMapping("/export-parallel")
	public ResponseEntity<String> executeparallelExportJob() {
		try {
			String outpuDir = "src/main/resources/csv/export/parallel";
			
			JobParameters jobParameters = new JobParametersBuilder()
					.addLong("executionTime",  System.currentTimeMillis())
					.addString("outputDir", outpuDir)
					.toJobParameters();
			JobExecution jobExecution = jobOperator.start(parallelExportJob, jobParameters);
			
			return ResponseEntity.ok().body("Parallel Export started: " + jobExecution.getId());
			}catch (Exception e) {
				logger.error("Paralel Export failed", e);
				return ResponseEntity.internalServerError().body("Export failed: " + e.getMessage());
				
			}
	}
	
	@GetMapping("/conditional-flow")
	public ResponseEntity<String> executteConditionalFlowJob() {
		try {
			String inputDir = Path.of(inputdir).toAbsolutePath().normalize().toString();
			
			JobParameters jobParameters = new JobParametersBuilder()
					.addLong("executionTime", System.currentTimeMillis())
					.addString("inputDir", inputDir)
					.toJobParameters();
			
			
			System.out.println("入力ディレクトリのパス: " + Path.of(inputDir).toAbsolutePath().normalize());
			JobExecution jobExecution = jobOperator.start(conditionalFlowJob, jobParameters);
			
			return ResponseEntity.ok().body("Conditional Flow Job started: " + jobExecution.getId());
		}catch(Exception e) {
			
			logger.error("Conditional Flow Job failed", e);
			return ResponseEntity.internalServerError().body("Job failed: " + e.getMessage());
			
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
