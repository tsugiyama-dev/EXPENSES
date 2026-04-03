package com.example.expenses.batch.tasklet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.expenses.dto.batch.MonthlyExpenseReport;

@Component
public class ReportGenerationTasklet implements Tasklet {

	private static final Logger logger = LoggerFactory.getLogger(ReportGenerationTasklet.class);
	
	@Value("${batch.report.output-dir}")
	private String outputDir;
	
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		
		logger.info("レポート生成タスクレット開始");
		
		MonthlyExpenseReport report = (MonthlyExpenseReport) chunkContext.getStepContext()
				.getStepExecution()
				.getJobExecution()
				.getExecutionContext()
				.get("monthlyReport");
		
		if(report == null) {
			throw new IllegalStateException(" 月次レポートが見つかりません");
		}
		
		File reportFile = generateExcelReport(report);
		logger.info("レポートファイル生成：{}", reportFile.getAbsolutePath());
		
		
		chunkContext.getStepContext()
		.getStepExecution()
		.getJobExecution()
		.getExecutionContext()
		.put("reportFilePath", reportFile.getAbsolutePath());
		
		logger.info("レポート生成タスクレット完了");
		
		return RepeatStatus.FINISHED;
		
	}
	
	private File generateExcelReport(MonthlyExpenseReport report) throws IOException {
		
		File dir = new File(outputDir);
		if(!dir.exists()) {
			dir.mkdirs();
		}
		
		String filename = String.format("monthly_report_%s.xlsx", 
				report.getTargetMonth().format(DateTimeFormatter.ofPattern("yyyyMM")));
		
		File file = new File(dir, filename);
		
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("月次レポート");
			
			CellStyle headerStyle = createHeaderStyle(workbook);
			CellStyle dataStyle = createDataStyle(workbook);
			CellStyle currencyStyle = createCurrencyStyle(workbook);
			
			int rowNum = 0;
			
			Row titleRow = sheet.createRow(rowNum++);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellValue("月次経費レポート - " + 
			   report.getTargetMonth().format(DateTimeFormatter.ofPattern("yyyy年MM月")));
			
			rowNum++;
			
			Row headerRow  = sheet.createRow(rowNum++);
			String[] headers = {"ステータス", "件数", "合計金額", "割合"};
			for(int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}
			
			report.getStatusSummaries().forEach((status, summary) -> {
				Row dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
				
				Cell statusCell = dataRow.createCell(0);
				statusCell.setCellValue(status.toString());
				statusCell.setCellStyle(dataStyle);
				
				Cell countCell = dataRow.createCell(1);
				countCell.setCellValue(summary.getCount());
				countCell.setCellStyle(dataStyle);
			
				
				Cell amountCell = dataRow.createCell(2);
				amountCell.setCellValue(summary.getAmount().doubleValue());
				amountCell.setCellStyle(currencyStyle);
				
				
				Cell percentageCell = dataRow.createCell(3);
				percentageCell.setCellValue(String.format("%.1f%%",  summary.getPercentage()));
				percentageCell.setCellStyle(dataStyle);
				
			});
			
			Row totalRow = sheet.createRow(sheet.getLastRowNum() + 1);
			Cell totalLabelCell = totalRow.createCell(0);
			totalLabelCell.setCellValue("合計");
			totalLabelCell.setCellStyle(headerStyle);
			
			Cell totalCountCell = totalRow.createCell(1);
			totalCountCell.setCellValue(report.getTotalCount());
			totalCountCell.setCellStyle(headerStyle);
			
			Cell totalAmountCell = totalRow.createCell(2);
			totalAmountCell.setCellValue(report.getTotalAmount().doubleValue());
			totalAmountCell.setCellStyle(currencyStyle);
			
			for(int i =0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
				sheet.setColumnWidth(i,  sheet.getColumnWidth(i) + 512);
			}
			
			try (FileOutputStream outputStream = new FileOutputStream(file)) {
				workbook.write(outputStream);
			}
		}
		
		return file;
	}
	
	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		Font font = workbook.createFont();
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(font);
		
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		
		return style;
	}
	
	private CellStyle createDataStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		return style;
	}
	
	private CellStyle createCurrencyStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		
		DataFormat format = workbook.createDataFormat();
		style.setDataFormat(format.getFormat("#,##0"));
		
		return style;
	}

}
