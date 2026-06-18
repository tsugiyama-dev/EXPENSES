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

- Javaの `interface` と `List` を使い、Observerパターンの基本構造を実装した
- イベントを `sealed interface` と `record` で表現する方法を学習した
- PublisherにListenerを登録し、複数の処理へ通知する流れを作成した
- 直接メソッドを呼び出す構成と、イベントで間接的に通知する構成の違いを整理した
- Listenerを追加するだけで処理を拡張できる設計を確認した

### 作業所感

ObserverパターンをJavaだけで実装すると、イベント通知の仕組みが単純な部品の組み合わせでできていることが分かった。処理を直接呼ぶよりも構造は少し増えるが、後から処理を追加しやすくなる利点がある。`sealed interface` と `record` を組み合わせると、イベントの種類とデータを明確に表現できる点も学びになった。
