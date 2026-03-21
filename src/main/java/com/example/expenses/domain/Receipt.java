package com.example.expenses.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Receipt {

	private Long id;
	private Long expenseId;
	/**
	 * 元のファイル名
	 */
	private String originalFilename;
	/**
	 * UUID + 拡張子
	 */
	private String storedFilename;
	private String filePath;
	/**
	 * MIMEtype "image/jpeg" "application/pdf"
	 */
	private String contentType;
	private Long fileSize;
	/**
	 * アップロードしたユーザーのメールアドレス
	 */
	private String uploadedBy;
	private LocalDateTime uploadedAt;
	
	public Receipt(
			Long expenseId, String originalname, String storedFilename,
			String filePath, String contentType, Long fileSize, String uploadedBy) {
		this.expenseId = expenseId;
		this.originalFilename = originalname;
		this.storedFilename = storedFilename;
		this.filePath = filePath;
		this.contentType = contentType;
		this.fileSize = fileSize;
		this.uploadedBy = uploadedBy;
		this.uploadedAt = LocalDateTime.now();
		
		
	}
	@Override
	public String toString() {
		return "Receipt{" + 
					"id=" + id + 
					", expenseId=" + expenseId + 
					", originalFilename=" + originalFilename + 
					", storedFilename=" + storedFilename + 
					", filePath=" + filePath + 
					", contentType=" + contentType + 
					", fileSize=" + fileSize + 
					", uploadedBy=" + uploadedBy + 
					", uploadedAt=" + uploadedAt;
	}
	
}
