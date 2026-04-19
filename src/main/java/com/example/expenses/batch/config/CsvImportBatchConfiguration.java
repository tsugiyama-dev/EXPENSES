package com.example.expenses.batch.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.expenses.batch.dto.ExpenseCsvRow;
import com.example.expenses.batch.processor.ExpenseCsvItemProcessor;
import com.example.expenses.batch.writer.ExpenseCsvItemWriter;
import com.example.expenses.domain.Expense;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CsvImportBatchConfiguration {

	private final ExpenseCsvItemProcessor processor;
	private final ExpenseCsvItemWriter writer;
	
	@Bean
	FlatFileItemReader<ExpenseCsvRow> expenseCsvReader() {
		
		return new FlatFileItemReaderBuilder<ExpenseCsvRow>()
				.name("expenseCsvReader")
				.resource(new ClassPathResource("csv/sample.csv"))
				.linesToSkip(1) // ヘッダー行をスキップ
				.delimited()
				.names("applicantId", "title", "amount", "currency")
//				.lineMapper((line, lineNumber) -> {
//					String[] tokens = line.split(",");
//					ExpenseCsvRow row = new ExpenseCsvRow();
//					row.setApplicantId(Long.parseLong(tokens[0].trim()));
//					row.setTitle(tokens[1].trim());
//					row.setAmount(tokens[2].trim());
//					row.setCurrency(tokens[3].trim());
//					return row;
//				})
				.targetType(ExpenseCsvRow.class)
				.build();
	}
	
	@Bean
	Step csvImportStep(JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			FlatFileItemReader<ExpenseCsvRow> expenseCsvReader) {
		return new StepBuilder("csvImportStep", jobRepository)
				.<ExpenseCsvRow, Expense>chunk(100, transactionManager)
				.reader(expenseCsvReader)
				.processor(processor)
				.writer(writer)
				.faultTolerant()
				.skipLimit(1000)
				.skip(Exception.class) // 例外が発生した行はスキップ
				.build();
	}
	
	@Bean
	Job csvImportJob(JobRepository jobRepository, Step csvImportStep) {
		return new JobBuilder("csvImportJob", jobRepository)
				.start(csvImportStep)
				.build();
		
	}
}
