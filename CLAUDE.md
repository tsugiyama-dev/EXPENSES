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
  ✅ WebSocket（完了）
     ✅ バックエンド基盤（STOMP/SockJS/イベントブリッジ）
     ✅ フロントデモページ（notification-test.html）
     ✅ 実画面（detail.html）への組み込み
     ❌ Redis Pub/Sub でスケールアウト対応  ← 次の作業

【Phase 2】非同期・イベント駆動
  ❌ Apache Kafka
     → 現在の SpringEvents → Kafka への置き換え体験

【Phase 3】観測性（Observability）
  ❌ Micrometer + Prometheus + Grafana
  ❌ 分散トレーシング（OpenTelemetry）
  ❌ 構造化ログ

【Phase 4】セキュリティ強化
  ❌ OAuth2 / OIDC（Keycloak）
  ❌ JWT Token 管理

【Phase 5】インフラ
  ❌ Docker → Kubernetes 基礎 → CI/CD（GitHub Actions）
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

## WebSocket 学習進捗

### ブランチ情報
- ユーザーの実装ブランチ: `feature/websocket-approval-notification`
- 作業ブランチ: `claude/websocket-frontend-demo-D9un2`

### 実装済みファイル

| ファイル | 内容 | 状態 |
|---|---|---|
| `config/WebSocketConfig.java` | STOMP/SockJS設定（/ws, /topic, /queue） | ✅ |
| `dto/NotificationMessage.java` | WebSocket用DTOクラス | ✅ |
| `controller/NotificationWebSocketController.java` | broadcast / 個人送信 | ✅ |
| `controller/TestController.java` | GET /test → テストページ | ✅ |
| `listener/ExpenseWebSocketNotificationListener.java` | SpringEvents→WebSocketブリッジ | ✅ |
| `static/js/notification.js` | NotificationManagerクラス（バグ修正済） | ✅ |
| `templates/notification-test.html` | モックデータでのデモページ | ✅ |
| `config/SecurityConfig.java` | /ws/** 許可・CSRF除外 | ✅ |

### 残タスク（優先順）

1. ~~**実画面への組み込み**~~ ✅ `detail.html` に組み込み完了
2. ~~**ユーザーID の動的取得**~~ ✅ Thymeleaf actorId で解決済み
3. ~~**動作確認**~~ ✅ 通知の送受信を確認済み
4. **Redis Pub/Sub 対応** - 複数インスタンス時でも通知が届くようスケールアウト対応　← **次の作業**

### アーキテクチャの現状と課題

```
【現状：単一インスタンスのみ動作】
経費操作 → SpringEvent → WebSocketListener → SimpMessagingTemplate → ブラウザ

【課題：スケールアウトで壊れる】
Server1 ←→ UserA の接続
Server2 ←→ UserB の接続
→ Server1で発生したイベントがServer2のUserBに届かない

【解決策：Redis Pub/Sub 導入後】
経費操作 → SpringEvent → WebSocketListener → Redis Publish
                                                    ↓
                                     全Serverがsubscribe → 各自のユーザーへ送信
```

---

## 重要な設計メモ

### イベント設計
- `ExpenseSubmittedEvent` → 全体ブロードキャスト（承認者全員が対象）
- `ExpenseApprovedEvent` / `ExpenseRejectedEvent` → 申請者個人へ `/queue/{userId}/notifications`

### セキュリティ
- `/ws/**` は `permitAll` + CSRF除外（SockJSハンドシェイクのため）
- WebSocket接続自体はセッションCookieで認証

### ブランチ戦略
作業は常に `claude/websocket-frontend-demo-D9un2` で実施し push する。

---

## 次セッション開始時にやること

1. このファイルを読む
2. `git log --oneline -5` で最新コミットを確認
3. 残タスクの先頭から着手

**次の具体的な作業: `detail.html` への notification.js 組み込み**

---

## 更新履歴

| 日付 | セッション内容 |
|---|---|
| 2026-04-26 | WebSocket バックエンド完成、フロントデモ完成、バグ修正。CLAUDE.md 作成。次: detail.html組み込み |
| 2026-04-26 | detail.html へ通知トースト組み込み完了（navbar WS状態バッジ、トーストCSS、スクリプトタグ）。次: Redis Pub/Sub |
| 2026-04-27 | WebSocket 実装完了・動作確認済み。feature/websocket-approval-notification にプッシュ。次: Redis Pub/Sub |
