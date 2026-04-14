package com.example.expenses.batch.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.expenses.domain.Expense;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CsvExportBatchConfiguration {

	private final SqlSessionFactory sqlSessionFactory;
	
	@Bean
	@StepScope
	MyBatisCursorItemReader<Expense> expenseDbReader() {
	    MyBatisCursorItemReader<Expense> reader = new MyBatisCursorItemReaderBuilder<Expense>()
				.sqlSessionFactory(sqlSessionFactory)
				.queryId("com.example.expenses.repository.ExpenseMapper.findAllForExport")
				.build();
	    reader.setName("expenseDbReader");
	    return reader;
	}
	
	@Bean
	@StepScope
	FlatFileItemWriter<Expense> expenseCsvWriter(
			@Value("#{jobParameters['outputFile'] ?: 'src/main/resources/csv/export/expenses.csv'}")String outputFile) {
		
		BeanWrapperFieldExtractor<Expense> fieldExtractor = new BeanWrapperFieldExtractor<>();
		fieldExtractor.setNames(new String[] {"id", "applicantId", "title", "amount", "currency", "status", "createdAt"});
		
		DelimitedLineAggregator<Expense> lineAggregator = new DelimitedLineAggregator<>();
		lineAggregator.setDelimiter(",");
		lineAggregator.setFieldExtractor(fieldExtractor);
		
		return new FlatFileItemWriterBuilder<Expense>()
				.name("expenseCsvWriter")
				.resource(new FileSystemResource(outputFile))
				.encoding("MS932")
				.lineAggregator(lineAggregator)
				.headerCallback(writer -> writer.write("id,applicantId,title,amount,currency,status,createdAt")) // ヘッダー行の追加
				.build();
		
	}
	@Bean
	Step csvExportStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			MyBatisCursorItemReader<Expense> expenseDbReader,
			FlatFileItemWriter<Expense> expenseCsvWriter) {
		
		return new StepBuilder("csvExportStep", jobRepository)
				.<Expense, Expense>chunk(100, transactionManager)
				.reader(expenseDbReader)
				.writer(expenseCsvWriter)
				.build();
	}
	@Bean
	Job csvExportJob(JobRepository jobRepository, Step csvExportStep) {
		return new JobBuilder("csvExportJob", jobRepository)
				.start(csvExportStep)
				.build();
	}
}
