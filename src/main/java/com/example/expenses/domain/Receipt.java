package com.example.expenses.domain;

import java.time.LocalDateTime;

/**
 * 領収書エンティティ
 *
 * <p>経費申請に添付される領収書ファイルの情報を表します。</p>
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>ファイルメタデータの設計</li>
 *   <li>元のファイル名と保存用ファイル名の分離</li>
 *   <li>セキュリティを考慮したファイル管理</li>
 * </ul>
 */
public class Receipt {

    /**
     * 領収書ID
     */
    private Long id;

    /**
     * 経費ID（外部キー）
     */
    private Long expenseId;

    /**
     * 元のファイル名（ユーザーがアップロードした名前）
     * 例: "領収書_2026年3月.jpg"
     */
    private String originalFilename;

    /**
     * 保存時のファイル名（UUID + 拡張子）
     * 例: "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
     */
    private String storedFilename;

    /**
     * ストレージ内のファイルパス
     * 例: "2026/03/07/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
     */
    private String filePath;

    /**
     * MIMEタイプ
     * 例: "image/jpeg", "application/pdf"
     */
    private String contentType;

    /**
     * ファイルサイズ（バイト）
     */
    private Long fileSize;

    /**
     * アップロードしたユーザーのメールアドレス
     */
    private String uploadedBy;

    /**
     * アップロード日時
     */
    private LocalDateTime uploadedAt;

    // コンストラクタ
    public Receipt() {
    }

    public Receipt(Long expenseId, String originalFilename, String storedFilename,
                   String filePath, String contentType, Long fileSize, String uploadedBy) {
        this.expenseId = expenseId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getter / Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "Receipt{" +
                "id=" + id +
                ", expenseId=" + expenseId +
                ", originalFilename='" + originalFilename + '\'' +
                ", storedFilename='" + storedFilename + '\'' +
                ", filePath='" + filePath + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
