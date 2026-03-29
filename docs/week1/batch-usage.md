# 🔄 バッチ処理（Spring Batch）使い方ガイド

## 🎯 概要

月末に経費データを自動集計し、Excelレポートを生成してメール送信するバッチ処理です。

---

## 🚀 使い方

### 1. 自動実行（スケジューリング）

#### **実行タイミング**
- 毎月1日 AM 0:00に自動実行
- Cron式: `0 0 0 1 * ?`

#### **設定変更**
`application.properties`で変更可能:
```properties
# 毎月1日 AM 0:00（デフォルト）
batch.schedule.monthly-report=0 0 0 1 * ?

# 例: 毎日 AM 3:00に実行
batch.schedule.monthly-report=0 0 3 * * ?

# 例: 毎週月曜日 AM 9:00に実行
batch.schedule.monthly-report=0 0 9 ? * MON
```

#### **Cron式の説明**
```
秒 分 時 日 月 曜日
0  0  0  1  *  ?
│  │  │  │  │  └─ 曜日（? = 指定なし）
│  │  │  │  └──── 月（* = 毎月）
│  │  │  └─────── 日（1 = 1日）
│  │  └────────── 時（0 = 0時）
│  └───────────── 分（0 = 0分）
└──────────────── 秒（0 = 0秒）
```

---

### 2. 手動実行（REST API）

#### **エンドポイント**
```
POST /api/batch/jobs/monthly-report
```

#### **リクエスト例（curl）**
```bash
curl -X POST http://localhost:8080/api/batch/jobs/monthly-report \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### **レスポンス例（成功）**
```json
{
  "jobExecutionId": 12345,
  "status": "COMPLETED",
  "startTime": "2026-03-07T10:00:00",
  "endTime": "2026-03-07T10:00:30",
  "exitCode": "COMPLETED",
  "message": "月次レポートジョブが正常に完了しました"
}
```

#### **レスポンス例（失敗）**
```json
{
  "jobExecutionId": 12346,
  "status": "FAILED",
  "startTime": "2026-03-07T10:05:00",
  "endTime": "2026-03-07T10:05:15",
  "exitCode": "FAILED",
  "message": "月次レポートジョブが失敗しました"
}
```

---

## 📊 処理フロー

### Step 1: データ集計

```
1. 前月の期間を計算
   └─ 例: 2026年2月 → 2026-02-01 00:00 ~ 2026-02-28 23:59

2. 前月のExpenseデータを取得
   └─ SQL: SELECT * FROM expenses WHERE created_at BETWEEN ? AND ?

3. ステータス別に集計
   ├─ 承認済み（APPROVED）: 件数、合計金額
   ├─ 提出済み（SUBMITTED）: 件数、合計金額
   ├─ 却下（REJECTED）: 件数、合計金額
   └─ 下書き（DRAFT）: 件数、合計金額

4. 割合を計算
   └─ 各ステータスの金額 ÷ 合計金額 × 100
```

### Step 2: レポート生成

```
1. Excelファイルを生成
   └─ ファイル名: monthly_report_YYYYMM.xlsx

2. シート構成
   ├─ タイトル行: 月次経費レポート - YYYY年MM月
   ├─ ヘッダー行: ステータス、件数、合計金額、割合
   ├─ データ行: 各ステータスの集計結果
   └─ 合計行: 全体の件数、合計金額

3. ファイル保存
   └─ 保存先: ./data/reports/monthly_report_YYYYMM.xlsx
```

### Step 3: メール送信

```
1. メール作成
   ├─ 宛先: admin@example.com（設定可能）
   ├─ 件名: 【経費管理】月次レポート（YYYY年MM月）
   ├─ 本文: HTMLメール（集計結果のテーブル表示）
   └─ 添付: monthly_report_YYYYMM.xlsx

2. メール送信
   └─ SMTPサーバー経由で送信
```

---

## 📁 生成されるファイル

### Excelレポート

**ファイル名:** `monthly_report_202602.xlsx`

**保存先:** `./data/reports/`

**内容例:**
```
┌───────────────────────────────────────────────────────┐
│ 月次経費レポート - 2026年2月                          │
├───────────────────────────────────────────────────────┤
│ ステータス別集計                                      │
├─────────────┬──────┬────────────┬─────────────────┤
│ ステータス   │ 件数 │ 合計金額   │ 割合             │
├─────────────┼──────┼────────────┼─────────────────┤
│ 承認済み     │  150 │ ¥5,000,000 │ 62.5%            │
│ 提出済み     │   80 │ ¥2,000,000 │ 25.0%            │
│ 却下         │   20 │   ¥500,000 │  6.3%            │
│ 下書き       │   50 │   ¥500,000 │  6.3%            │
├─────────────┼──────┼────────────┼─────────────────┤
│ 合計         │  300 │ ¥8,000,000 │ 100.0%           │
└─────────────┴──────┴────────────┴─────────────────┘
```

---

## ⚙️ 設定

### application.properties

```properties
# Spring Batch設定
spring.batch.jdbc.initialize-schema=always  # バッチテーブルの自動作成
spring.batch.job.enabled=false              # 自動実行を無効化（スケジューラーから実行）

# スケジュール設定
batch.schedule.monthly-report=0 0 0 1 * ?   # 毎月1日 AM 0:00

# レポート保存先
batch.report.output-dir=./data/reports/     # 出力ディレクトリ

# メール通知先
batch.notification.admin-email=admin@example.com
```

---

## 📊 ジョブ履歴の確認

### データベーステーブル

Spring Batchは自動的に履歴テーブルを作成します：

```sql
-- ジョブ実行履歴の確認
SELECT
    je.JOB_EXECUTION_ID,
    ji.JOB_NAME,
    je.STATUS,
    je.START_TIME,
    je.END_TIME,
    je.EXIT_CODE
FROM BATCH_JOB_EXECUTION je
JOIN BATCH_JOB_INSTANCE ji ON je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
ORDER BY je.START_TIME DESC;
```

**結果例:**
```
┌────────────────┬──────────────────────────┬───────────┬─────────────────────┬─────────────────────┬───────────┐
│ JOB_EXECUTION_ID│ JOB_NAME                 │ STATUS    │ START_TIME          │ END_TIME            │ EXIT_CODE │
├────────────────┼──────────────────────────┼───────────┼─────────────────────┼─────────────────────┼───────────┤
│ 1              │ monthlyExpenseReportJob  │ COMPLETED │ 2026-03-01 00:00:00 │ 2026-03-01 00:00:30 │ COMPLETED │
│ 2              │ monthlyExpenseReportJob  │ COMPLETED │ 2026-02-01 00:00:00 │ 2026-02-01 00:00:25 │ COMPLETED │
└────────────────┴──────────────────────────┴───────────┴─────────────────────┴─────────────────────┴───────────┘
```

---

## 🛡️ エラーハンドリング

### エラー発生時の動作

1. **ログ出力**
   ```
   ERROR [BatchScheduler] 月次経費レポートジョブのスケジュール実行エラー
   ```

2. **ジョブステータス**
   - ステータスが `FAILED` になる
   - データベースの `BATCH_JOB_EXECUTION` テーブルに記録

3. **メール送信**
   - エラー時はメールは送信されない
   - ログを確認して原因を調査

### よくあるエラーと対処法

#### **エラー1: データベース接続エラー**
```
Could not open JDBC Connection for transaction
```
**対処法:**
- データベースが起動しているか確認
- 接続情報（URL、ユーザー名、パスワード）を確認

#### **エラー2: メール送信エラー**
```
MailSendException: Could not connect to SMTP host
```
**対処法:**
- SMTPサーバーが起動しているか確認（MailHog等）
- メール設定を確認（`spring.mail.*`）

#### **エラー3: ファイル書き込みエラー**
```
FileNotFoundException: ./data/reports/ (No such file or directory)
```
**対処法:**
- 出力ディレクトリを作成
```bash
mkdir -p ./data/reports/
```

---

## 🎓 学習ポイント

このプロジェクトで学べること：

✅ **Spring Batch** - ジョブ・ステップの設計
✅ **タスクレット方式** - シンプルなバッチ処理
✅ **スケジューリング** - @Scheduled、Cron式
✅ **データ集計** - Stream API、GROUP BY
✅ **Excelレポート生成** - Apache POI
✅ **メール送信** - JavaMailSender、ファイル添付
✅ **ジョブ履歴管理** - Spring Batchの履歴テーブル

---

## 🚀 次のステップ（Phase 2）

### 中級機能

1. **週次レポート**
   - 毎週月曜日にレポート生成
   - 先週のデータを集計

2. **日次レポート**
   - 毎日AM 9:00にレポート生成
   - 前日のデータを集計

3. **カスタムレポート**
   - 条件指定でレポート生成
   - REST APIで期間指定

### 上級機能

1. **チャンク処理**
   - ItemReader/ItemProcessor/ItemWriterの実装
   - 大量データの効率的な処理

2. **並列処理**
   - パーティショニング
   - マルチスレッド処理

3. **リトライ・スキップ戦略**
   - エラー時の自動リトライ
   - スキップ可能なエラーの設定

---

## 🎉 完成！

これで、大企業レベルのバッチ処理機能が実装できました！

**実装した機能:**
- ✅ 月次経費集計バッチ
- ✅ Excelレポート生成
- ✅ メール通知
- ✅ 自動スケジューリング（毎月1日 AM 0:00）
- ✅ 手動実行API
- ✅ ジョブ履歴管理

**習得したスキル:**
- ✅ Spring Batch（ジョブ・ステップ）
- ✅ タスクレット方式
- ✅ スケジューリング（@Scheduled）
- ✅ データ集計処理
- ✅ Excelレポート生成
- ✅ メール送信（ファイル添付）

**おめでとうございます！** 🎊
