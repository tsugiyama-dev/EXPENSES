package com.example.expenses.batch.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.expenses.domain.Expense;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class PagingBatchConfiguration {

	private final SqlSessionFactory sqlSessionFactory;
	
	@Bean
	@StepScope
	MyBatisPagingItemReader<Expense> expensePagingReader(
			@Value("#{jobParameters['pageSize'] ?: 1000}")Long pageSize,
			@Value("#{jobParameters['maxId'] ?: 0}") Long maxId) {
		
		Map<String, Object> parameterValues = new HashMap<>();
		parameterValues.put("maxId", maxId);
		
		return new MyBatisPagingItemReaderBuilder<Expense>() 
				.sqlSessionFactory(sqlSessionFactory)
				.queryId("com.example.expenses.repository.ExpenseMapper.findAllWithPaging")
				.pageSize(Math.toIntExact(pageSize))
				.parameterValues(parameterValues) 
				.build();
		
	}
	
	@Bean
	@StepScope
	FlatFileItemWriter<Expense> expensePagingCsvWriter(
			@Value("#{jobParameters['outputFile'] ?: 'src/main/resources/csv/export/expenses_paging.csv'}") String outputFile) {
		
		BeanWrapperFieldExtractor<Expense> fieldExtractor  = new BeanWrapperFieldExtractor<>();
		fieldExtractor.setNames(new String[]{"id", "applicantId", "title", "amount", "currency", "status", "createdAt"});
	
		DelimitedLineAggregator<Expense> lineAggregator = new DelimitedLineAggregator<>();
		lineAggregator.setDelimiter(",");
		lineAggregator.setFieldExtractor(fieldExtractor);
		
		return new FlatFileItemWriterBuilder<Expense>()
				.name("expensePagingCsvWriter")
				.resource(new FileSystemResource(outputFile))
				.encoding("MS932")
				.lineAggregator(lineAggregator)
				.headerCallback(writer -> writer.write("id,applicant_id,title,amount,currency,status,created_at"))
				.build();
	}
	@Bean
	Step pagingExportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			MyBatisPagingItemReader<Expense> expensePagingReader,
			FlatFileItemWriter<Expense> expensePagingCsvWriter) {
		return new StepBuilder("pagingExportStep", jobRepository)
				.<Expense, Expense>chunk(100, transactionManager)
				.reader(expensePagingReader)
				.writer(expensePagingCsvWriter)
				.build();
	}
	
	@Bean
	@Primary
    Job pagingExportJob(JobRepository jobRepository, Step pagingExportStep) {
		return new JobBuilder("pagingExportJob", jobRepository)
				.start(pagingExportStep)
				.build();
	}
	
}
