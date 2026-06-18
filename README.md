# Expenses Application — 経費申請管理システム

Spring Boot を中心に、**イベント駆動アーキテクチャ・リアルタイム通知・バッチ処理・可観測性**を統合した経費申請管理アプリケーションです。
複数インスタンス構成を前提に設計しており、Kafka / Redis / WebSocket を用いた非同期処理とスケールアウトを実践しています。

---

## 目次
- [主な特徴](#主な特徴)
- [技術スタック](#技術スタック)
- [アーキテクチャ](#アーキテクチャ)
- [主要機能](#主要機能)
- [ドメインモデル](#ドメインモデル)
- [セットアップ・起動方法](#セットアップ起動方法)
- [API 概要](#api-概要)
- [監視（Observability）](#監視observability)
- [テスト](#テスト)
- [設計上の工夫・学習ポイント](#設計上の工夫学習ポイント)

---

## 主な特徴

- **イベント駆動**：経費の提出・承認・却下を Spring Events → Kafka に橋渡しし、メール通知・WebSocket 通知・監査ログ・分析を疎結合に処理
- **マルチインスタンス対応**：Redis Pub/Sub を経由して、どのインスタンスに接続中のユーザーにも WebSocket 通知が届く
- **トランザクション整合性**：`@TransactionalEventListener(AFTER_COMMIT)` により DB コミット後にのみイベントを発火し、DB と Kafka の不整合を防止
- **信頼性**：Kafka コンシューマーのリトライ（`@RetryableTopic`）と Dead Letter Topic（`@DltHandler`）による失敗ハンドリング
- **キャッシュ**：Redis をキャッシュとして利用（`@Cacheable` / `@CacheEvict`、トランザクション境界を意識したエビクション）
- **バッチ処理**：Spring Batch による CSV インポート / エクスポート、PDF / Excel 帳票出力
- **可観測性**：Actuator + Micrometer + Prometheus + Grafana、リクエストごとの TraceID 付与
- **セキュリティ**：Spring Security によるロールベース認可、WebSocket（STOMP）の認証・認可インターセプター

---

## 技術スタック

| カテゴリ | 技術 |
|---------|------|
| 言語 / FW | Java 21, Spring Boot 4.0 |
| 永続化 | MySQL 8, MyBatis, Flyway（マイグレーション） |
| メッセージング | Apache Kafka |
| キャッシュ / Pub-Sub | Redis |
| リアルタイム通信 | WebSocket（STOMP + SockJS） |
| バッチ | Spring Batch |
| 認証・認可 | Spring Security |
| 帳票 | OpenPDF（PDF）, Apache POI（Excel） |
| 可観測性 | Spring Boot Actuator, Micrometer, Prometheus, Grafana |
| テンプレート | Thymeleaf |
| ビルド | Maven |
| コンテナ | Docker / Docker Compose |
| メール（開発） | MailHog |

---

## アーキテクチャ

複数のアプリインスタンス（app1 / app2）を Docker Compose 上で起動し、ミドルウェアを共有します。

```
                    ┌─────────────┐     ┌─────────────┐
   Browser ───────► │    app1     │     │    app2     │ ◄─────── Browser
   (WebSocket)      │ Spring Boot │     │ Spring Boot │      (WebSocket)
                    └──────┬──────┘     └──────┬──────┘
                           │                   │
        ┌──────────────────┼───────────────────┼──────────────────┐
        │                  │                   │                  │
   ┌────▼────┐      ┌──────▼──────┐     ┌───────▼──────┐    ┌──────▼──────┐
   │  MySQL  │      │    Kafka    │     │    Redis     │    │   MailHog   │
   │ (MyBatis│      │ (イベント   │     │ (Pub/Sub +   │    │ (メール検証 │
   │ +Flyway)│      │  配信)      │     │  キャッシュ) │    │  用 SMTP)   │
   └─────────┘      └─────────────┘     └──────────────┘    └─────────────┘

   ┌────────────┐   ┌────────────┐
   │ Prometheus │──►│  Grafana   │   ← /actuator/prometheus をスクレイプして可視化
   └────────────┘   └────────────┘
```

### 経費イベントの処理フロー

```
ExpenseService.submit() / approve() / reject()  [@Transactional]
   │
   ├─ DB 更新（expenses / audit_logs）
   └─ ApplicationEventPublisher.publishEvent(ExpenseXxxEvent)
        │
        ▼  DB コミット後に発火（@TransactionalEventListener AFTER_COMMIT）
   ExpenseKafkaBridgeListener ──► Kafka "expense-events" トピック
        │
        ├──► NotificationConsumer  ：メール送信 + 未読通知DB保存 + WebSocket通知
        │         │                   （失敗時は @RetryableTopic でリトライ → DLT）
        │         └──► RedisWebSocketPublisher ──► Redis Pub/Sub
        │                                              │
        │                          各インスタンスの RedisWebSocketSubscriber が購読
        │                                              │
        │                                  SimpMessagingTemplate ──► WebSocket ──► Browser
        ├──► AnalyticsConsumer      ：Micrometer メトリクス集計
        └──► LogConsumer            ：イベントログ出力
```

**ポイント**：通知の送信経路を Redis Pub/Sub に集約することで、ユーザーが app1 / app2 のどちらに接続していても通知が届く（WebSocket のセッションはインスタンスローカルなため、Redis で全インスタンスにファンアウトする）。

---

## 主要機能

### 経費申請ワークフロー
- 経費の作成（下書き）→ 提出 → 承認 / 却下
- ステータス遷移を `Expense` ドメインのメソッドで制御（提出は本人かつ下書きのみ、など）
- **楽観的ロック**（`version` カラム）による同時更新の検出

### 通知
- 提出時：承認者へメール + 全体ブロードキャスト通知
- 承認 / 却下時：申請者へメール + 個人宛 WebSocket 通知
- **オフライン通知の永続化**：ログアウト中に発生した通知を DB に保存し、ログイン時に未読を表示

### 添付・帳票
- 領収書（レシート）画像のアップロード / ダウンロード
- 経費一覧の CSV / Excel / PDF 出力（CSV は MyBatis Cursor によるストリーミング出力に対応）

### バッチ
- CSV からの経費一括インポート
- 並列・チャンク処理、条件分岐フローなど Spring Batch の各種構成

---

## ドメインモデル

主なエンティティ：

- **Expense（経費）**：申請者・タイトル・金額・通貨・ステータス・version など
- **User / Role**：メールアドレスでログイン、ロールによる認可（申請者 / 承認者）
- **ExpenseAuditLog**：操作履歴（作成・提出・承認・却下）の監査ログ
- **Receipt**：経費に紐づく領収書ファイル
- **PendingNotification**：未読通知の永続化
- **Category**：経費カテゴリ

ステータス遷移：

```
DRAFT ──submit──► SUBMITTED ──approve──► APPROVED
                       │
                       └────reject─────► REJECTED
```

---

## セットアップ・起動方法

### 前提
- Docker / Docker Compose
- （ローカルビルドする場合）JDK 21, Maven

### Docker Compose で一括起動

```bash
# アプリ + MySQL + Kafka + Redis + MailHog + Prometheus + Grafana をまとめて起動
docker compose up --build
```

起動後のアクセス先：

| サービス | URL |
|---------|-----|
| アプリ（app1） | http://localhost:8080 |
| アプリ（app2） | http://localhost:8081 |
| MailHog（受信メール確認） | http://localhost:8025 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 （admin / admin） |

### ローカル実行（ミドルウェアのみ Docker）

```bash
# ミドルウェアだけ起動して、アプリは IDE / Maven から実行
docker compose up mysql kafka redis mailhog
./mvnw spring-boot:run
```

---

## API 概要

REST / 画面遷移あわせて約 50 のエンドポイントを提供。主なもの：

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/expenses` | 経費を作成 |
| POST | `/expenses/{id}/submit` | 提出 |
| POST | `/expenses/{id}/approve` | 承認（`ROLE_APPROVER` のみ） |
| POST | `/expenses/{id}/reject` | 却下（`ROLE_APPROVER` のみ） |
| GET | `/expenses` | 一覧（検索・ページング・ソート対応） |
| POST | `/api/expenses/{id}/receipts` | 領収書アップロード |
| GET | `/api/exports/pdf/expenses` | PDF 出力 |
| GET | `/api/exports/excel/expenses` | Excel 出力 |
| GET | `/api/exports/csv/expenses/stream` | CSV ストリーミング出力 |
| GET | `/api/notifications/unread` | 未読通知取得 |
| PUT | `/api/notifications/read` | 既読化 |

API ドキュメント（springdoc-openapi）：`/swagger-ui`

---

## 監視（Observability）

- すべてのリクエストに **TraceID** を付与し、ログとエラーレスポンスに含めることで追跡可能
- `/actuator/prometheus` で Micrometer メトリクスを公開
- Prometheus がスクレイプし、Grafana で可視化
- 経費イベント（提出 / 承認 / 却下）の件数を独自カウンターで計測

---

## テスト

```bash
./mvnw test
```

- サービス層のユニットテスト（Mockito）
- コントローラ層のテスト（MockMvc）
- バッチ処理のテスト
- Testcontainers による DB 連携テスト基盤

---

## 設計上の工夫・学習ポイント

| テーマ | 内容 |
|--------|------|
| **イベントの整合性** | `@EventListener` ではなく `@TransactionalEventListener(AFTER_COMMIT)` を使い、DB ロールバック時に Kafka へ誤送信しないようにした |
| **マルチインスタンス通知** | WebSocket セッションはインスタンスローカルなため、Redis Pub/Sub で全インスタンスへファンアウト |
| **Kafka の信頼性** | `@RetryableTopic` でリトライ、最終失敗は `@DltHandler` で Dead Letter Topic に隔離 |
| **キャッシュの安全性** | `RedisCacheManager` に `transactionAware()` を設定し、コミット後にキャッシュをエビクトして古いデータの再キャッシュを防止 |
| **WebSocket セキュリティ** | STOMP の Inbound チャネルに `ChannelInterceptor` を挟み、CONNECT の認証と SUBSCRIBE の認可をフレーム単位で実施 |
| **ストリーミング出力** | 大量データの CSV 出力で MyBatis `Cursor` を使い、メモリに全件載せずに逐次書き出し |

---

## ライセンス

学習・ポートフォリオ目的のプロジェクトです。
