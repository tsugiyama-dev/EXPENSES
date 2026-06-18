# 2026-06-18 業務フローとドメインモデル

## 目的

`expenses` の申請、提出、承認、却下の流れを、Java標準ライブラリだけで表現する。フレームワークやDBの前に、業務状態と状態遷移をコードで安全に扱う感覚を身につける。

## 7時間の進め方

- 1.0h: `ExpenseService` の提出、承認、却下処理を読み、状態遷移を洗い出す
- 1.5h: `enum`、`record`、不変オブジェクト、例外設計を復習する
- 2.0h: 経費申請のミニドメインモデルを実装する
- 1.0h: Serviceに置く処理とDomainに置く処理を分けて整理する
- 1.0h: `expenses` のリファクタ候補をメモする
- 0.5h: 日報を作成する

## 成果物

### 状態をenumで表す

```java
enum ExpenseStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED
}
```

### 経費申請をrecordで表す

```java
record Expense(
        long id,
        long applicantId,
        String title,
        int amount,
        ExpenseStatus status,
        int version
) {
    Expense submit(long actorId) {
        if (actorId != applicantId) {
            throw new IllegalStateException("本人以外は提出できません");
        }
        if (status != ExpenseStatus.DRAFT) {
            throw new IllegalStateException("下書き以外は提出できません");
        }
        return new Expense(id, applicantId, title, amount, ExpenseStatus.SUBMITTED, version + 1);
    }

    Expense approve() {
        if (status != ExpenseStatus.SUBMITTED) {
            throw new IllegalStateException("提出済み以外は承認できません");
        }
        return new Expense(id, applicantId, title, amount, ExpenseStatus.APPROVED, version + 1);
    }

    Expense reject() {
        if (status != ExpenseStatus.SUBMITTED) {
            throw new IllegalStateException("提出済み以外は却下できません");
        }
        return new Expense(id, applicantId, title, amount, ExpenseStatus.REJECTED, version + 1);
    }
}
```

## 実務への接続

`ExpenseService` に状態遷移の条件が増えると、通知、監査ログ、DB更新と混ざって読みづらくなる。状態遷移の可否はドメイン側に寄せ、Serviceは「取得、状態変更、保存、副作用の起動」に集中させると将来の修正範囲を小さくできる。

## リファクタ観点

- `canBeSubmittedBy`、`canBeApproved`、`canBeRejected` の責務をドメイン側に集約する
- Service内の重複した存在チェック、状態チェック、versionチェックを共通化する
- 例外メッセージとエラーコードを状態遷移ごとに整理する
- 状態遷移の単体テストをDBなしで書ける形にする

## 日報

### 作業概要

- Javaの `enum` を使い、状態を型として表現する方法を学習した
- Javaの `record` を使い、不変データを簡潔に定義する方法を確認した
- 状態遷移をメソッドとして実装し、不正な操作を例外で防ぐ処理を作成した
- 条件分岐を呼び出し側ではなく、データを持つクラス側へ寄せる考え方を整理した
- フレームワークを使わず、Javaだけで業務ルールを表現する練習を行った

### 作業所感

`enum` と `record` を使うことで、文字列やMapだけで表現するよりも状態やデータ構造が明確になると感じた。特に状態遷移をメソッドとして持たせると、どの状態からどの状態へ変更できるかをコード上で追いやすくなる。Javaの基本機能だけでも、業務ルールを安全に表現するための土台を作れることが分かった。
