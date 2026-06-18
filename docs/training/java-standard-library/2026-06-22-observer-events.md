# 2026-06-22 イベント設計とObserver

## 目的

`@TransactionalEventListener` やSpring Eventの前提を外し、JavaのinterfaceとListだけでイベント通知の基本を理解する。Serviceから通知、監査ログ、分析処理を直接呼ばない設計を練習する。

## 7時間の進め方

- 1.0h: `ExpenseKafkaBridgeListener` と `ExpenseService` のイベント発行箇所を読む
- 1.5h: Observerパターン、Publisher-Subscriber、疎結合の考え方を整理する
- 2.0h: イベントPublisherとListenerを実装する
- 1.0h: 同期イベントとトランザクション後イベントの違いを整理する
- 1.0h: `expenses` でイベント化すべき処理を洗い出す
- 0.5h: 日報を作成する

## 成果物

### Eventを型で表す

```java
sealed interface ExpenseEvent permits ExpenseSubmittedEvent, ExpenseApprovedEvent, ExpenseRejectedEvent {
    long expenseId();
    long actorId();
}

record ExpenseSubmittedEvent(long expenseId, long actorId) implements ExpenseEvent {}
record ExpenseApprovedEvent(long expenseId, long actorId, long applicantId) implements ExpenseEvent {}
record ExpenseRejectedEvent(long expenseId, long actorId, long applicantId, String reason) implements ExpenseEvent {}
```

### Listenerを定義する

```java
interface ExpenseEventListener {
    void onEvent(ExpenseEvent event);
}
```

### Publisherを実装する

```java
import java.util.ArrayList;
import java.util.List;

class ExpenseEventPublisher {
    private final List<ExpenseEventListener> listeners = new ArrayList<>();

    void register(ExpenseEventListener listener) {
        listeners.add(listener);
    }

    void publish(ExpenseEvent event) {
        for (ExpenseEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
```

### 複数の副作用をListenerへ分ける

```java
class AuditLogListener implements ExpenseEventListener {
    @Override
    public void onEvent(ExpenseEvent event) {
        System.out.println("audit: " + event);
    }
}

class NotificationListener implements ExpenseEventListener {
    @Override
    public void onEvent(ExpenseEvent event) {
        System.out.println("notify: " + event);
    }
}
```

## 実務への接続

`ExpenseService` がメール、WebSocket、監査ログ、分析処理を直接呼び始めると、変更理由が増えすぎる。イベント発行にすると、Serviceは業務状態の変更に集中し、通知や分析はListener側で追加できる。

ただし、同期イベントはListenerの失敗がServiceの失敗に直結する。`expenses` のようにDB更新後にKafkaへ流す場合は、コミット後に発行する設計が必要になる。

## リファクタ観点

- Serviceから直接呼ばれている副作用をイベントに寄せられないか確認する
- イベント名、payload、必須項目を業務用語で整理する
- Listenerの失敗が元処理へ影響してよいかを処理ごとに分類する
- 監査ログのように同一トランザクションで必要なものと、通知のように非同期でよいものを分ける

## 日報

### 作業概要

- Java標準ライブラリのみでイベントPublisherとListenerを実装した
- 経費提出、承認、却下をイベント型として定義した
- 監査ログ処理と通知処理をListenerとして分離した
- Serviceから副作用を直接呼ばない設計の利点を整理した
- 同期イベントとコミット後イベントの違いを確認した

### 作業所感

Observerパターンで副作用を分離すると、機能追加時にServiceへ手を入れる範囲を小さくできると分かった。一方で、イベントを使えば必ずよいわけではなく、Listenerの失敗をどう扱うかを決める必要がある。`expenses` では通知や分析のような処理はイベント化しやすいが、監査ログのように業務更新と一体で扱いたい処理は慎重に分ける必要があると感じた。
