package com.example.expenses.controller;

import com.example.expenses.config.LoginUser;
import com.example.expenses.domain.Receipt;
import com.example.expenses.service.ReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 領収書コントローラー
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>マルチパートファイルアップロードのAPI設計</li>
 *   <li>ファイルダウンロードのレスポンス設定</li>
 *   <li>RESTful APIのベストプラクティス</li>
 *   <li>Spring Securityとの連携</li>
 * </ul>
 *
 * <p><b>エンドポイント:</b></p>
 * <ul>
 *   <li>POST   /api/expenses/{expenseId}/receipts - 領収書をアップロード</li>
 *   <li>GET    /api/expenses/{expenseId}/receipts - 領収書一覧を取得</li>
 *   <li>GET    /api/receipts/{receiptId}/download - 領収書をダウンロード</li>
 *   <li>DELETE /api/receipts/{receiptId} - 領収書を削除</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class ReceiptController {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptController.class);

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /**
     * 領収書をアップロードします
     *
     * <p><b>リクエスト例:</b></p>
     * <pre>
     * POST /api/expenses/123/receipts
     * Content-Type: multipart/form-data
     *
     * file: (バイナリデータ)
     * </pre>
     *
     * <p><b>レスポンス例:</b></p>
     * <pre>
     * {
     *   "id": 1,
     *   "expenseId": 123,
     *   "originalFilename": "receipt.jpg",
     *   "fileSize": 1024000,
     *   "contentType": "image/jpeg",
     *   "uploadedAt": "2026-03-07T10:30:00"
     * }
     * </pre>
     *
     * @param expenseId 経費ID
     * @param file アップロードされたファイル
     * @param loginUser ログインユーザー
     * @return アップロードされた領収書情報
     */
    @PostMapping("/expenses/{expenseId}/receipts")
    public ResponseEntity<Map<String, Object>> uploadReceipt(
            @PathVariable Long expenseId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal LoginUser loginUser) {

        logger.info("領収書アップロード開始: expenseId={}, filename={}, user={}",
                expenseId, file.getOriginalFilename(), loginUser.getUsername());

        Receipt receipt = receiptService.uploadReceipt(expenseId, file, loginUser.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("id", receipt.getId());
        response.put("expenseId", receipt.getExpenseId());
        response.put("originalFilename", receipt.getOriginalFilename());
        response.put("fileSize", receipt.getFileSize());
        response.put("contentType", receipt.getContentType());
        response.put("uploadedAt", receipt.getUploadedAt());

        logger.info("領収書アップロード完了: receiptId={}", receipt.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 経費に紐づく領収書一覧を取得します
     *
     * <p><b>リクエスト例:</b></p>
     * <pre>
     * GET /api/expenses/123/receipts
     * </pre>
     *
     * <p><b>レスポンス例:</b></p>
     * <pre>
     * {
     *   "receipts": [
     *     {
     *       "id": 1,
     *       "originalFilename": "receipt1.jpg",
     *       "fileSize": 1024000,
     *       "contentType": "image/jpeg",
     *       "uploadedAt": "2026-03-07T10:30:00"
     *     },
     *     {
     *       "id": 2,
     *       "originalFilename": "receipt2.pdf",
     *       "fileSize": 2048000,
     *       "contentType": "application/pdf",
     *       "uploadedAt": "2026-03-07T11:00:00"
     *     }
     *   ],
     *   "count": 2
     * }
     * </pre>
     *
     * @param expenseId 経費ID
     * @param loginUser ログインユーザー
     * @return 領収書一覧
     */
    @GetMapping("/expenses/{expenseId}/receipts")
    public ResponseEntity<Map<String, Object>> getReceipts(
            @PathVariable Long expenseId,
            @AuthenticationPrincipal LoginUser loginUser) {

        logger.info("領収書一覧取得: expenseId={}, user={}", expenseId, loginUser.getUsername());

        List<Receipt> receipts = receiptService.getReceipts(expenseId, loginUser.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("receipts", receipts);
        response.put("count", receipts.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 領収書をダウンロードします
     *
     * <p><b>リクエスト例:</b></p>
     * <pre>
     * GET /api/receipts/1/download
     * </pre>
     *
     * <p><b>レスポンス:</b></p>
     * <ul>
     *   <li>Content-Type: ファイルのMIMEタイプ（例: image/jpeg）</li>
     *   <li>Content-Disposition: attachment; filename="receipt.jpg"</li>
     *   <li>Body: ファイルのバイナリデータ</li>
     * </ul>
     *
     * @param receiptId 領収書ID
     * @param loginUser ログインユーザー
     * @return ファイルリソース
     */
    @GetMapping("/receipts/{receiptId}/download")
    public ResponseEntity<Resource> downloadReceipt(
            @PathVariable Long receiptId,
            @AuthenticationPrincipal LoginUser loginUser) {

        logger.info("領収書ダウンロード: receiptId={}, user={}", receiptId, loginUser.getUsername());

        // メタデータ取得
        Receipt receipt = receiptService.getReceipt(receiptId);

        // ファイル取得
        Resource resource = receiptService.downloadReceipt(receiptId, loginUser.getUsername());

        // Content-Typeヘッダーの設定
        String contentType = receipt.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // Content-Dispositionヘッダーの設定
        // ブラウザでダウンロードさせる（inline ではなく attachment）
        String filename = receipt.getOriginalFilename();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * 領収書を削除します
     *
     * <p><b>リクエスト例:</b></p>
     * <pre>
     * DELETE /api/receipts/1
     * </pre>
     *
     * <p><b>レスポンス例:</b></p>
     * <pre>
     * {
     *   "message": "領収書を削除しました"
     * }
     * </pre>
     *
     * @param receiptId 領収書ID
     * @param loginUser ログインユーザー
     * @return 削除結果
     */
    @DeleteMapping("/receipts/{receiptId}")
    public ResponseEntity<Map<String, String>> deleteReceipt(
            @PathVariable Long receiptId,
            @AuthenticationPrincipal LoginUser loginUser) {

        logger.info("領収書削除: receiptId={}, user={}", receiptId, loginUser.getUsername());

        receiptService.deleteReceipt(receiptId, loginUser.getUsername());

        Map<String, String> response = new HashMap<>();
        response.put("message", "領収書を削除しました");

        return ResponseEntity.ok(response);
    }
}
