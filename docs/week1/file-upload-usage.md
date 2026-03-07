# 📦 ファイルアップロード機能 使い方ガイド

## 🎯 概要

経費申請に領収書画像をアップロードする機能です。

---

## 🚀 セットアップ手順

### 1. データベーステーブルの作成

```bash
mysql -u app -p newschema < docs/week1/receipts-table.sql
```

または、直接SQLを実行：

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. アプリケーションの起動

```bash
./mvnw spring-boot:run
```

---

## 📡 API 使用例

### 1. 領収書のアップロード

**リクエスト:**

```bash
curl -X POST "http://localhost:8080/api/expenses/1/receipts" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/receipt.jpg"
```

**レスポンス:**

```json
{
  "id": 1,
  "expenseId": 1,
  "originalFilename": "receipt.jpg",
  "fileSize": 1024000,
  "contentType": "image/jpeg",
  "uploadedAt": "2026-03-07T10:30:00"
}
```

---

### 2. 領収書一覧の取得

**リクエスト:**

```bash
curl -X GET "http://localhost:8080/api/expenses/1/receipts" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**レスポンス:**

```json
{
  "receipts": [
    {
      "id": 1,
      "originalFilename": "receipt1.jpg",
      "fileSize": 1024000,
      "contentType": "image/jpeg",
      "uploadedAt": "2026-03-07T10:30:00"
    },
    {
      "id": 2,
      "originalFilename": "receipt2.pdf",
      "fileSize": 2048000,
      "contentType": "application/pdf",
      "uploadedAt": "2026-03-07T11:00:00"
    }
  ],
  "count": 2
}
```

---

### 3. 領収書のダウンロード

**リクエスト:**

```bash
curl -X GET "http://localhost:8080/api/receipts/1/download" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o receipt.jpg
```

---

### 4. 領収書の削除

**リクエスト:**

```bash
curl -X DELETE "http://localhost:8080/api/receipts/1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**レスポンス:**

```json
{
  "message": "領収書を削除しました"
}
```

---

## 🧪 テストの実行

### 統合テストの実行

```bash
./mvnw test -Dtest=ReceiptControllerTest
```

---

## 🔐 セキュリティ機能

### 1. ファイル形式の制限

**許可されるファイル:**
- JPEG（.jpg, .jpeg）
- PNG（.png）
- PDF（.pdf）

**拒否される例:**
- 実行ファイル（.exe, .sh）
- スクリプト（.js, .py）
- その他の形式

### 2. ファイルサイズの制限

- 最大ファイルサイズ: **10MB**
- 最大リクエストサイズ: **50MB**（複数ファイル対応）

### 3. パストラバーサル攻撃防止

**攻撃例:**
```bash
# 攻撃: ファイル名に "../" を含める
curl -F "file=@../../etc/passwd"
```

**防御:**
- ファイル名をサニタイズ
- UUIDでファイル名を生成
- ルートディレクトリ外へのアクセスをチェック

### 4. MIMEタイプのチェック

**攻撃例:**
```bash
# .exeファイルを.jpgにリネームしてアップロード
mv malware.exe fake-receipt.jpg
```

**防御:**
- 拡張子とMIMEタイプの両方をチェック
- Content-Typeヘッダーを検証

---

## 📁 ファイルの保存場所

### ローカルストレージ

```
./uploads/
  ├── 2026/
  │   ├── 03/
  │   │   ├── 07/
  │   │   │   ├── a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
  │   │   │   ├── b2c3d4e5-f6a7-8901-bcde-f12345678901.pdf
```

### 設定の変更

`application.properties`:

```properties
# アップロードディレクトリ
file.upload-dir=./uploads

# ファイルサイズ制限
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

---

## 🎓 学習ポイント

### 実装した技術

1. **Spring MVC**
   - マルチパートファイルアップロード
   - ファイルダウンロード
   - RESTful API設計

2. **Java NIO**
   - ファイルシステム操作
   - ストリーミング処理
   - パス操作

3. **セキュリティ**
   - ファイルバリデーション
   - パストラバーサル攻撃防止
   - MIMEタイプチェック

4. **アーキテクチャ**
   - インターフェースによる抽象化
   - レイヤー分離
   - 依存性の注入

5. **テスト**
   - MockMvcを使った統合テスト
   - ファイルアップロードのテスト

---

## 🚀 次のステップ

### Phase 2: AWS S3への移行

1. **S3FileStorageService の実装**

```java
@Service
@Profile("production")
public class S3FileStorageService implements FileStorageService {
    // AWS S3を使った実装
}
```

2. **設定の追加**

```properties
# AWS S3設定
aws.s3.bucket-name=my-expense-bucket
aws.s3.region=ap-northeast-1
```

### Phase 3: 高度な機能

- **サムネイル生成** - 画像のプレビュー表示
- **OCR** - 領収書の文字認識
- **ウイルススキャン** - ClamAVによるスキャン
- **CDN配信** - CloudFrontでの配信

---

## ❓ トラブルシューティング

### エラー: "ファイルのアップロードに失敗しました"

**原因:**
- アップロードディレクトリの権限がない
- ディスク容量不足

**解決策:**

```bash
# ディレクトリの作成と権限付与
mkdir -p ./uploads
chmod 755 ./uploads
```

### エラー: "サポートされていないファイル形式です"

**原因:**
- 許可されていないファイル形式

**解決策:**
- JPG, PNG, PDF のみアップロード可能

### エラー: "ファイルサイズが大きすぎます"

**原因:**
- ファイルサイズが10MBを超えている

**解決策:**
- ファイルサイズを10MB以下に圧縮

---

## 🎉 完了！

これで、大企業レベルのファイルアップロード機能が実装できました！

**次に学ぶべきこと:**
1. AWS S3への移行
2. メッセージングシステム（Kafka）
3. 可観測性（分散トレーシング）

**おめでとうございます！** 🎊
