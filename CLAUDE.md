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

## フェーズの実装状況

| フェーズ | 内容 | ブランチ / 状態 |
|---------|------|----------------|
| Phase 1 | 基本 CRUD・Spring Batch・CSV | `main` にマージ済み |
| Phase 2 | WebSocket + Redis Pub/Sub + Kafka | `main` にマージ済み |
| Phase 3 | Prometheus + Grafana + Actuator | `feature/observability`（ユーザー実装中） |
| Phase 4 | カスタムメトリクス（Counter + @Timed） | `claude/phase4-custom-metrics`（Phase 3 完了後に案内） |

**ユーザーのベースブランチ：** `feature/observability`
**Phase 3 完了後の次ステップ：** `claude/phase4-custom-metrics` の内容を案内する

---

## 再学習・繰り返し実装計画

### 背景と方針
実装は進んでいるが「なんとなくわかっている」状態。
「自分で書ける・説明できる」レベルにするため、
**整理（コードを読める状態）＋ 実装（自分で書く）** を繰り返す。

Claude は完成版ブランチを作成 → ユーザーが読んで理解 → 自分のブランチで再実装。

### テーマ一覧

| テーマ | 内容 | Claude ブランチ | 状態 |
|--------|------|----------------|------|
| テーマ 1 | MyBatis XML 整理・ページング方式の説明 | `claude/refactor-theme1-mybatis` | ✅ 完了・ユーザー反映済み |
| テーマ 2 | Spring Events + Kafka Bridge の一本化 | `claude/refactor-theme2-events` | ✅ 完了・ユーザー確認中 |
| テーマ 3 | Kafka Analytics Consumer の実装 | 未作成 | ⬜ 次 |
| テーマ 4 | MyBatis Cursor を使ったストリーミング | 未作成 | ⬜ 待機中 |
| テーマ 5 | Redis + WebSocket 通知フローの整理 | 未作成 | ⬜ 待機中 |

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

テスト確認コマンド：
```bash
./mvnw test -Dtest="ExpenseServiceTest,ExpenseServiceTestExercises"
```

---

### ⬜ テーマ 3：Kafka Analytics Consumer の実装（次のテーマ）
**ブランチ：** `claude/refactor-theme3-analytics`（未作成）

予定内容：
- `ExpenseKafkaAnalyticsConsumer` がログ出力のみ → Micrometer Counter を使ったメトリクス計測に実装
- イベント種別ごとのカウンターを登録：`expense.event.count{type=SUBMITTED}` 等
- `/actuator/prometheus` でイベント受信件数を確認できるようにする

学習ポイント：
- Kafka の `groupId` が異なると同じメッセージを別々のコンシューマーが受け取れる仕組みを体感
- Micrometer の `Counter.builder().tag()` による多次元メトリクス

---

### ⬜ テーマ 4：MyBatis Cursor（ストリーミング読み込み）
**ブランチ：** `claude/refactor-theme4-cursor`（未作成）

予定内容：
- `ExpenseMapper` に `Cursor<Expense> findAllAsStream()` を追加
- `ExpenseExportService` に Cursor を使った逐次 CSV 書き出しを実装
- `List<Expense>` との比較（全件メモリロード vs ストリーミング）

学習ポイント：
- `try (Cursor<Expense> cursor = ...)` で必ずクローズする理由
- `@Options(fetchSize = Integer.MIN_VALUE)` で MySQL Streaming を有効にする仕組み

---

### ⬜ テーマ 5：Redis + WebSocket 通知フローの整理
**ブランチ：** `claude/refactor-theme5-redis-ws`（未作成）

予定内容：
- `RedisNotificationMessage`（冗長なラッパー）を廃止、`NotificationMessage` に `destination` フィールドを追加
- Redis チャネル名 `"ws-notifications"` を `application.properties` に移動
- `NotificationWebSocketController` の未使用メソッドを削除
- JS 再接続ロジックを指数バックオフに書き直す（1s → 2s → 4s → 最大 30s）

---

## 進め方のルール

1. ユーザーが「テーマ X を始めます」と言う
2. Claude が `claude/refactor-themeX-*` ブランチを `feature/observability` から作成してプッシュ
3. ユーザーがコードを読んで理解し、自分のブランチで再実装
4. 完了したら次のテーマへ

**Claude が勝手に先へ進まない。** ユーザーの合図を待つ。

---

## 現在の重要な状態

- **ユーザーの作業ブランチ：** `feature/observability`
- **Phase 3 はユーザーが実装中** → 完了報告があるまで Phase 4 には触れない
- **次に着手するテーマ：** テーマ 3（Kafka Analytics Consumer）
  - ユーザーがテーマ 2 の内容を自分のブランチに反映したら開始

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
