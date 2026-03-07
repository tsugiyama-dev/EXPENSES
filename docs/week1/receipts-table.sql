-- 領収書テーブルの作成

CREATE TABLE receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '領収書ID',
    expense_id BIGINT NOT NULL COMMENT '経費ID（外部キー）',
    original_filename VARCHAR(255) NOT NULL COMMENT '元のファイル名',
    stored_filename VARCHAR(255) NOT NULL COMMENT '保存時のファイル名（UUID）',
    file_path VARCHAR(500) NOT NULL COMMENT 'ストレージ内のパス',
    content_type VARCHAR(100) NOT NULL COMMENT 'MIMEタイプ',
    file_size BIGINT NOT NULL COMMENT 'ファイルサイズ（バイト）',
    uploaded_by VARCHAR(255) NOT NULL COMMENT 'アップロードしたユーザー',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'アップロード日時',
    FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    INDEX idx_expense_id (expense_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='経費の領収書ファイル';

-- 実行方法:
-- mysql -u app -p newschema < docs/week1/receipts-table.sql
