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

- 経費アプリに適用できるデザインパターンを整理した
- Strategy、Factory Method、Decorator、Commandのコード例を作成した
- `expenses` のエクスポート、通知、計測、状態変更処理へパターンを対応付けた
- パターンを導入すべき場面と導入しない方がよい場面を整理した
- リファクタ時に優先して検討するパターンを洗い出した

### 作業所感

デザインパターンは名前から入ると抽象的に感じるが、変更理由に対応付けると実務で使う判断がしやすくなると分かった。`expenses` では特にエクスポート形式と通知処理が増えやすいため、StrategyやFactoryの効果が出やすい。一方で、まだ変更軸がはっきりしない箇所に先回りしてパターンを入れると複雑化するため、導入範囲を絞ることが重要だと感じた。
