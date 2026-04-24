package com.example.expenses.batch.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.expenses.batch.partitioner.RangePartitioner;
import com.example.expenses.domain.Expense;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ParallelBatchConfiguration {

	private final SqlSessionFactory sqlSessionFactory;
	
	@Bean
	TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(4);
		taskExecutor.setMaxPoolSize(4);;
		taskExecutor.setQueueCapacity(10);
		taskExecutor.setThreadNamePrefix("batch-");
		taskExecutor.initialize();
		
		return taskExecutor;
	}
	
	@Bean
	@StepScope
	MyBatisCursorItemReader<Expense> parallelReader(
			@Value("#{stepExecutionContext['minId']}") Long minId,
			@Value("#{stepExecutionContext['maxId']}") Long maxId) {
		
		Map<String, Object> param = new HashMap<>();
		param.put("minId", minId);
		param.put("maxId", maxId);
		
		return new MyBatisCursorItemReaderBuilder<Expense>()
				.sqlSessionFactory(sqlSessionFactory)
				.queryId("com.example.expenses.repository.ExpenseMapper.findByIdRange")
				.parameterValues(param)
				.build();
		
	}
	
	@Bean
	@StepScope
	FlatFileItemWriter<Expense> parallelWriter(
			@Value("#{stepExecutionContext['partition']}")String partition,
			@Value("#{jobParameters['outputDir']}") String outputDir) {
		
		BeanWrapperFieldExtractor<Expense> fieldExtractor = new BeanWrapperFieldExtractor<>();
		fieldExtractor.setNames(new String[] {"id", "applicantId", "title", "amount", "currency", "status"});
		
		DelimitedLineAggregator<Expense> lineAggregator = new DelimitedLineAggregator<>();
		lineAggregator.setDelimiter(",");
		lineAggregator.setFieldExtractor(fieldExtractor);
		
		String fileName = outputDir + "/expenses_" + partition + ".csv";
		
		return new FlatFileItemWriterBuilder<Expense>()
				.name("parallelWriter")
				.resource(new FileSystemResource(fileName))
				.encoding("MS932")
				.lineAggregator(lineAggregator)
				.headerCallback(writer -> writer.write("id,applicant_id,title,amount,currency,status"))
				.build();
	}
	
	@Bean
	Step workerStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			MyBatisCursorItemReader<Expense> parallelReader,
			FlatFileItemWriter<Expense> parallelWriter) {
		return new ChunkOrientedStepBuilder<Expense, Expense>("workerStep", jobRepository, 100)
				.transactionManager(transactionManager)
				.reader(parallelReader)
				.writer(parallelWriter)
				.build();
	}
	
	@Bean
	Step masterStep(JobRepository jobRepository,
			Step workerStep,
			TaskExecutor taskExecutor) {
		
	TaskExecutorPartitionHandler partitionHandler =new TaskExecutorPartitionHandler();
	partitionHandler.setGridSize(4);
	partitionHandler.setTaskExecutor(taskExecutor);
	partitionHandler.setStep(workerStep);
	
	try {
		partitionHandler.afterPropertiesSet();
		
	}catch (Exception e) {
		throw new RuntimeException(e);
	}
	
	return new StepBuilder("masterStep", jobRepository)
			.partitioner("workerStep", new RangePartitioner())
			.partitionHandler(partitionHandler)
			.build();
	}
	
	@Bean
	Job parallelExportJob(JobRepository jobRepository, Step masterStep) {
		return new JobBuilder("parallelExportJob", jobRepository)
				.start(masterStep)
				.build();
		
	}
	
}
