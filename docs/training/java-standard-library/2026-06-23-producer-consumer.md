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

- `BlockingQueue` を使って通知キューを実装した
- ProducerとConsumerを分け、処理依頼と実行を分離した
- `ExecutorService` を使ってConsumerを別スレッドで動かす構成を確認した
- 同期処理と非同期処理の違いを整理した
- Kafkaの基本構造をJava標準ライブラリで抽象的に理解した

### 作業所感

`BlockingQueue` でProducer-Consumerを実装すると、Kafkaが担っている役割を小さく理解できた。非同期化すると業務処理の応答は軽くなるが、通知処理の失敗が利用者に見えづらくなるため、後続のRetryやDLQの設計が必要になる。`expenses` の通知Consumerは複数の処理をまとめているため、将来的には処理単位ごとに分けた方が修正しやすいと感じた。
