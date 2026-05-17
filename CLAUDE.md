# EXPENSES プロジェクト - 学習進捗・引き継ぎ情報

> このファイルは**セッション間の引き継ぎ用**です。作業開始時に必ず読み、作業後に更新してください。

---

## プロジェクト概要

Spring Boot 4.x ベースの経費管理システム。実務レベルの技術を学習するための教材プロジェクト。

| 項目 | 内容 |
|---|---|
| フレームワーク | Spring Boot 4.0.1（Spring Framework 7.x） |
| 言語 | Java 21 |
| DB | MySQL（Flyway マイグレーション） |
| ORM | MyBatis |
| セキュリティ | Spring Security |
| テンプレート | Thymeleaf |
| インフラ | Docker Compose |

---

## 目標

**大規模システムで通用するエンジニアになる**ための実践的な技術習得。
単なる機能実装ではなく「なぜその技術が必要か」を理解しながら進める。

---

## 学習ロードマップ

```
【Phase 1】今動いているものを大規模対応させる
  ✅ Spring Batch 基礎（お題1〜5）
  ✅ WebSocket + Redis Pub/Sub（完了）
     ✅ バックエンド基盤（STOMP/SockJS/イベントブリッジ）
     ✅ フロントデモページ（notification-test.html）
     ✅ 実画面（detail.html）への組み込み
     ✅ Redis Pub/Sub でスケールアウト対応

【Phase 2】非同期・イベント駆動
  ✅ Apache Kafka（完了）
     ✅ インフラ整備（docker-compose, pom.xml, application.properties）
     ✅ 基盤クラス（ExpenseEventMessage, KafkaConfig, Producer, Consumer, BridgeListener）
     ✅ 動作確認（Consumer Group ログで各グループ処理を確認済み）
     ✅ Consumer での実処理実装（メール送信 / WebSocket / 監査ログ / 分析）
     ✅ SpringEvent を廃止して KafkaTemplate.send() に直結

【Phase 3】観測性（Observability）  ← 現在
  ❌ Micrometer + Prometheus + Grafana
  ❌ 分散トレーシング（OpenTelemetry）
  ❌ 構造化ログ

【Phase 4】セキュリティ強化
  ❌ OAuth2 / OIDC（Keycloak）
  ❌ JWT Token 管理

【Phase 5】インフラ
  ❌ Docker → Kubernetes 基礎 → CI/CD（GitHub Actions）

【Phase 6】Spring Batch 応用
  ❌ Skip / Retry / デッドレターハンドリング
  ❌ Job 監視（Micrometer 連携）← Phase 3 の知識が前提
  ❌ Kafka → Batch Job トリガー連携 ← Phase 2 の知識が前提
```

---

## Spring Batch 学習進捗

| お題 | 内容 | 状態 |
|---|---|---|
| お題1 | CSV Import（FlatFileItemReader） | ✅ 完了 |
| お題2 | DB→CSV Export（FlatFileItemWriter） | ✅ 完了 |
| お題3 | Paging処理（MyBatisPagingItemReader） | ✅ 完了 |
| お題4 | 並列処理（Partitioning） | ✅ 完了 |
| お題5 | 条件分岐フロー（Conditional Flow） | ✅ 完了（基礎編） |
| 応用 | エラーリカバリ・Skip/Retry・監視 | ❌ 未着手 |

---

## Phase 1+2 実装済みアーキテクチャ（main マージ済み）

```
経費操作 → KafkaTemplate.send()
               ↓
          Kafka Topic: expense-events
               ↓ （3 Consumer Group が並行消費）
          ├── expenses-audit-log    → 監査ログ書き込み
          ├── expenses-notification → メール送信 + Redis Publish → WebSocket → ブラウザ
          └── expenses-analytics   → 分析処理
```

### ブランチ戦略
- Phase 1+2 は `feature/websocket-approval-notification` → `main` にマージ済み（PR #9）
- Phase 3 は `main` から `feature/observability` を切って着手

---

## 次セッション開始時にやること

1. このファイルを読む
2. `git log --oneline -5` で最新コミットを確認
3. 残タスクの先頭から着手

**次の具体的な作業:**
1. `main` から `feature/observability` ブランチを切る
2. `pom.xml` に `spring-boot-starter-actuator` + `micrometer-registry-prometheus` を追加
3. `docker-compose.yml` に Prometheus + Grafana を追加
4. `application.properties` で `/actuator/prometheus` を公開設定
5. `http://localhost:9090` で Prometheus の動作確認

---

## 更新履歴

| 日付 | セッション内容 |
|---|---|
| 2026-04-26 | WebSocket バックエンド完成、フロントデモ完成、バグ修正。CLAUDE.md 作成。次: detail.html組み込み |
| 2026-04-26 | detail.html へ通知トースト組み込み完了（navbar WS状態バッジ、トーストCSS、スクリプトタグ）。次: Redis Pub/Sub |
| 2026-04-27 | WebSocket 実装完了・動作確認済み。feature/websocket-approval-notification にプッシュ。次: Redis Pub/Sub |
| 2026-04-27 | Redis Pub/Sub 実装完了。WebSocket スケールアウト対応完成。Phase 1 完了。次: Spring Batch 応用 or Kafka |
| 2026-05-02 | Kafka 基盤整備完了。docker-compose/pom/properties/kafka パッケージ一式。Spring Batch 応用は Phase 6 へ。次: 動作確認 |
| 2026-05-17 | Phase 1+2 完了。feature/websocket-approval-notification → main を PR #9 でマージ。次: feature/observability で Phase 3 開始 |
