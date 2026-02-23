package com.example.expenses.export.strategy;

import java.nio.charset.Charset;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.expenses.domain.Expense;
import com.example.expenses.export.ExportStrategy;

@Component
public class CsvExportStrategy implements ExportStrategy {

	private final String HEADER = "ID,APPLICANTID,TITLE,AMOUNT,CURRENCY,STATUS,SUBMITTEDAT,CREATEDAT,UPDATEDAT,VERSION";
	private final Charset MS932 = Charset.forName("MS932");
	
	@Override
	public byte[] export(List<Expense> expenses) {
		
		boolean isHeader = true;
		StringBuilder csv = new StringBuilder();
		
		
		if(isHeader) {
			csv.append(HEADER + "\n");
			isHeader = false;
		}
		
		for(Expense expense : expenses) {
			
			csv.append(expense.getId() + ",");
			csv.append(expense.getApplicantId() + ",");
			csv.append(quote(expense.getTitle()) + ",");
			csv.append(expense.getAmount() + ",");
			csv.append(expense.getCurrency() + ",");
			csv.append(expense.getStatus() + ",");
			csv.append(expense.getSubmittedAt() + ",");
			csv.append(expense.getCreatedAt() + ",");
			csv.append(expense.getUpdatedAt() + "\n");
		}
		
		return csv.toString().getBytes(MS932);
	}

	@Override
	public String getContentType() {
		return "text/csv; charset=MS932";
	}

	@Override
	public String getFileExtension() {
		return "csv";
	}
	
	private String quote(String value) {
		
		if(value == null || value.isBlank()) return"";
		
		if(value.contains(",") || value.contains("\n") || value.contains("\"")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		
		return value;
	}

}
