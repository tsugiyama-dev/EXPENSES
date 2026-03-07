# 📦 ファイルアップロード機能 設計書

## 🎯 目的

経費申請に領収書画像をアップロードする機能を実装する。

---

## 📋 要件定義

### 機能要件

1. **アップロード機能**
   - 経費申請時に領収書画像をアップロード
   - 1つの経費に対して複数ファイル（最大5枚）をアップロード可能
   - サポートするファイル形式: JPG, PNG, PDF
   - 最大ファイルサイズ: 10MB

2. **ダウンロード機能**
   - アップロードした領収書をダウンロード
   - 権限チェック（自分の経費 or 承認者のみ）

3. **削除機能**
   - 領収書の削除（経費が申請前の場合のみ）

4. **一覧表示**
   - 経費に紐づく領収書一覧を表示

### 非機能要件

1. **セキュリティ**
   - ファイル名のサニタイズ（パストラバーサル攻撃防止）
   - MIMEタイプチェック（拡張子偽装防止）
   - ファイルサイズ制限
   - アクセス制御（他人の領収書にアクセス不可）

2. **パフォーマンス**
   - ファイルアップロード時のストリーミング処理
   - サムネイル生成（将来対応）

3. **拡張性**
   - ローカルストレージ → AWS S3への移行を見据えた抽象化

---

## 🏗️ アーキテクチャ設計

### レイヤー構成

```
┌─────────────────────────────────────┐
│  ReceiptController (REST API)       │
│  - POST   /api/expenses/{id}/receipts
│  - GET    /api/expenses/{id}/receipts
│  - GET    /api/receipts/{receiptId}/download
│  - DELETE /api/receipts/{receiptId}
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  ReceiptService (Business Logic)    │
│  - uploadReceipt()                  │
│  - getReceipts()                    │
│  - downloadReceipt()                │
│  - deleteReceipt()                  │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│  FileStorageService (Interface)     │
│  - store()                          │
│  - load()                           │
│  - delete()                         │
└─────────────────────────────────────┘
       ↓                      ↓
┌──────────────┐      ┌──────────────┐
│ LocalStorage │      │  S3Storage   │
│ (初期実装)    │      │  (将来拡張)   │
└──────────────┘      └──────────────┘
              ↓
┌─────────────────────────────────────┐
│  ReceiptMapper (MyBatis)            │
│  - insert()                         │
│  - findByExpenseId()                │
│  - findById()                       │
│  - deleteById()                     │
└─────────────────────────────────────┘
```

---

## 📊 データベース設計

### receipts テーブル

```sql
CREATE TABLE receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    INDEX idx_expense_id (expense_id)
);
```

### カラム説明

- `id`: 領収書ID（主キー）
- `expense_id`: 経費ID（外部キー）
- `original_filename`: 元のファイル名（ユーザーがアップロードした名前）
- `stored_filename`: 保存時のファイル名（UUID + 拡張子）
- `file_path`: ストレージ内のパス
- `content_type`: MIMEタイプ（image/jpeg, application/pdf など）
- `file_size`: ファイルサイズ（バイト）
- `uploaded_by`: アップロードしたユーザーのメールアドレス
- `uploaded_at`: アップロード日時

---

## 🔐 セキュリティ対策

### 1. ファイル名のサニタイズ

**脅威:** パストラバーサル攻撃（`../../etc/passwd`）

**対策:**
```java
// 元のファイル名を保存するが、保存時はUUIDを使用
String storedFilename = UUID.randomUUID().toString() + getExtension(originalFilename);
```

### 2. MIMEタイプチェック

**脅威:** 拡張子偽装（.exeを.jpgにリネーム）

**対策:**
```java
// 拡張子とMIMEタイプの両方をチェック
String contentType = file.getContentType();
if (!ALLOWED_MIME_TYPES.contains(contentType)) {
    throw new BusinessException("サポートされていないファイル形式です");
}
```

### 3. ファイルサイズ制限

**脅威:** ディスク容量の枯渇

**対策:**
```properties
# application.properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

### 4. アクセス制御

**脅威:** 他人の領収書へのアクセス

**対策:**
```java
// ダウンロード時に権限チェック
if (!canAccessReceipt(user, receipt)) {
    throw new BusinessException("アクセス権限がありません");
}
```

---

## 📁 ファイルストレージ設計

### ローカルストレージのディレクトリ構造

```
/var/expenses/uploads/
  ├── 2026/
  │   ├── 03/
  │   │   ├── 07/
  │   │   │   ├── uuid-1.jpg
  │   │   │   ├── uuid-2.pdf
  │   │   │   └── uuid-3.png
```

- 年/月/日でディレクトリを分割（検索性向上）
- ファイル名はUUID（衝突防止）

### S3移行時の設計

```
s3://my-expense-bucket/
  ├── receipts/
  │   ├── 2026/
  │   │   ├── 03/
  │   │   │   ├── 07/
  │   │   │   │   ├── uuid-1.jpg
```

---

## 🧪 テスト戦略

### 1. 単体テスト

- `FileStorageService` のテスト（モック不要、実ファイル操作）
- `ReceiptService` のテスト（FileStorageServiceをモック化）

### 2. 統合テスト

- ファイルアップロード → ダウンロード → 削除の一連のフロー
- 不正なファイル形式・サイズのテスト
- 権限チェックのテスト

### 3. セキュリティテスト

- パストラバーサル攻撃のテスト
- MIMEタイプ偽装のテスト
- 認可チェックのテスト

---

## 🚀 実装順序

1. **FileStorageService インターフェース** → 抽象化
2. **LocalFileStorageService** → ローカル実装
3. **Receipt エンティティ** → データモデル
4. **ReceiptMapper** → データベースアクセス
5. **ReceiptService** → ビジネスロジック
6. **ReceiptController** → REST API
7. **セキュリティ対策** → バリデーション・認可
8. **テスト** → 統合テスト

---

## 📈 将来の拡張

### Phase 2（中級）

- AWS S3への移行
- サムネイル生成（画像のプレビュー）
- OCR（領収書の文字認識）

### Phase 3（上級）

- ウイルススキャン（ClamAV）
- CDN配信（CloudFront）
- 画像の自動圧縮

---

## 🎓 学習ポイント

このプロジェクトで学べること：

✅ **マルチパートファイルアップロード** - Spring MVCの標準機能
✅ **ファイルシステム操作** - Java NIOの使い方
✅ **セキュリティベストプラクティス** - OWASP対策
✅ **抽象化設計** - インターフェースによる拡張性
✅ **統合テスト** - ファイル操作のテスト手法

---

**さあ、実装を始めましょう！** 🚀
