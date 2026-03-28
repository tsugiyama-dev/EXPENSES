package com.example.expenses.export;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.expenses.domain.Expense;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.service.AuthenticationContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExcelExportImplService {

	private final Logger logger = LoggerFactory.getLogger(ExcelExportImplService.class);
	private final ExpenseMapper expenseMapper;
	private final AuthenticationContext authenticationContext;
	
	public byte[] exportExcelData() {

		//ログイン中ユーザーのデータ生成
		Long userId = authenticationContext.getCurrentUserId();
		
		List<Expense> expenses = expenseMapper.findByUserId(userId);
		logger.info("Excelのエクスポート処理を開始");
		
		Workbook workbook = new XSSFWorkbook();
		String applicantId = expenses.get(0).getApplicantId().toString();
		Sheet sheet = workbook.createSheet("applicantId=" + applicantId);
		CellStyle line = workbook.createCellStyle();
		line.setBorderBottom(BorderStyle.THIN);
		line.setBorderTop(BorderStyle.THIN);
		line.setBorderRight(BorderStyle.THIN);
		line.setBorderLeft(BorderStyle.THIN);
		
		// Title
		Row row = sheet.createRow(0);
		Cell cell = row.createCell(0);
		cell.setCellValue(" applicantId = " + applicantId + " の経費一覧");
		
		CellStyle titleCellStyle = workbook.createCellStyle();
		Font fontStyle = workbook.createFont();
		fontStyle.setBold(true);
		fontStyle.setFontHeightInPoints((short)22);
		titleCellStyle.setFont(fontStyle);
		cell.setCellStyle(titleCellStyle);
	
		
		//Header style
		
		CellStyle headerStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setColor(IndexedColors.WHITE.getIndex());
		headerStyle.setFont(font);
		headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		headerStyle.setAlignment(HorizontalAlignment.CENTER);
		
		Row headerRow = sheet.createRow(2);
		int colNum = 0;
		List<String> headers = List.of("No", "title", "金額", "ステータス", "作成日時","更新日時","通貨");
		for(String header : headers) {
			Cell headerCell = headerRow.createCell(colNum++);
			headerCell.setCellValue(header);
			headerCell.setCellStyle(headerStyle);
			headerCell.setCellStyle(line);
		}
		
		
		int startRow = 3;
		int rowCnt = startRow;
		// CurrencyStyle
		DataFormat format = workbook.createDataFormat();
		CellStyle currencyStyle = workbook.createCellStyle();
		currencyStyle.setDataFormat(format.getFormat("#,##0"));
		
		//DateFormatter
		DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
		
		// Data
		for(Expense expense : expenses) {
			Row dataRow = sheet.createRow(rowCnt++);
			format.getFormat("#,##0");
				Cell idCell = dataRow.createCell(0);
				idCell.setCellValue(expense.getId());
				idCell.setCellStyle(line);
				
				Cell titleCell = dataRow.createCell(1);
				titleCell.setCellValue(expense.getTitle());
				titleCell.setCellStyle(line);
				
				Cell amountCell = dataRow.createCell(2);
				amountCell.setCellValue(expense.getAmount().doubleValue());
				amountCell.setCellStyle(currencyStyle);
				amountCell.setCellStyle(line);
				
				Cell statusCell = dataRow.createCell(3);
				statusCell.setCellValue(expense.getStatus().toString());
				statusCell.setCellStyle(line);
				
				Cell createdAtCell = dataRow.createCell(4);
				createdAtCell.setCellValue(expense.getCreatedAt().format(DATE_TIME_FORMAT));
				createdAtCell.setCellStyle(line);
				
				Cell updatedAtCell = dataRow.createCell(5);
				updatedAtCell.setCellValue(expense.getUpdatedAt().format(DATE_TIME_FORMAT));
				updatedAtCell.setCellStyle(line);
				
				Cell currencyCell = dataRow.createCell(6);
				currencyCell.setCellValue(expense.getCurrency());
				currencyCell.setCellStyle(line);
		}
		// 合計行
		BigDecimal sum = expenses.stream().map(e -> e.getAmount()).reduce(BigDecimal.ZERO,BigDecimal::add);
		Row totalRow = sheet.createRow(startRow + expenses.size() - 1);
		//
		Cell total = totalRow.createCell(1);
		total.setCellValue("合計");
		//
		Cell totalCell = totalRow.createCell(2);
		totalCell.setCellValue(sum.doubleValue());
		totalCell.setCellStyle(currencyStyle);
		
		ByteArrayOutputStream excelData = new ByteArrayOutputStream();
		try {
			workbook.write(excelData);
		} catch (IOException e) {
			
			logger.error("error 発生", e);
		}
		return  excelData.toByteArray();
	}
	
	
}
