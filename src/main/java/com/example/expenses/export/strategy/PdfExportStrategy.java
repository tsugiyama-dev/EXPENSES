package com.example.expenses.export.strategy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.expenses.domain.Expense;
import com.example.expenses.export.ExportStrategy;

public class PdfExportStrategy implements ExportStrategy {

	@Override
	public byte[] export(List<Expense> expenses) {
		String pdfContent = "PDF形式の経費データ\n件数：" + expenses.size();
		return pdfContent.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public String getContentType() {

		return "application/pdf";
	}

	@Override
	public String getFileExtension() {
		return "pdf";
	}

}
