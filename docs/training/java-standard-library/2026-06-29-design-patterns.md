# 2026-06-29 デザインパターン整理

## 目的

Java標準ライブラリだけで実装したミニ経費アプリに、実務で使いやすいデザインパターンを対応付ける。パターン名を覚えることではなく、どの変更に強くなるかを理解する。

## 7時間の進め方

- 1.0h: これまで作成したDomain、Repository、Event、Queue、Cache、CSV出力を見直す
- 1.5h: Strategy、Factory Method、Template Method、Decorator、Commandを復習する
- 2.0h: 経費アプリにパターンを適用したコード例を作成する
- 1.0h: パターンを使うべき場面、使わない方がよい場面を整理する
- 1.0h: `expenses` のリファクタ候補へ対応付ける
- 0.5h: 日報を作成する

## 成果物

## パターン対応表

| パターン | 使いどころ | `expenses` での候補 |
| --- | --- | --- |
| Repository | 保存先の詳細を隠す | MyBatis Mapper依存の整理 |
| Observer | 副作用を追加しやすくする | 通知、監査ログ、分析イベント |
| Producer-Consumer | 処理を非同期化する | Kafka通知処理 |
| Strategy | 処理方式を差し替える | CSV、PDF、Excelエクスポート |
| Factory Method | 複雑な生成処理を集約する | 通知メッセージ生成 |
| Template Method | 処理の流れを固定し一部だけ差し替える | エクスポート共通処理 |
| Decorator | 既存処理にログや計測を追加する | 処理時間測定、メトリクス |
| Command | 操作をオブジェクトとして扱う | 承認、却下、再通知 |

### Strategy

```java
interface ExportStrategy {
    String extension();
    byte[] export(Iterable<Expense> expenses);
}

class CsvExportStrategy implements ExportStrategy {
    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public byte[] export(Iterable<Expense> expenses) {
        return "csv data".getBytes();
    }
}
```

### Factory Method

```java
class NotificationFactory {
    NotificationMessage approved(Expense expense) {
        return new NotificationMessage(
                expense.id(),
                expense.applicantId(),
                "EXPENSE_APPROVED",
                "経費が承認されました");
    }
}
```

### Decorator

```java
class TimedExpenseRepository implements ExpenseRepository {
    private final ExpenseRepository delegate;

    TimedExpenseRepository(ExpenseRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public java.util.Optional<Expense> findById(long id) {
        long start = System.nanoTime();
        try {
            return delegate.findById(id);
        } finally {
            long elapsed = System.nanoTime() - start;
            System.out.println("findById elapsedNanos=" + elapsed);
        }
    }

    @Override
    public Expense save(Expense expense) {
        return delegate.save(expense);
    }
}
```

### Command

```java
interface ExpenseCommand {
    Expense execute(Expense expense);
}

class ApproveExpenseCommand implements ExpenseCommand {
    @Override
    public Expense execute(Expense expense) {
        return expense.approve();
    }
}
```

## 実務への接続

デザインパターンは、現在のコードを複雑にするためではなく、今後増える変更に備えるために使う。変更が1種類しかない場所に無理にパターンを入れると読みにくくなる。逆に、通知方式、出力形式、イベント処理のように増えやすい箇所では、パターンを使うことで追加時の修正範囲を小さくできる。

## リファクタ観点

- エクスポート形式の追加はStrategyで扱えるか確認する
- 通知メッセージ生成の重複はFactoryへ寄せられるか確認する
- ログ、メトリクス、処理時間測定はDecoratorで追加できるか検討する
- 承認、却下、再申請などの操作をCommandとして扱う必要があるか考える
- パターン導入の前に、対象箇所に本当に変更の軸があるか確認する

## 日報

### 作業概要

- Javaでよく使われるデザインパターンの目的と使いどころを整理した
- Strategy、Factory Method、Decorator、Commandのサンプルコードを作成した
- `interface` と委譲を使い、処理の差し替えや追加を行う方法を確認した
- デザインパターンを使う場面と、使いすぎると複雑になる場面を整理した
- 変更に強いコードを書くために、変更理由ごとにクラスを分ける考え方を学習した

### 作業所感

デザインパターンは名前だけを覚えると抽象的だが、実際にJavaで小さなコードを書くと目的が理解しやすくなった。Strategyは処理方式の差し替え、Factoryは生成処理の集約、Decoratorは既存処理への機能追加に向いていることが分かった。一方で、必要以上にパターンを使うとクラスが増えて読みにくくなるため、変更が見込まれる箇所に絞って使う判断が重要だと感じた。
