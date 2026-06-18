# Java標準ライブラリ学習成果物

このディレクトリは、`feature/observability` の実装内容をリファクタリングする前段階として、Java標準ライブラリだけで設計・実装の考え方を学ぶための研修成果物です。

Spring Boot、Kafka、Redis、MyBatis、Prometheusなどのフレームワーク機能を直接学ぶ前に、以下のような基礎概念へ抽象化して扱います。

- 業務状態を `enum` とドメインモデルで表す
- ServiceとRepositoryの責務を分ける
- Observer / Publisher-Subscriberで副作用を分離する
- `BlockingQueue` と `ExecutorService` で非同期処理を理解する
- Retry / Dead Letter Queueで失敗処理を設計する
- `ConcurrentHashMap` でTTL付きキャッシュを実装する
- `BufferedWriter` と `Files` でストリーミング出力を行う
- Strategy、Factory Method、Template Method、Decorator、Commandを業務コードに適用する

## 日程別成果物

| 日付 | テーマ | 成果物 |
| --- | --- | --- |
| 2026-06-18 | 業務フローとドメインモデル | [2026-06-18-domain-model.md](./2026-06-18-domain-model.md) |
| 2026-06-19 | 責務分割とRepository | [2026-06-19-repository-responsibility.md](./2026-06-19-repository-responsibility.md) |
| 2026-06-22 | イベント設計とObserver | [2026-06-22-observer-events.md](./2026-06-22-observer-events.md) |
| 2026-06-23 | 非同期処理とProducer-Consumer | [2026-06-23-producer-consumer.md](./2026-06-23-producer-consumer.md) |
| 2026-06-24 | RetryとDead Letter Queue | [2026-06-24-retry-dlq.md](./2026-06-24-retry-dlq.md) |
| 2026-06-25 | TTL付きCache設計 | [2026-06-25-ttl-cache.md](./2026-06-25-ttl-cache.md) |
| 2026-06-26 | CSVストリーミング出力 | [2026-06-26-csv-streaming.md](./2026-06-26-csv-streaming.md) |
| 2026-06-29 | デザインパターン整理 | [2026-06-29-design-patterns.md](./2026-06-29-design-patterns.md) |
| 2026-06-30 | リファクタ計画 | [2026-06-30-refactoring-plan.md](./2026-06-30-refactoring-plan.md) |

## 日報形式

各成果物の末尾に、会社提出用の日報として使いやすいように以下の項目を入れています。

- 作業概要: すぐに内容を把握できるリスト形式
- 作業所感: 学習内容から得た気づき、実務へつながる観点

## 使い方

1日7時間の研修時間を想定し、各日で完結する内容にしています。各ファイルを上から順に確認し、コード例を手元で写経または小さく改変すると、`expenses` の将来リファクタで必要になる設計判断の練習になります。
