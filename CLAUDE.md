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

**2つの計画が並行している。** 新規チャットを開始したら必ずユーザーに確認すること：
- 「新機能フェーズ（Phase 3/4 Observability）を進めますか？」
- 「再学習テーマはすべて完了済み。新しい学習テーマを追加しますか？」

| 計画 | 内容 | 現在地 |
|------|------|--------|
| **A：新機能フェーズ** | Phase 3 → Phase 4 と段階的に新技術を追加 | Phase 3 をユーザーが実装中。完了報告待ち |
| **B 第1弾：再学習テーマ** | 既存実装を整理＋再実装して知識定着 | ✅ **テーマ 1〜5（+3.5）すべて完了** |
| **B 第2弾：Kafka/Redis/WS 深化** | 復習＋新機能ハイブリッド | ✅ **テーマ 6・7・8 すべて完了** |

**計画 A と B は独立している。** ユーザーの都合でいつでも切り替えてよい。

### 計画 A に戻る場合
- Phase 3 完了報告を受けたら `claude/phase4-custom-metrics` ブランチの内容を案内する
- Phase 4 の内容：`ExpenseMetrics`（Counter）+ `MetricsConfig`（@Timed AOP）の実装

### 計画 B について
- テーマ 1〜5 と割込みのテーマ 3.5 はすべて完了・ユーザー反映済み
- 第2弾（テーマ 6〜8）はすべて完了・ユーザー反映済み

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

## 計画 B：再学習・繰り返し実装テーマの状況

### 背景
実装は進んでいるが「なんとなくわかっている」状態を解消するため、
**整理（コードを読める状態）＋ 実装（自分で書く）** を繰り返す計画。

Claude が完成版ブランチを作成 → ユーザーが読んで理解 → 自分のブランチで再実装。

### テーマ一覧

| テーマ | 内容 | Claude ブランチ | 状態 |
|--------|------|----------------|------|
| テーマ 1 | MyBatis XML 整理・ページング方式の説明 | `claude/refactor-theme1-mybatis` | ✅ 完了・反映済み |
| テーマ 2 | Spring Events + Kafka Bridge の一本化 | `claude/refactor-theme2-events` | ✅ 完了・反映済み |
| テーマ 3 | Kafka Analytics Consumer の実装 | `claude/refactor-theme3-analytics` | ✅ 完了・反映済み |
| テーマ 3.5 | オフライン通知の DB 永続化（割込み） | `claude/feature-pending-notifications` | ✅ 完了・反映済み |
| テーマ 4 | MyBatis Cursor を使ったストリーミング | `claude/refactor-theme4-cursor` | ✅ 完了・反映済み |
| テーマ 5 | Redis + WebSocket 通知フローの整理 | `claude/refactor-theme5-redis-ws` | ✅ 完了・反映済み |

### B 第2弾：Kafka/Redis/WebSocket 深化テーマ一覧

| テーマ | 内容 | Claude ブランチ（模範実装） | 状態 |
|--------|------|--------------------------|------|
| テーマ 6 | Kafka Dead Letter Topic（`@RetryableTopic` + `@DltHandler`） | `claude/refactor-theme6-kafka-dlt` | ✅ 完了・反映済み |
| テーマ 7 | Redis キャッシュ（`@Cacheable` / `@CacheEvict` / `RedisCacheManager`） | `claude/refactor-theme7-redis-cache` | ✅ 完了・反映済み |
| テーマ 8 | WebSocket セキュリティ（STOMP `ChannelInterceptor`） | `claude/refactor-theme8-ws-security` | ✅ 完了・反映済み |

---

## 各テーマの詳細

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

**変更後のフロー：**
```
ExpenseService.submit() [@Transactional]
  └─ publishEvent(new ExpenseSubmittedEvent(...))
       ↓ コミット後
  ExpenseKafkaBridgeListener.onSubmitted() [@TransactionalEventListener(AFTER_COMMIT)]
       └─ KafkaProducer.publish(SUBMITTED)
            ↓
         Kafka "expense-events" トピック
            ↓
    各 Consumer（通知 / 分析 / 監査ログ）
```

---

### ✅ テーマ 3：Kafka Analytics Consumer の実装（完了）
**ブランチ：** `claude/refactor-theme3-analytics`

変更内容：
- `ExpenseKafkaAnalyticsConsumer` がログ出力のみ → Micrometer Counter を使ったメトリクス計測に実装
- `MeterRegistry` をコンストラクタ注入し、イベント種別ごとに Counter を登録：
  `expense.event.count{type=SUBMITTED / APPROVED / REJECTED}`
- イベント受信のたびに `counter.increment()` を呼ぶ
- `/actuator/prometheus` でイベント受信件数を確認できる

学習ポイント：
- Kafka の `groupId` が異なると同じメッセージを別々のコンシューマーが受け取れる
- Micrometer の `Counter.builder().tag()` による多次元メトリクス

---

### ✅ テーマ 3.5：オフライン通知の DB 永続化（割込み・完了）
**ブランチ：** `claude/feature-pending-notifications`

背景：ユーザーがログアウト中に発生した通知を取りこぼさないよう、
DB に保存しておきログイン時に表示する。

変更内容：
- `V17__create_pending_notifications.sql`：`pending_notifications` テーブル新規作成
  （`user_id` / `type` / `expense_id` / `message` / `title` / `amount` / `is_read` / `created_at`）
- `PendingNotification` ドメイン + `PendingNotificationMapper`（insert / 未読取得 / 既読化）
- `PendingNotificationService`：保存・未読取得・一括既読
- `PendingNotificationController`：
  `GET /api/notifications/unread`（未読取得）/ `PUT /api/notifications/read`（既読化）
- `ExpenseKafkaNotificationConsumer`：Kafka イベント受信時に常に DB へ保存
  （SUBMITTED は `UserMapper.findAnyApproverId()` で承認者を特定）
- `notification.js`：ページ読み込み時に `loadPendingNotifications()` で未読を取得・表示し既読化

設計判断：
- 通知内容と未読管理を**1テーブルにまとめた**（テーブル分割しない）
  → 通知は特定の 1〜2 名宛で、ブロードキャストではないため

---

### ✅ テーマ 4：MyBatis Cursor（ストリーミング読み込み・完了）
**ブランチ：** `claude/refactor-theme4-cursor`

変更内容：
- `ExpenseMapper` に `Cursor<Expense> findAllAsStream()` を追加
- `ExpenseMapper.xml` に対応 SELECT を追加（MySQL Streaming 用に `fetchSize="-2147483648"`）
- `ExpenseExportService.exportAllAsCsvStream(OutputStream)` を Cursor で逐次書き出し実装
- `ExportController` に `GET /api/exports/csv/expenses/stream` を追加
- `findAllWithPaging` の `applicant}_id` タイポを修正

学習ポイント：
- `Cursor<T>` は `@Transactional(readOnly=true)` で SqlSession を開いたままにする必要がある
- `try (Cursor<Expense> cursor = ...)` で必ずクローズ（コネクションプール枯渇防止）
- MySQL は `fetchSize=Integer.MIN_VALUE` で行ごとフェッチ（ストリーミング）になる

---

### ✅ テーマ 5：Redis + WebSocket 通知フローの整理（完了）
**ブランチ：** `claude/refactor-theme5-redis-ws`

変更内容：
- `RedisNotificationMessage`（冗長なラッパー）を**削除**、`NotificationMessage` に `destination` フィールドを追加
- Redis チャネル名 `"ws-notifications"` を `application.properties` の `app.redis.ws-channel` に移動
- `RedisConfig` / `RedisWebSocketPublisher` を `@Value` でチャネル名取得・`NotificationMessage` 型に統一
- `RedisWebSocketSubscriber`：クライアント送信前に `destination` を null 化
  （`@JsonInclude(NON_NULL)` と組み合わせ、内部ルーティング情報をブラウザに漏らさない）
- `NotificationWebSocketController` の未使用メソッドを削除
- `notification.js`：再接続を指数バックオフに（1s → 2s → 4s → … → 最大 30s、接続成功でリセット）

学習ポイント：
- Redis Pub/Sub の「Publisher → チャネル → Subscriber → SimpMessagingTemplate → WebSocket」の流れ
- `convertAndSend`（/topic ブロードキャスト）と `convertAndSendToUser`（/queue 個人宛）の違い
- 注意：`RedisConfig` の `ChannelTopic(wsChannel)` を文字列リテラル `"wsChannel"` と書くと
  Publisher のチャネルと不一致になり通知が届かなくなる（実際に発生したバグ）

---

### ✅ テーマ 6：Kafka Dead Letter Topic（完了）
**ブランチ：** `claude/refactor-theme6-kafka-dlt`

変更内容：
- `pom.xml`：`spring-retry 2.0.11` を追加（`@RetryableTopic` の依存）
- `ExpenseKafkaNotificationConsumer`：
  - `consume()` の例外を飲み込む `try-catch` を削除（例外が伝播しないと DLT にルーティングされない）
  - `@RetryableTopic` を追加：4回試行、1s → 2s → 4s → 8s のバックオフ、`SUFFIX_WITH_INDEX_VALUE` でトピック名自動生成
  - `@DltHandler` を追加：最終失敗時に `[DLT]` ログを出力して手動確認を促す

学習ポイント：
- `@BackOff` は `org.springframework.kafka.annotation.BackOff`（spring-retry の `@Backoff` ではない）
- 例外を `catch` して再スローしないと DLT にルーティングされない
- 自動生成トピック：`expense-events-retry-0`, `-retry-1`, `-retry-2`, `.DLT`
- `groupId` が同じなら Partition を分散処理、異なれば同じメッセージをそれぞれ独立して受け取る

---

### ✅ テーマ 7：Redis キャッシュ（完了）
**ブランチ：** `claude/refactor-theme7-redis-cache`

変更内容：
- `CacheConfig.java`（新規）：`@EnableCaching` + `RedisCacheManager` Bean を定義
- `ExpenseService.java`：
  - `getExpense()` に `@Cacheable(cacheNames = "expenses", key = "#expenseId")` を追加
  - `submit()` / `approve()` / `reject()` に `@CacheEvict(cacheNames = "expenses", key = "#expenseId")` を追加

学習ポイント：
- **`GenericJacksonJsonRedisSerializer`（Jackson 3）を使う**
  - `GenericJackson2JsonRedisSerializer` は Spring Boot 4.x で deprecated → `LocalDateTime` でランタイムエラー
- **`enableDefaultTyping(typeValidator)` 必須**
  - ないと `LinkedHashMap` が返り `ClassCastException`（@class 型情報が JSON に埋め込まれないため）
  - `PolymorphicTypeValidator` で `com.example.expenses.*` と `java.*` に限定（セキュリティ）
- **`transactionAware()` 必須**
  - ないと `@CacheEvict`（beforeInvocation=false）がコミット前に実行され、
    直後の別スレッドが古いデータを再キャッシュするレースコンディションが発生する
- **`@EnableCaching` 必須**
  - ないと `@Cacheable` / `@CacheEvict` が Spring AOP に無視される

---

### ✅ テーマ 8：WebSocket セキュリティ（完了）
**ブランチ：** `claude/refactor-theme8-ws-security`

変更内容：
- `StompAuthChannelInterceptor.java`（新規）：`ChannelInterceptor` を実装し `preSend()` で STOMP フレームを検査
  - `CONNECT`：`accessor.getUser()` が null（未ログイン）なら例外で接続を拒否
  - `SUBSCRIBE /topic/**`：`ROLE_APPROVER` を持つユーザーのみ許可。一般ユーザーは拒否
  - `SUBSCRIBE /user/**`：Spring が Principal 名でルーティングするので追加チェック不要（素通し）
  - `command == null`（ハートビート等）：そのまま通す
- `WebSocketConfig.java`：`configureClientInboundChannel()` をオーバーライドしてインターセプター登録

学習ポイント：
- **なぜ HTTP Security だけでは足りないか**
  - `authorizeHttpRequests` は HTTP リクエストにしか効かない
  - WebSocket 確立後の STOMP フレーム（CONNECT/SUBSCRIBE/SEND）は HTTP ではないため素通しになる
- **Principal の出所**
  - ハンドシェイク時に HttpSession の `Authentication` が WebSocket セッションの Principal として自動引き継ぎ
  - `accessor.getUser()` を `Authentication` にキャストして `getAuthorities()` で権限チェックできる
- **`preSend` で例外を投げると**
  - フレームがブローカーに到達せずクライアントに STOMP ERROR フレームが返る
- **Inbound vs Outbound**
  - `configureClientInboundChannel`：クライアント → サーバーの入口（今回はこれ）
  - `configureClientOutboundChannel`：サーバー → クライアントの出口（送信を絞りたい場合）
- **設計判断：`/topic` は承認者のみ**
  - 提出通知は承認者向けブロードキャストなので、一般申請者は `/user/queue` で十分
  - 一般申請者が `/topic/notifications` を購読しようとすると InterceptorがERRORを返す

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
