package com.example.expenses.controller;

import com.example.expenses.config.LoginUser;
import com.example.expenses.domain.Expense;
import com.example.expenses.dto.request.ExpenseSearchCriteria;
import com.example.expenses.export.ExcelExportService;
import com.example.expenses.export.PdfExportService;
import com.example.expenses.service.ExpenseService;
import com.example.expenses.service.ExpenseViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * エクスポートコントローラー
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>ファイルダウンロードのレスポンス設定</li>
 *   <li>Content-Typeヘッダーの設定</li>
 *   <li>Content-Dispositionヘッダーの設定</li>
 *   <li>ファイル名のURLエンコード（日本語対応）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final ExpenseViewService expenseViewService;
    private final ExpenseService expenseService;

    public ExportController(ExcelExportService excelExportService,
                            PdfExportService pdfExportService,
                            ExpenseViewService expenseViewService,
                            ExpenseService expenseService) {
        this.excelExportService = excelExportService;
        this.pdfExportService = pdfExportService;
        this.expenseViewService = expenseViewService;
        this.expenseService = expenseService;
    }

    /**
     * 経費一覧をExcel形式でエクスポートします
     *
     * <p><b>リクエスト例:</b></p>
     * <pre>
     * GET /api/exports/excel/expenses?status=APPROVED
     * </pre>
     *
     * @param criteria 検索条件
     * @param loginUser ログインユーザー
     * @return Excelファイル
     */
    @GetMapping("/excel/expenses")
    public ResponseEntity<byte[]> exportExpensesToExcel(
            @ModelAttribute ExpenseSearchCriteria criteria,
            @AuthenticationPrincipal LoginUser loginUser) {

        logger.info("経費一覧Excelエクスポート開始: user={}, criteria={}", loginUser.getUsername(), criteria);

        try {
            // 経費一覧を取得（ページネーションなし、全件取得）
            List<Expense> expenses = expenseViewService.search(criteria).items();

            // Excelファイル生成
            byte[] excelData = excelExportService.exportExpenseList(expenses);

            // ファイル名生成（例: 経費一覧_20260307.xlsx）
            String filename = generateFilename("経費一覧", "xlsx");

            logger.info("経費一覧Excelエクスポート完了: user={}, 件数={}", loginUser.getUsername(), expenses.size());

            // レスポンスヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", encodeFilename(filename));
            headers.setContentLength(excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (IOException e) {
            logger.error("経費一覧Excelエクスポートエラー: user={}", loginUser.getUsername(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 経費一覧をPDF形式でエクスポートします
     *
     * <p><b>リクエスト例:</b></p>
     * <pre>
     * GET /api/exports/pdf/expenses?status=APPROVED
     * </pre>
     *
     * @param criteria 検索条件
     * @param loginUser ログインユーザー
     * @return PDFファイル
     */
    @GetMapping("/pdf/expenses")
    public ResponseEntity<byte[]> exportExpensesToPdf(
            @ModelAttribute ExpenseSearchCriteria criteria,
            @AuthenticationPrincipal LoginUser loginUser) {

        logger.info("経費一覧PDFエクスポート開始: user={}, criteria={}", loginUser.getUsername(), criteria);

        try {
            // 経費一覧を取得
            List<Expense> expenses = expenseViewService.search(criteria).items();

            // PDFファイル生成
            byte[] pdfData = pdfExportService.exportExpenseList(expenses);

            // ファイル名生成
            String filename = generateFilename("経費一覧", "pdf");

            logger.info("経費一覧PDFエクスポート完了: user={}, 件数={}", loginUser.getUsername(), expenses.size());

            // レスポンスヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodeFilename(filename));
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);

        } catch (IOException e) {
            logger.error("経費一覧PDFエクスポートエラー: user={}", loginUser.getUsername(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 経費詳細をPDF形式でエクスポートします
     *
     * <p><b>リクエスト例:</b></p>
     * <pre>
     * GET /api/exports/pdf/expense/123
     * </pre>
     *
     * @param expenseId 経費ID
     * @param loginUser ログインユーザー
     * @return PDFファイル
     */
    @GetMapping("/pdf/expense/{expenseId}")
    public ResponseEntity<byte[]> exportExpenseDetailToPdf(
            @PathVariable Long expenseId,
            @AuthenticationPrincipal LoginUser loginUser) {

        logger.info("経費詳細PDFエクスポート開始: user={}, expenseId={}", loginUser.getUsername(), expenseId);

        try {
            // 経費を取得
            Expense expense = expenseService.findById(expenseId);

            if (expense == null) {
                logger.warn("経費が見つかりません: expenseId={}", expenseId);
                return ResponseEntity.notFound().build();
            }

            // TODO: アクセス権限チェック（自分の経費 or 承認者のみ）

            // PDFファイル生成
            byte[] pdfData = pdfExportService.exportExpenseDetail(expense);

            // ファイル名生成（例: 経費詳細_12345_20260307.pdf）
            String filename = generateFilename("経費詳細_" + expenseId, "pdf");

            logger.info("経費詳細PDFエクスポート完了: user={}, expenseId={}", loginUser.getUsername(), expenseId);

            // レスポンスヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodeFilename(filename));
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);

        } catch (IOException e) {
            logger.error("経費詳細PDFエクスポートエラー: user={}, expenseId={}", loginUser.getUsername(), expenseId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ファイル名を生成します
     *
     * @param baseName ベース名（例: "経費一覧"）
     * @param extension 拡張子（例: "xlsx"）
     * @return ファイル名（例: "経費一覧_20260307.xlsx"）
     */
    private String generateFilename(String baseName, String extension) {
        String date = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        return baseName + "_" + date + "." + extension;
    }

    /**
     * ファイル名をURLエンコードします（日本語対応）
     *
     * @param filename ファイル名
     * @return エンコードされたファイル名
     */
    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20"); // スペースを%20に変換
    }
}
