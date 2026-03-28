package com.example.expenses.export;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

import com.example.expenses.domain.Expense;

@Service
public class ExcelExportService {



	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	
	
	public byte[] exportExpenseList(List<Expense> expenses) throws IOException {
		
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("経費一覧");
			
			// スタイルの作成
			CellStyle headerStyle = createHeaderStyle(workbook);
			CellStyle dataStyle = createDataStyle(workbook);
			CellStyle currencyStyle = createCurrencyStyle(workbook);
			CellStyle dateStyle  = createDateStyle(workbook);
			
			// タイトル行
			createTitleRow(sheet, workbook);
			
			// ヘッダー行
			createHeaderRow(sheet, headerStyle);
			
			// データ行
			int rowNum = 3; // タイトル行(0)、空行(1)、ヘッダー行(2)の次
			for(Expense expense : expenses) {
				createDataRow(sheet, expense, rowNum++, dataStyle, currencyStyle, dateStyle);
			}
			
			// 合計行
			createTotalRow(sheet, expenses, rowNum, headerStyle, currencyStyle);
			
			
			// 列幅の自動調整
			autoSizeColumns(sheet, 9);
			
			// ByteArrayOutputStreamに書き込み
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}
	
	/**
	 * タイトル行を作成
	 */
	private void createTitleRow(Sheet sheet, Workbook workbook) {
		Row titleRow = sheet.createRow(0);
		
		Cell titleCell = titleRow.createCell(0);
		titleCell.setCellValue("経費一覧レポート - 出力日時: " + LocalDateTime.now().format(DATE_FORMATTER));
		
		// titleスタイル
		CellStyle titleStyle = workbook.createCellStyle();
		Font titleFont = workbook.createFont();
		titleFont.setBold(true); // 太字
		titleFont.setFontHeightInPoints((short) 14); //文字大きさ
		titleStyle.setFont(titleFont); //
		titleCell.setCellStyle(titleStyle);
	}
	
	/**
	 * ヘッダー行を作成
	 */
	private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
		
		Row headerRow = sheet.createRow(2);
		String[] headers = {"No", "申請者ID","タイトル","金額","通貨","ステータス","提出日時","作成日時"};
		
		for(int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i + 1);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}
	}
	
	/**
	 * データ行を作成します
	 */
	private void createDataRow(Sheet sheet, Expense expense, int rowNum, CellStyle dataStyle, CellStyle currencyStyle, CellStyle dateStyle) {
		
		Row row = sheet.createRow(rowNum);
		
		//No
		Cell noCell = row.createCell(1);
		noCell.setCellValue(rowNum - 2);
		noCell.setCellStyle(dateStyle);
		
		//申請者ID
		Cell applicantIdCell = row.createCell(2);
		applicantIdCell.setCellValue(expense.getApplicantId());
		applicantIdCell.setCellStyle(dataStyle);
		
		// title
		Cell titleCell = row.createCell(3);
		titleCell.setCellValue(expense.getTitle());
		titleCell.setCellStyle(dataStyle);
		
		// 金額
		Cell amountCell = row.createCell(4);
		amountCell.setCellValue(expense.getAmount().doubleValue());
		amountCell.setCellStyle(currencyStyle);
		
		// 通貨
		Cell currencyCell = row.createCell(5);
		currencyCell.setCellValue(expense.getCurrency());
		currencyCell.setCellStyle(dataStyle);
		
		// ステータス
		Cell statusCell = row.createCell(6);
		statusCell.setCellValue(expense.getStatus().toString());
		statusCell.setCellStyle(dataStyle);
		
		// 提出日時
		Cell submittedAtCell = row.createCell(7);
		if(expense.getSubmittedAt() != null) {
			submittedAtCell.setCellValue(expense.getSubmittedAt().format(DATE_FORMATTER));
		}else {
			submittedAtCell.setCellValue("未提出");
		}
		submittedAtCell.setCellStyle(dataStyle);
		
		// 作成日時
		Cell createdAtCell = row.createCell(8);
		createdAtCell.setCellValue(expense.getCreatedAt().format(DATE_FORMATTER));
		createdAtCell.setCellStyle(dataStyle);
		
	}
	
	/**
	 * 合計行を作成
	 */
	private void createTotalRow(Sheet sheet, List<Expense> expenses, int rowNum, CellStyle headerStyle, CellStyle currencyStyle) {
		
		Row totalRow = sheet.createRow(rowNum);
		
		// 合計ラベル
		Cell labelcell = totalRow.createCell(1);
		labelcell.setCellValue("合計");
		labelcell.setCellStyle(headerStyle);
		
		// 件数
		Cell  countCell = totalRow.createCell(2);
		countCell.setCellValue("件数: " + expenses.size());
		countCell.setCellStyle(headerStyle);
		
		// 合計金額
		BigDecimal totalAmount = expenses.stream()
				.map(Expense::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		Cell totalAmountCell = totalRow.createCell(4);
		totalAmountCell.setCellValue(totalAmount.doubleValue());
		totalAmountCell.setCellStyle(currencyStyle);
		
	}
	
	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		
		// 背景色（青）
		style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		// フォント
		Font font = workbook.createFont();
		font.setBold(true);
		font.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(font);
		
		// 枠線
		setBorders(style);
		
		// 中央揃え
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		
		return style;
		
	}
	
	
	/**
	 * データスタイルを作成
	 */
	private CellStyle createDataStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		setBorders(style);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		return style;
	}
	/**
	 * 通貨スタイルを作成
	 */
	private CellStyle createCurrencyStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		setBorders(style);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		
		// 通貨フォーマット（￥123,456)
		DataFormat format = workbook.createDataFormat();
		style.setDataFormat(format.getFormat("#,##0"));
		
		return style;
	}
	
	/**
	 * 日付スタイルを作成
	 */
	private CellStyle createDateStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		setBorders(style);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		return style;
	}
	
	/**
	 * セルに枠線を設定
	 */
	private void setBorders(CellStyle style) {
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		
	}
	
	/**
	 * 列幅を自動調整
	 */
	private void autoSizeColumns(Sheet sheet, int columnCount) {
		for(int i = 1; i < columnCount; i++) {
			sheet.autoSizeColumn(i);
			// 少し余裕をもたせる
			sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512);
		}
			
		
	}
}
