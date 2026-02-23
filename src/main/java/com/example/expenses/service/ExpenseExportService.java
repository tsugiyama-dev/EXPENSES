package com.example.expenses.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.expenses.domain.Expense;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.export.ExportStrategy;
import com.example.expenses.repository.ExpenseMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseExportService {

	private final ExpenseMapper expenseMapper;
	private final Map<String, ExportStrategy> strategies;
	private final Set<String> types = new HashSet<String>(Set.of("csv", "pdf"));
	
	public byte[] export(ExpenseSearchCriteria criteria, String exportType) {
		
		if(!types.contains(exportType)) {
			exportType = "csv";
		}
		
		ExportStrategy strategy = strategies.get(exportType);
		
		List<Expense> expenses = expenseMapper.filter(ExpenseSearchCriteria.toEntity(criteria), "created_at", "DESC");
		return strategy.export(expenses);
		
	}
}
