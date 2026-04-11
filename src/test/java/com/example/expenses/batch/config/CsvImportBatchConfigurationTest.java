package com.example.expenses.batch.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.utility.TestcontainersConfiguration;

import com.example.expenses.repository.ExpenseMapper;

@SpringBootTest
@SpringBatchTest
@DisplayName("Batch処理が正常に動作しているか")
@Import(TestcontainersConfiguration.class)
//@Sql(scripts = "/db/cleanup-expenses.sql", executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
//@Import(TestcontainersConfiguration.class)

class CsvImportBatchConfigurationTest {
	
	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;
	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;
	
	@Autowired
	private Job csvImportJob;
	@Autowired
	private ExpenseMapper expenseMapper;
	
	private JobParameters jobParameters;
	
	@BeforeEach
	void setUp() {
		jobLauncherTestUtils.setJob(csvImportJob);
		jobRepositoryTestUtils.removeJobExecutions();
		jobParameters = new JobParametersBuilder()
				.addLong("executionTime", System.currentTimeMillis())
				.toJobParameters();
	}
	
	@Test
	void CSVインポートジョブが正常に完了する() throws Exception{
		
		// When
		JobExecution jobExecution  = jobLauncherTestUtils.launchJob(jobParameters);
			
		//Then
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);//(jobExecution.getExitStatus())
		
	}
	
	@Test
	@DisplayName("CSVの10行中、3行がスキップされ7行がDBに保存される")
	void testCsvImportJob_SkipAndWrite() throws Exception {
		
		//Given
		JobParameters jobParameters =new JobParametersBuilder()
				.addLong("executionTime", System.currentTimeMillis())
				.toJobParameters();
		
		//When
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
		
		//Then
		//ジョブの実行結果を確認
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
		
		//Stepの統計情報を確認
		jobExecution.getStepExecutions().forEach(stepExecution -> {
			assertThat(stepExecution.getReadCount()).isEqualTo(10);
			assertThat(stepExecution.getWriteCount()).isEqualTo(7);
		    assertThat(stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount()).isEqualTo(3);
		});
	}
	@Test
	void testCsvImportJobSkipAndWrite() throws Exception {
		//Give
		JobParameters jobParameters = new JobParametersBuilder()
				.addLong("executionTime", System.currentTimeMillis())
				.toJobParameters();
		
		
		//When
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
		
		//Then
		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
		
		// Stepの統計情報ヲ確認
		jobExecution.getStepExecutions().forEach(stepExecution -> {
		assertThat(stepExecution.getReadCount()).isEqualTo(10);
		assertThat(stepExecution.getWriteCount()).isEqualTo(7);
		});
	}

}
