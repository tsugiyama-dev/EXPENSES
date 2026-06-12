package com.example.expenses.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.expenses.config.LoginUser;
import com.example.expenses.domain.Expense;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.export.ExcelExportImplService;
import com.example.expenses.export.ExcelExportService;
import com.example.expenses.export.PdfExportService;
import com.example.expenses.service.ExpenseExportService;
import com.example.expenses.service.ExpenseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exports")
public class ExportController {

	private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
	private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	
	private final ExcelExportService excelExportService;
	private final PdfExportService pdfExportService;
	private final ExpenseService expenseService;
	private final ExcelExportImplService excelExportImplService;
	private final ExpenseExportService expenseExportService;
	
	@GetMapping("/excel/expenses")
	public ResponseEntity<byte[]> exportExpensesToExcel(
			@ModelAttribute ExpenseSearchCriteria criteria,
			@AuthenticationPrincipal LoginUser loginUser) {
		
		logger.info("経費一覧Excelエクスポート開始：user={}, criteria={}", loginUser.getUsername(), criteria);
		
		try {
			// 経費一覧を取得
			List<Expense> expenses = expenseService.getAllExpenses(criteria, loginUser.getUserId());
			
			// Excelファイルを生成
			byte[] excelData = excelExportService.exportExpenseList(expenses);
			
			// ファイル名生成
			String filename = generateFilename("経費一覧", "xlsx");
			
			logger.info("経費一覧Excelエクスポート完了：user={},件数={}", loginUser.getUsername(), expenses.size());
			
			// レスポンスヘッダー設定
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spredsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.parse("attachment; filename*=UTF-8''" + encodeFilename(filename)));
			headers.setContentLength(excelData.length);
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(excelData);
			
		} catch(IOException e) {
			logger.error("経費一覧Excelエクスポートエラー：user={}", loginUser.getUsername(), e);
			
			return ResponseEntity.internalServerError().build();
		
		}
	}
	
	@GetMapping("/excel/expenses/loginuser")
	public ResponseEntity<byte[]> gexportExpensesToExcelLoginUser() {
		
		byte[] excelData = excelExportImplService.exportExcelData();
		
		String filename = generateFilename("経費一覧", "xlsx");
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spredsheetml.sheet"));
		headers.setContentDisposition(ContentDisposition.parse("attachment; filename*=UTF-8''" + encodeFilename(filename)));
		
		return ResponseEntity.ok().headers(headers).body(excelData);
	}
	
	@GetMapping("/pdf/expenses")
	public ResponseEntity<byte[]> exportExpensesToPdf(
			@ModelAttribute ExpenseSearchCriteria criteria,
			@AuthenticationPrincipal LoginUser loginUser) {
	    logger.info("経費一覧PDFエクスポート開始：user={},criteria={}", loginUser.getUsername(), criteria);
	    
	    try {
	    	List<Expense> expenses = expenseService.getAllExpenses(criteria, loginUser.getUserId());
	    	
	    	byte[] pdfData = pdfExportService.exportExpenseList(expenses);
	    	
	    	String filename = generateFilename("経費一覧", "pdf");
	    	
	    	logger.info("経費一覧PDFエクスポート完了：user={},件数={}", loginUser.getUsername(), expenses.size());
	    	
	    	HttpHeaders headers = new HttpHeaders();
	    	headers.setContentType(MediaType.APPLICATION_PDF);
	    	headers.setContentDisposition(ContentDisposition.parse("filename*=UTF-8''" + encodeFilename(filename)));
	    	headers.setContentLength(pdfData.length);
	    	
	    	return ResponseEntity.ok()
	    			.headers(headers)
	    			.body(pdfData);
	    }catch(IOException e) {
	    	logger.error("経費一覧PDFエクスポートエラー：user={}", loginUser.getUsername(), e);
	    	return ResponseEntity.internalServerError().build();
	    }
	}
	
	@GetMapping("/pdf/expense/{expenseId}")
	public ResponseEntity<byte[]> exportExpenseDetailToPdf(
			@PathVariable Long expenseId,
			@AuthenticationPrincipal LoginUser loginUser) {
		
		
		logger.info("経費詳細PDFエクスポート開始: user={},expenseId={}", loginUser.getUsername(), expenseId);
		
		try {
			Expense expense = expenseService.getExpense(expenseId);
			
			if(Objects.isNull(expense)) {
				return ResponseEntity.notFound().build();
			}
			
			byte[] pdfData = pdfExportService.exportExpenseDetail(expense);
			String filename = generateFilename("経費詳細_" + expenseId, "pdf");
			
			logger.info("経費詳細PDFエクスポート完了: user={},expenseId={}", loginUser.getUsername(), expenseId);
			
			HttpHeaders header = new HttpHeaders();
			header.setContentType(MediaType.APPLICATION_PDF);
			header.setContentDisposition(ContentDisposition.attachment().filename(encodeFilename(filename)).build());
			header.setContentLength(pdfData.length);
			
			return ResponseEntity.ok().headers(header).body(pdfData);
		}catch (IOException e) {
			logger.error("経費詳細レポートエクスポートエラー：user={],expenseId={]", loginUser.getUsername(), expenseId);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	/**
	 * Cursor を使った CSV ストリーミングエクスポート
	 *
	 * 既存の Excel/PDF エンドポイントとの違い：
	 *   既存：byte[] を全件分メモリに構築してから返す
	 *   ここ：response.getOutputStream() に1行ずつ書き出す
	 *         → サービス側の Cursor と合わせて、件数に依存しない一定メモリで動作する
	 *
	 * 注意：サービスの @Transactional はこの同期呼び出しの間ずっと有効。
	 *       StreamingResponseBody（別スレッド実行）を使うとトランザクションが
	 *       切れて Cursor が閉じるため、ここでは同期書き出しにしている。
	 */
	@GetMapping("/csv/expenses/stream")
	public void exportExpensesToCsvStream(
			HttpServletResponse response,
			@AuthenticationPrincipal LoginUser loginUser) throws IOException {

		logger.info("経費一覧CSVストリーミングエクスポート開始：user={}", loginUser.getUsername());

		String filename = generateFilename("経費一覧", "csv");

		response.setContentType("text/csv");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename*=UTF-8''" + encodeFilename(filename));

		expenseExportService.exportAllAsCsvStream(response.getOutputStream());

		logger.info("経費一覧CSVストリーミングエクスポート完了：user={}", loginUser.getUsername());
	}

	/**
	 *
	 */
	private String generateFilename(String baseName, String extension) {
		String date = LocalDateTime.now().format(FILE_DATE_FORMATTER);
		return baseName + "_" + date + "." + extension;
	}
	
	private String encodeFilename(String filename) {
		return URLEncoder.encode(filename, StandardCharsets.UTF_8)
				.replaceAll("\\+", "%20");
	}

}
