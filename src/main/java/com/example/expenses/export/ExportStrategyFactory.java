package com.example.expenses.export;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ExportStrategyFactory {

	private final List<ExportStrategy> exportStrategy;
	
	@Bean
	Map<String, ExportStrategy> getStrategies() {
		
		return exportStrategy.stream().collect(Collectors.toMap(
				ExportStrategy::getFileExtension,
				Function.identity()));
	}
}
