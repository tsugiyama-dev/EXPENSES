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

- ServiceとRepositoryの責務分割をJava標準ライブラリで再現した
- `ExpenseRepository` interfaceを作成し、保存先の詳細をServiceから分離した
- `ConcurrentHashMap` を使ったインメモリRepositoryを作成した
- `Optional` を使い、存在しないデータの扱いを明示した
- `expenses` のMapper依存を整理するための観点を洗い出した

### 作業所感

Repositoryをinterfaceとして切ると、Serviceの関心が業務手順に集中することが分かった。実務ではMyBatis Mapperを直接使う方が短く書けるが、Serviceのテストや将来の保存先変更を考えると、境界を明確にする価値がある。特に`ExpenseService`は状態遷移、監査ログ、イベント発行、検索処理が混ざっているため、まず保存処理との境界を意識して読むことが重要だと感じた。
