# 2026-06-23 非同期処理とProducer-Consumer

## 目的

Kafkaを直接使わず、Java標準ライブラリの `BlockingQueue` と `ExecutorService` で非同期処理の基本を理解する。処理を依頼する側と実行する側を分ける設計を練習する。

## 7時間の進め方

- 1.0h: `ExpenseKafkaProducer` と `ExpenseKafkaNotificationConsumer` の役割を読む
- 1.5h: `BlockingQueue`、`ExecutorService`、スレッド終了処理を復習する
- 2.0h: 通知キューとConsumerを実装する
- 1.0h: 同期処理と非同期処理の違い、失敗時の影響を整理する
- 1.0h: `expenses` の通知処理における責務分割を考える
- 0.5h: 日報を作成する

## 成果物

### 通知メッセージ

```java
record NotificationMessage(long expenseId, long userId, String type, String text) {}
```

### Producer

```java
import java.util.concurrent.BlockingQueue;

class NotificationProducer {
    private final BlockingQueue<NotificationMessage> queue;

    NotificationProducer(BlockingQueue<NotificationMessage> queue) {
        this.queue = queue;
    }

    void publish(NotificationMessage message) {
        queue.offer(message);
    }
}
```

### Consumer

```java
import java.util.concurrent.BlockingQueue;

class NotificationConsumer implements Runnable {
    private final BlockingQueue<NotificationMessage> queue;
    private volatile boolean running = true;

    NotificationConsumer(BlockingQueue<NotificationMessage> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (running || !queue.isEmpty()) {
            try {
                NotificationMessage message = queue.take();
                send(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    void stop() {
        running = false;
    }

    private void send(NotificationMessage message) {
        System.out.println("send notification: " + message);
    }
}
```

### 起動例

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

BlockingQueue<NotificationMessage> queue = new LinkedBlockingQueue<>();
NotificationProducer producer = new NotificationProducer(queue);
NotificationConsumer consumer = new NotificationConsumer(queue);

ExecutorService executor = Executors.newSingleThreadExecutor();
executor.submit(consumer);

producer.publish(new NotificationMessage(1L, 10L, "APPROVED", "経費が承認されました"));
```

## 実務への接続

Kafkaは分散されたProducer-Consumerの仕組みとして理解できる。Serviceが通知処理を直接実行すると、通知が遅い場合に業務処理も遅くなる。キューに積むだけにすると、業務処理と通知処理を時間的に分離できる。

## リファクタ観点

- 通知Consumerがメール、DB保存、WebSocket配信をまとめて持ちすぎていないか確認する
- メッセージを作る責務とメッセージを処理する責務を分ける
- Consumerの失敗時に再実行するのか、諦めるのか、手動確認に回すのかを決める
- 非同期化しても業務上問題ない処理かどうかを分類する

## 日報

### 作業概要

- Javaの `BlockingQueue` を使い、Producer-Consumer構成を実装した
- `ExecutorService` を使って、処理を別スレッドで実行する方法を確認した
- `Runnable`、`volatile`、`InterruptedException` の基本的な扱いを学習した
- 同期処理と非同期処理の違いを、処理の流れと失敗時の影響から整理した
- キューを使って処理依頼側と処理実行側を分離する練習を行った

### 作業所感

`java.util.concurrent` を使うことで、非同期処理の基本構造をフレームワークなしで理解できた。特に `BlockingQueue` は、依頼された処理を順番に受け渡す仕組みとして分かりやすかった。一方で、スレッド停止や割り込み処理を考慮しないと安全に終了できないため、非同期処理では正常系だけでなく終了処理も設計する必要があると感じた。
