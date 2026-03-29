# 🔄 バッチ処理（Spring Batch）設計書

## 🎯 目的

月末に経費データを集計し、レポートを自動生成・メール送信するバッチ処理を実装する。

---

## 📋 要件定義

### 機能要件

1. **月次経費集計バッチ**
   - 月末に自動実行（毎月1日 AM 0:00）
   - ステータス別集計（承認済み、提出済み、却下、下書き）
   - 合計金額、件数の集計
   - 前月データの集計

2. **レポート生成**
   - Excel形式のレポート生成
   - ファイルシステムへの保存
   - メール添付用ファイル作成

3. **メール通知**
   - 管理者へのレポートメール送信
   - 集計結果の要約を本文に記載
   - Excelファイルを添付

4. **スケジューリング**
   - Cron式でのスケジュール設定
   - 手動実行機能（REST API）
   - ジョブの並行実行制御

5. **エラーハンドリング**
   - リトライ戦略（3回まで）
   - スキップ戦略（エラー時の継続処理）
   - ジョブ履歴の記録

### 非機能要件

1. **パフォーマンス**
   - チャンク処理（100件ずつ処理）
   - メモリ効率的な処理

2. **信頼性**
   - トランザクション管理
   - ジョブの再実行可能性
   - エラー時のロールバック

3. **運用性**
   - ジョブ実行履歴の管理
   - ログ出力
   - 手動実行機能

---

## 🏗️ アーキテクチャ設計

### Spring Batchの基本構成

```
┌─────────────────────────────────────────────┐
│  JobScheduler (@Scheduled)                  │
│  - 毎月1日 AM 0:00に実行                    │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  MonthlyExpenseReportJob                    │
│  - Step 1: データ集計                       │
│  - Step 2: レポート生成                     │
│  - Step 3: メール送信                       │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  Step 1: データ集計                         │
│  ┌───────────────────────────────────────┐  │
│  │ ItemReader: ExpenseItemReader         │  │
│  │ - 前月のExpenseを読み込み              │  │
│  └───────────────────────────────────────┘  │
│              ↓                              │
│  ┌───────────────────────────────────────┐  │
│  │ ItemProcessor: ExpenseItemProcessor   │  │
│  │ - ステータス別集計                    │  │
│  │ - 合計金額・件数の計算                │  │
│  └───────────────────────────────────────┘  │
│              ↓                              │
│  ┌───────────────────────────────────────┐  │
│  │ ItemWriter: ExpenseItemWriter         │  │
│  │ - 集計結果を一時保存                  │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  Step 2: レポート生成                       │
│  - ExcelExportServiceを使用                 │
│  - ファイルシステムに保存                   │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  Step 3: メール送信                         │
│  - EmailServiceを使用                       │
│  - 管理者にメール送信                       │
│  - Excelファイルを添付                      │
└─────────────────────────────────────────────┘
```

---

## 📊 使用技術

### Spring Batch

- **バージョン**: 5.1.0（Spring Boot 4.0.1）
- **機能**:
  - ジョブ・ステップの管理
  - チャンク処理
  - トランザクション管理
  - リトライ・スキップ戦略
  - ジョブ履歴の管理

### Spring Scheduling

- **@Scheduled**: Cron式でのスケジューリング
- **@EnableScheduling**: スケジューリング有効化

---

## 📁 ファイル構成

### Javaファイル

```
src/main/java/com/example/expenses/
├── batch/
│   ├── config/
│   │   └── BatchConfiguration.java          # バッチ設定
│   ├── job/
│   │   └── MonthlyExpenseReportJob.java     # 月次レポートジョブ
│   ├── reader/
│   │   └── ExpenseItemReader.java           # ItemReader
│   ├── processor/
│   │   └── ExpenseItemProcessor.java        # ItemProcessor
│   ├── writer/
│   │   └── ExpenseItemWriter.java           # ItemWriter
│   ├── scheduler/
│   │   └── BatchScheduler.java              # スケジューラー
│   └── listener/
│       └── JobCompletionListener.java       # ジョブリスナー
├── dto/
│   └── batch/
│       └── MonthlyExpenseReport.java        # 月次レポートDTO
```

### データベーステーブル（Spring Batch自動生成）

```sql
-- ジョブ実行履歴
BATCH_JOB_INSTANCE
BATCH_JOB_EXECUTION
BATCH_JOB_EXECUTION_PARAMS
BATCH_JOB_EXECUTION_CONTEXT

-- ステップ実行履歴
BATCH_STEP_EXECUTION
BATCH_STEP_EXECUTION_CONTEXT
```

---

## 🔄 バッチ処理フロー

### 1. 月次経費集計バッチ

```
1. 前月のExpenseデータを取得
   ├─ 期間: 前月1日 00:00 ~ 前月末日 23:59
   └─ 全ステータス対象

2. ステータス別集計
   ├─ 承認済み: 件数、合計金額
   ├─ 提出済み: 件数、合計金額
   ├─ 却下: 件数、合計金額
   └─ 下書き: 件数、合計金額

3. Excelレポート生成
   ├─ ファイル名: monthly_report_YYYYMM.xlsx
   └─ 保存先: /data/reports/

4. メール送信
   ├─ 宛先: 管理者
   ├─ 件名: 【経費管理】月次レポート（YYYY年MM月）
   ├─ 本文: 集計結果の要約
   └─ 添付: Excelファイル
```

---

## ⚙️ 設定

### application.properties

```properties
# Spring Batch設定
spring.batch.jdbc.initialize-schema=always
spring.batch.job.enabled=false  # 自動実行を無効化（スケジューラーから実行）

# バッチスケジュール（毎月1日 AM 0:00）
batch.schedule.monthly-report=0 0 0 1 * ?

# レポート保存先
batch.report.output-dir=/data/reports/

# メール通知先（管理者）
batch.notification.admin-email=admin@example.com
```

---

## 📊 月次レポートExcel

### フォーマット

```
┌───────────────────────────────────────────────────────┐
│ 月次経費レポート - 2026年3月                          │
│ 集計期間: 2026-03-01 ~ 2026-03-31                     │
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

【前月との比較】
- 件数: 300件（前月比 +10件、+3.4%）
- 金額: ¥8,000,000（前月比 +500,000円、+6.7%）
```

---

## 🛡️ エラーハンドリング

### リトライ戦略

```java
@Bean
public Step expenseAggregationStep() {
    return stepBuilderFactory.get("expenseAggregationStep")
        .<Expense, MonthlyExpenseReport>chunk(100)
        .reader(expenseItemReader())
        .processor(expenseItemProcessor())
        .writer(expenseItemWriter())
        .faultTolerant()  // エラー許容設定
        .retry(Exception.class)  // リトライ対象例外
        .retryLimit(3)  // 最大3回リトライ
        .skip(ValidationException.class)  // スキップ対象例外
        .skipLimit(10)  // 最大10件スキップ
        .build();
}
```

### ジョブリスナー

```java
@Component
public class JobCompletionListener extends JobExecutionListenerSupport {
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            logger.info("ジョブ完了: {}", jobExecution.getJobInstance().getJobName());
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            logger.error("ジョブ失敗: {}", jobExecution.getAllFailureExceptions());
        }
    }
}
```

---

## 🎯 手動実行API

### REST APIエンドポイント

```
POST /api/batch/jobs/monthly-report
```

**リクエスト例:**
```bash
curl -X POST http://localhost:8080/api/batch/jobs/monthly-report \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**レスポンス例:**
```json
{
  "jobExecutionId": 12345,
  "status": "STARTED",
  "startTime": "2026-03-07T10:00:00",
  "message": "月次レポートジョブを開始しました"
}
```

---

## 📈 将来の拡張

### Phase 2（中級）

- 週次レポート
- 日次レポート
- カスタムレポート（条件指定）

### Phase 3（上級）

- 並列処理（パーティショニング）
- リモート実行（Remote Chunking）
- クラスタリング対応

---

## 🎓 学習ポイント

このプロジェクトで学べること：

✅ **Spring Batch** - ジョブ・ステップの設計
✅ **チャンク処理** - 大量データの効率的な処理
✅ **スケジューリング** - @Scheduled、Cron式
✅ **リトライ・スキップ戦略** - エラーハンドリング
✅ **トランザクション管理** - データ整合性の確保
✅ **ジョブ履歴管理** - 実行履歴の追跡

---

**さあ、実装を始めましょう！** 🚀
