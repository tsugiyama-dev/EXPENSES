# 2026-06-19 責務分割とRepository

## 目的

ServiceがDB操作の詳細を直接意識しない形を、Java標準ライブラリだけで練習する。MyBatis Mapperを使う前提ではなく、保存先を抽象化することでテストしやすい設計を理解する。

## 7時間の進め方

- 1.0h: `ExpenseService` と `ExpenseMapper` の役割を読み分ける
- 1.5h: interface、`Map`、`Optional`、例外設計を復習する
- 2.0h: `ExpenseRepository` とインメモリ実装を作成する
- 1.0h: Serviceが持つべき責務、Repositoryが持つべき責務を整理する
- 1.0h: MyBatis Mapperへ戻した場合の境界線を考える
- 0.5h: 日報を作成する

## 成果物

### Repository interface

```java
import java.util.Optional;

interface ExpenseRepository {
    Optional<Expense> findById(long id);
    Expense save(Expense expense);
}
```

### インメモリ実装

```java
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryExpenseRepository implements ExpenseRepository {
    private final Map<Long, Expense> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Expense> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Expense save(Expense expense) {
        store.put(expense.id(), expense);
        return expense;
    }
}
```

### Serviceは業務手順に集中する

```java
class ExpenseApplicationService {
    private final ExpenseRepository repository;

    ExpenseApplicationService(ExpenseRepository repository) {
        this.repository = repository;
    }

    Expense submit(long expenseId, long actorId) {
        Expense expense = repository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("経費申請が見つかりません"));

        Expense submitted = expense.submit(actorId);
        return repository.save(submitted);
    }
}
```

## 実務への接続

MyBatis MapperはDBアクセスの実装詳細であり、ServiceがSQLや永続化の都合を知りすぎると変更しづらくなる。Repository interfaceを挟む考え方を持つと、単体テストではインメモリ実装を使い、実運用ではMyBatis実装を使う、という切り替えがしやすくなる。

## リファクタ観点

- `ExpenseMapper` を直接使う箇所が増えすぎていないか確認する
- ServiceのテストでDBが不要なロジックを切り出せないか検討する
- `findById` の戻り値が `null` になる設計を `Optional` 相当の扱いへ寄せる
- 検索、更新、集計、エクスポート向け読み取りを同じMapperに詰め込みすぎていないか確認する

## 日報

### 作業概要

- Javaの `interface` を使い、処理の利用側と実装側を分ける方法を学習した
- `Map` と `ConcurrentHashMap` を使い、インメモリの保存処理を実装した
- `Optional` を使い、値が存在しないケースを明示的に扱う方法を確認した
- Service相当のクラスとRepository相当のクラスで責務を分ける練習を行った
- 実装クラスを差し替えやすくするための依存関係の持たせ方を整理した

### 作業所感

`interface` を使うと、呼び出し側が具体的な保存方法を知らなくても処理を進められることが分かった。`Map` による簡易実装でも、責務を分けることでテストしやすい構成を作れる。Javaの基本構文だけで抽象化の効果を確認できたため、今後は処理を実装する前に「何をinterfaceとして切り出せるか」を意識したい。
