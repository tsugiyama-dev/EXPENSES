package com.example.expenses.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.expenses.config.LoginUser;
import com.example.expenses.domain.Receipt;
import com.example.expenses.service.ReceiptService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReceiptController {

	private static final Logger logger = LoggerFactory.getLogger(ReceiptController.class);
	private final ReceiptService receiptService;
	
	
	@PostMapping("/expenses/{expenseId}/receipts")
	public ResponseEntity<Map<String, Object>> uploadReceipt(
			@PathVariable Long expenseId,
			@RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal LoginUser loginUser) {
		
		logger.info("領収書のアップロード開始：expenseId={}, filename={}, user={}",
				expenseId, file.getOriginalFilename(),loginUser.getUsername());
		
		Receipt receipt = receiptService.uploadReceipt(expenseId, file, loginUser.getUsername());
		
		Map<String, Object> response = new HashMap<>();
		response.put("id",  receipt.getId());
		response.put("expenseId", receipt.getExpenseId());
		response.put("originalFilename", receipt.getOriginalFilename());
		response.put("fileSize", receipt.getFileSize());
		response.put("contentType", receipt.getContentType());
		response.put("uploadedAt", receipt.getUploadedAt());
		
		logger.info("領収書のアップロード完了：receiptId={}", receipt.getId());
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	/**
	 * 経費一覧の取得
	 * @param expenseId
	 * @param loginUser
	 * @return
	 */
	@GetMapping("/expenses/{expenseId}/receipts")
	public ResponseEntity<Map<String, Object>> getReceipts(
			@PathVariable Long expenseId,
			@AuthenticationPrincipal LoginUser loginUser) {
		
		logger.info("領収書一覧の取得： expenseId={}, user={}", expenseId,loginUser.getUsername());
		
		List<Receipt> receipts = receiptService.getReceipts(expenseId, loginUser.getUsername());
		
		Map<String, Object> response = new HashMap<>();
		response.put("receipts", receipts);
		response.put("count", receipts.size());
		
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/receipts/{receiptId}/download")
	public ResponseEntity<Resource> downloadReceipt(
			@PathVariable Long receiptId,
			@AuthenticationPrincipal LoginUser loginUser) {
		logger.info("領収書ダウンロード：receiptId={}, user={}", receiptId, loginUser.getUsername());
		
		//メタデータ取得
		Receipt receipt = receiptService.getReceipt(receiptId);
	
		//ファイル取得
		Resource resource = receiptService.downloadReceipt(receiptId, loginUser.getUsername());
		
		// Content-Typeヘッダーの設定
		String contentType= receipt.getContentType();
		
		if(contentType == null) {
			contentType = "application/octet-stream";
		}
		
		String filename = receipt.getOriginalFilename();

	
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
				.body(resource);
	}
	
	@DeleteMapping("/receipts/{receiptId}")
	public ResponseEntity<Map<String, String>> deleteReceipt(
			@PathVariable Long receiptId,
			@AuthenticationPrincipal LoginUser loginUser) {
		logger.info("領収書の削除：receiptId={}, user={}", receiptId, loginUser.getUsername());
		
		receiptService.deleteReceipt(receiptId, loginUser.getUsername());
		
		Map<String, String> response = new HashMap<>();
		response.put("message", "領収書を削除しました");
		
		return ResponseEntity.ok(response);
		
	}
	
}
