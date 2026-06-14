# CLAUDE.md — セッション引き継ぎ情報

このファイルを読めば、新しいチャットでも作業の文脈をそのまま引き継げます。

---

## プロジェクト概要

Spring Boot 製の**経費申請アプリ**を段階的に拡張する学習プロジェクト。
各フェーズで新技術を実装し、理解を定着させることが目的。

### 技術スタック
- Spring Boot 3.x / Java 21
- MyBatis（XML マッピング）
- MySQL 8
- Kafka（イベント駆動）
- Redis（WebSocket Pub/Sub）
- WebSocket（STOMP + SockJS）
- Spring Batch（CSV インポート / レポート生成）
- Prometheus + Grafana（Observability）

---

## ⚠️ 現在の状況（新規チャット開始時に必ず確認）

**複数の計画が並行している。** 新規チャットを開始したら必ずユーザーに確認すること：
- 「新機能フェーズ（Phase 3/4 Observability）を進めますか？」
- 「第2弾テーマ（Kafka/Redis/WebSocket 深化）を進めますか？」

| 計画 | 内容 | 現在地 |
|------|------|--------|
| **A：新機能フェーズ** | Phase 3 → Phase 4 と段階的に新技術を追加 | Phase 3 をユーザーが実装中。完了報告待ち |
| **B 第1弾：再学習テーマ** | 既存実装を整理＋再実装して知識定着 | ✅ **テーマ 1〜5（+3.5）すべて完了** |
| **B 第2弾：Kafka/Redis/WebSocket 深化** | 復習＋新機能追加のハイブリッド | テーマ 6（Kafka DLT）着手中 |

**各計画は独立している。** ユーザーの都合でいつでも切り替えてよい。

### 計画 A に戻る場合
- Phase 3 完了報告を受けたら `claude/phase4-custom-metrics` ブランチの内容を案内する
- Phase 4 の内容：`ExpenseMetrics`（Counter）+ `MetricsConfig`（@Timed AOP）の実装

### 計画 B 第2弾について
- 各テーマは **「既存フロー再読 → 説明 → 新機能追加」** の2段構成（ハイブリッド）
- テーマ 6 → 7 → 8 の順で進む
- ユーザーが「テーマ X を始めます」と言ったらブランチを作成する

---

## 計画 A：新機能フェーズの状況

| フェーズ | 内容 | ブランチ / 状態 |
|---------|------|----------------|
| Phase 1 | 基本 CRUD・Spring Batch・CSV | `main` にマージ済み |
| Phase 2 | WebSocket + Redis Pub/Sub + Kafka | `main` にマージ済み |
| Phase 3 | Prometheus + Grafana + Actuator | `feature/observability`（ユーザー実装中） |
| Phase 4 | カスタムメトリクス（Counter + @Timed） | `claude/phase4-custom-metrics`（Phase 3 完了後に案内） |

**ユーザーのベースブランチ：** `feature/observability`

### Phase 4 ブランチの内容（先行作成済み）
`claude/phase4-custom-metrics` に以下が実装済み：
- `metrics/ExpenseMetrics.java`：作成・提出・承認・却下の Counter を MeterRegistry に登録
- `config/MetricsConfig.java`：`@Timed` AOP を有効にする TimedAspect Bean
- `service/ExpenseService.java`：`@Timed` + `incrementXxx()` 追加
- Prometheus で確認：`expense_created_total`、`expense_submit_duration_seconds` など

---

## 計画 B 第1弾：再学習テーマの状況（すべて完了）

### テーマ一覧

| テーマ | 内容 | Claude ブランチ | 状態 |
|--------|------|----------------|------|
| テーマ 1 | MyBatis XML 整理・ページング方式の説明 | `claude/refactor-theme1-mybatis` | ✅ 完了・反映済み |
| テーマ 2 | Spring Events + Kafka Bridge の一本化 | `claude/refactor-theme2-events` | ✅ 完了・反映済み |
| テーマ 3 | Kafka Analytics Consumer の実装 | `claude/refactor-theme3-analytics` | ✅ 完了・反映済み |
| テーマ 3.5 | オフライン通知の DB 永続化（割込み） | `claude/feature-pending-notifications` | ✅ 完了・反映済み |
| テーマ 4 | MyBatis Cursor を使ったストリーミング | `claude/refactor-theme4-cursor` | ✅ 完了・反映済み |
| テーマ 5 | Redis + WebSocket 通知フローの整理 | `claude/refactor-theme5-redis-ws` | ✅ 完了・反映済み |

---

## 計画 B 第2弾：Kafka/Redis/WebSocket 深化テーマの状況

### 概要
WebSocket / Redis / Kafka は「なんとなく動いている」状態から脱するため、
**既存フローの再読・説明 → その上に新機能を1つ追加** のハイブリッド構成。

### テーマ一覧

| テーマ | 内容 | Claude ブランチ | 状態 |
|--------|------|----------------|------|
| テーマ 6 | Kafka 配信保証 + Dead Letter Topic | `claude/refactor-theme6-kafka-dlt` | 🔵 **着手中** |
| テーマ 7 | Redis キャッシュ（@Cacheable）の追加 | 未作成 | ⬜ 待機中 |
| テーマ 8 | WebSocket セキュリティ（STOMPインターセプター） | 未作成 | ⬜ 待機中 |

---

## 各テーマの詳細（第1弾）

### ✅ テーマ 1：MyBatis XML 整理（完了）
**ブランチ：** `claude/refactor-theme1-mybatis`

変更内容：
- `WHERE 1=1` → `<where>` タグに統一
- SQL 比較演算子を `<![CDATA[]]>` に統一（`&gt;=` との混在を解消）
- 未使用の `expenseSearchMap` resultMap を削除
- `filter` クエリの null チェック漏れを修正
- `findAllWithPaging`（Batch 順次処理）/ `findByIdRange`（Batch 並列処理）の用途をコメントで明記
- ORDER BY `${}` の安全性（`normalizedOrderBy()` による許可リスト検証）を説明

---

### ✅ テーマ 2：Spring Events + Kafka Bridge（完了）
**ブランチ：** `claude/refactor-theme2-events`

変更内容：
- `ExpenseService` の `KafkaProducer.publish()` 直接呼び出しを `eventPublisher.publishEvent()` に変更
- `ExpenseKafkaBridgeListener` を有効化し `@TransactionalEventListener(phase = AFTER_COMMIT)` に変更
- `application.properties` の `app.events.direct-listeners.enabled=false` を削除

**重要な学習ポイント：**
```
@EventListener         → トランザクション中に発火 → DBロールバックしてもKafkaに届く（不整合）
@TransactionalEventListener(AFTER_COMMIT)
                       → DBコミット確定後に発火 → DB と Kafka が常に整合する
```

---

### ✅ テーマ 3：Kafka Analytics Consumer の実装（完了）
**ブランチ：** `claude/refactor-theme3-analytics`

変更内容：
- `ExpenseKafkaAnalyticsConsumer` がログ出力のみ → Micrometer Counter を使ったメトリクス計測に実装
- `MeterRegistry` をコンストラクタ注入し、イベント種別ごとに Counter を登録：
  `expense.event.count{type=SUBMITTED / APPROVED / REJECTED}`

学習ポイント：
- Kafka の `groupId` が異なると同じメッセージを別々のコンシューマーが受け取れる
- Micrometer の `Counter.builder().tag()` による多次元メトリクス

---

### ✅ テーマ 3.5：オフライン通知の DB 永続化（割込み・完了）
**ブランチ：** `claude/feature-pending-notifications`

変更内容：
- `V17__create_pending_notifications.sql`：`pending_notifications` テーブル新規作成
- `PendingNotification` ドメイン + `PendingNotificationMapper`（insert / 未読取得 / 既読化）
- `PendingNotificationService`：保存・未読取得・一括既読
- `PendingNotificationController`：`GET /api/notifications/unread` / `PUT /api/notifications/read`
- `notification.js`：`loadPendingNotifications()` でページ読み込み時に未読を取得・既読化

設計判断：通知内容と未読管理を1テーブルにまとめた（特定1〜2名宛のためブロードキャストでない）

---

### ✅ テーマ 4：MyBatis Cursor（ストリーミング読み込み・完了）
**ブランチ：** `claude/refactor-theme4-cursor`

変更内容：
- `ExpenseMapper` に `Cursor<Expense> findAllAsStream()` を追加
- `ExpenseExportService.exportAllAsCsvStream(OutputStream)` を Cursor で逐次書き出し実装
- `ExportController` に `GET /api/exports/csv/expenses/stream` を追加

学習ポイント：
- `Cursor<T>` は `@Transactional(readOnly=true)` で SqlSession を開いたままにする必要がある
- MySQL は `fetchSize=Integer.MIN_VALUE` で行ごとフェッチになる

---

### ✅ テーマ 5：Redis + WebSocket 通知フローの整理（完了）
**ブランチ：** `claude/refactor-theme5-redis-ws`

変更内容：
- `RedisNotificationMessage`（冗長なラッパー）を削除、`NotificationMessage` に `destination` フィールドを追加
- Redis チャネル名を `application.properties` の `app.redis.ws-channel` に移動
- `RedisWebSocketSubscriber`：クライアント送信前に `destination` を null 化
- `notification.js`：再接続を指数バックオフに（1s → 2s → 4s → … → 最大 30s）

注意：`ChannelTopic(wsChannel)` を `"wsChannel"` とリテラルで書くと Publisher と不一致（実際に発生したバグ）

---

## 各テーマの詳細（第2弾）

### 🔵 テーマ 6：Kafka 配信保証 + Dead Letter Topic（着手中）
**ブランチ：** `claude/refactor-theme6-kafka-dlt`

#### 復習パート（読む・説明する）
- 既存の3つの Consumer（通知 / 分析 / 監査ログ）が別 `groupId` で同じメッセージを受け取る構造
- **現在の問題**：`ExpenseKafkaNotificationConsumer` が `try-catch` で例外を丸呑みしている
  → 例外が起きてもオフセットがコミットされ、メッセージは再処理されない
  → 通知がサイレント消滅する可能性がある

#### 新機能パート（書く）
変更内容：
- `pom.xml`：`spring-retry 2.0.11` を追加（`@RetryableTopic` の動作に必要）
- `ExpenseKafkaNotificationConsumer`：
  - `try-catch` を削除（例外を Spring Kafka に委ねる）
  - `@RetryableTopic(attempts="4", backOff=@BackOff(delay=1000, multiplier=2))` を追加
  - `@DltHandler` メソッドを追加（DLT に退避されたメッセージをエラーログ記録）

自動生成されるトピック：
```
expense-events-retry-0   （1回目リトライ、1秒後）
expense-events-retry-1   （2回目リトライ、2秒後）
expense-events-retry-2   （3回目リトライ、4秒後）
expense-events.DLT       （全リトライ失敗後の退避先）
```

学習ポイント：
- `@RetryableTopic` は **例外が投げられること** を前提に動く
  → `try-catch` で握りつぶすと DLT に届かない
- Spring Kafka 4.x では `@BackOff`（kafka 固有アノテーション）を使う
  （Spring Boot 3.x の `org.springframework.retry.annotation.Backoff` とは別物）
- 複数インスタンス時：各インスタンスが独立してリトライ → DLT に届く → 1つのDLT Consumer で集中監視

---

### ⬜ テーマ 7：Redis キャッシュ（@Cacheable）の追加
**ブランチ：** 未作成

#### 復習パート（読む・説明する）
- 既存の Redis Pub/Sub フロー（Publisher → チャネル → Subscriber → WebSocket）を再読
- 「なぜ複数インスタンスだと Redis Pub/Sub が必要なのか」を言語化
  → インスタンスA で承認 → インスタンスB に繋がっているユーザーへの WebSocket 転送

#### 新機能パート（書く）
- `@EnableCaching` + キャッシュ用 `RedisCacheManager` を Pub/Sub 用とは**別 Bean** で追加
- `ExpenseService.findById()` に `@Cacheable` / 更新・削除時に `@CacheEvict`
- TTL 設定（期限切れで自動削除）

学習ポイント：同じ Redis を「メッセージング（Pub/Sub）」と「キャッシュ」で使い分ける Bean 設計

---

### ⬜ テーマ 8：WebSocket セキュリティ（STOMP インターセプター）
**ブランチ：** 未作成

#### 復習パート（読む・説明する）
- 既存の `WebSocketConfig` / STOMP + SockJS 接続フローを再読
- `convertAndSendToUser` が email で個人に届く仕組み（`DefaultHandshakeHandler` + `UserDestinationResolver`）

#### 新機能パート（書く）
- `ChannelInterceptor` で CONNECT フレームを傍受し、未認証接続を切断
- `preSend()` でセッション認証チェック → `StompCommand.DISCONNECT` を返して拒否

学習ポイント：HTTP セキュリティと WebSocket セキュリティは**別レイヤー**（Spring Security の設定だけでは STOMP レベルで守れない）

---

## 進め方のルール

1. ユーザーが「テーマ X を始めます」と言う
2. Claude が `claude/refactor-themeX-*` ブランチを `feature/observability` から作成してプッシュ
3. ユーザーがコードを読んで理解し、自分のブランチで再実装
4. 完了したら次のテーマへ

**Claude が勝手に先へ進まない。** ユーザーの合図を待つ。

---

## よく使うコマンド

```bash
# ブランチを feature/observability から作成
git checkout -b claude/refactor-themeX-name origin/feature/observability

# テスト実行
./mvnw test -Dtest="ExpenseServiceTest,ExpenseServiceTestExercises"

# 全テスト
./mvnw test

# コンパイル確認
./mvnw compile -q
```
