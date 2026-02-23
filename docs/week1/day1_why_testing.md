# Day 1: テストとは何か？なぜ必要か？ 🤔

**学習時間:** 30分
**難易度:** ⭐☆☆☆☆

---

## 🎯 今日の目標

- テストの重要性を理解する
- テストがない世界の恐怖を知る
- テストがある世界の安心感を体感する
- テストの種類を知る

---

## 📖 ストーリーで理解する

### シーン1: テストがない世界 😱

**あなた**: 新人プログラマー
**タスク**: 経費申請システムに「金額の上限チェック」機能を追加

```java
// 金額チェック機能を追加
public void submit(Long expenseId) {
    Expense expense = expenseMapper.findById(expenseId);

    // 🆕 新しく追加したコード
    if(expense.getAmount().compareTo(new BigDecimal("100000")) > 0) {
        throw new BusinessException("AMOUNT_OVER", "金額が上限を超えています");
    }

    // 既存のコード
    expenseMapper.submitDraft(expenseId);
}
```

**あなた**: 「よし、実装完了！動作確認しよう」

```
1. ブラウザを開く
2. ログインする
3. 経費を作成する
4. 金額に "150000" を入力
5. 提出ボタンを押す
→ エラーが表示された！ ✅

「OK、正しく動いてる！」
```

**上司**: 「じゃあ本番環境にデプロイしてください」

**🚀 本番環境にデプロイ**

---

### 30分後...

**ユーザーA**: 「経費が提出できません！」
**ユーザーB**: 「エラーが出ます！」
**ユーザーC**: 「システムが壊れてる！」

**あなた**: 「え？さっき確認したのに...」

---

### 原因調査

```java
// バグ: nullチェックがない！
public void submit(Long expenseId) {
    Expense expense = expenseMapper.findById(expenseId);

    // expense が null だったら？
    if(expense.getAmount().compareTo(new BigDecimal("100000")) > 0) {
        // ← NullPointerException！
    }
}
```

**問題点:**
- 「存在しない経費ID」でテストしていなかった
- 手動テストは1つのパターンしか確認していない
- 他の機能が壊れていないか確認していない

**結果:**
- 😱 システムダウン
- 😱 ユーザーからのクレーム
- 😱 上司に怒られる
- 😱 緊急対応で徹夜

---

## シーン2: テストがある世界 😊

**同じタスク**: 金額の上限チェック機能を追加

```java
// 実装前にテストを書く
@Test
@DisplayName("金額が上限を超えている場合はエラー")
void 金額が上限を超えている場合() {
    // Given: 上限を超える金額の経費
    Expense expense = new Expense(...);
    expense.setAmount(new BigDecimal("150000"));

    // When & Then: エラーが発生する
    assertThatThrownBy(() -> service.submit(expense.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("金額が上限を超えています");
}

@Test
@DisplayName("存在しない経費IDの場合はエラー")
void 存在しない経費IDの場合() {
    // Given: 存在しないID
    long expenseId = 9999L;

    // When & Then: エラーが発生する
    assertThatThrownBy(() -> service.submit(expenseId))
        .isInstanceOf(NoSuchElementException.class);
}

@Test
@DisplayName("正常な経費は提出できる")
void 正常な経費は提出できる() {
    // Given: 正常な経費
    Expense expense = new Expense(...);
    expense.setAmount(new BigDecimal("5000"));

    // When: 提出
    ExpenseResponse result = service.submit(expense.getId());

    // Then: 成功
    assertThat(result.status()).isEqualTo(ExpenseStatus.SUBMITTED);
}
```

**実装**

```java
public void submit(Long expenseId) {
    Expense expense = expenseMapper.findById(expenseId);

    // ✅ nullチェック（テストで気づいた！）
    if(expense == null) {
        throw new NoSuchElementException("経費が見つかりません");
    }

    // ✅ 金額チェック
    if(expense.getAmount().compareTo(new BigDecimal("100000")) > 0) {
        throw new BusinessException("AMOUNT_OVER", "金額が上限を超えています");
    }

    expenseMapper.submitDraft(expenseId);
}
```

**テスト実行**

```bash
$ mvn test

[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] ✅ ALL TESTS PASSED
```

**結果:**
- ✅ バグを実装前に発見
- ✅ すべてのパターンを自動でテスト
- ✅ 他の機能が壊れていないことも確認
- ✅ 安心してデプロイ

---

## 💡 テストのメリット

### 1. バグを早期発見 🐛

```
テストなし:
  実装 → デプロイ → ユーザーがバグ発見 → 緊急対応

テストあり:
  実装 → テスト → バグ発見 → 修正 → デプロイ
```

**修正コスト:**
- 開発中: 1時間
- デプロイ後: 10時間（調査 + 修正 + 緊急対応）

---

### 2. リファクタリングの安全性 🔒

```java
// リファクタリング前
public void approve(Long expenseId) {
    Expense expense = expenseMapper.findById(expenseId);
    if(expense.getStatus() != ExpenseStatus.SUBMITTED) {
        throw new BusinessException("提出済み以外は承認できません");
    }
    expenseMapper.approve(expenseId);
}

// リファクタリング後（メソッド分割）
public void approve(Long expenseId) {
    Expense expense = findExpense(expenseId);
    validateStatus(expense);
    expenseMapper.approve(expenseId);
}

private Expense findExpense(Long expenseId) {
    return expenseMapper.findById(expenseId);
}

private void validateStatus(Expense expense) {
    if(expense.getStatus() != ExpenseStatus.SUBMITTED) {
        throw new BusinessException("提出済み以外は承認できません");
    }
}
```

**テストがある場合:**
```bash
$ mvn test
[INFO] ✅ ALL TESTS PASSED  ← 壊れてない！
```

**テストがない場合:**
```
「リファクタリングで壊れてないか不安...」
「怖くてリファクタリングできない...」
```

---

### 3. ドキュメント代わり 📚

**テストを見れば仕様がわかる:**

```java
@Test
@DisplayName("提出済みの経費を承認できる")
void 提出済みの経費を承認できる() {
    // ← 「提出済み」の経費が対象とわかる
}

@Test
@DisplayName("下書きの経費は承認できない")
void 下書きの経費は承認できない() {
    // ← 「下書き」は承認不可とわかる
}
```

---

### 4. 自信を持ってデプロイ 🚀

```
テストなし:
  「本当に大丈夫かな...」
  「バグがあったらどうしよう...」
  「金曜日にデプロイしたくない...」

テストあり:
  「テストが通ってるから大丈夫！」
  「安心してデプロイできる！」
  「金曜日でもOK！」
```

---

## 📊 テストの種類

### テストピラミッド

```
        /\
       /E2E\        ← システム全体（10%）
      /------\
     / 統合  \      ← 複数コンポーネント（20%）
    /----------\
   /   単体    \    ← 1つのクラス（70%）
  /--------------\
```

---

### 1. 単体テスト（Unit Test）

**対象:** 1つのクラス・メソッド

```java
@Test
void normalizedOrderBy_不正な値はデフォルト値() {
    // 1つのメソッドだけをテスト
    String result = service.normalizedOrderBy("invalid");
    assertThat(result).isEqualTo("created_at");
}
```

**特徴:**
- ⚡ 速い（数秒）
- ✅ 安定
- 💰 コストが低い

**割合:** 70%

---

### 2. 統合テスト（Integration Test）

**対象:** 複数のコンポーネント

```java
@Test
void 経費を提出できる() {
    // Controller + Service + Repository + DB
    mockMvc.perform(post("/expenses/{id}/submit", 32L)
            .with(httpBasic("hikaru@example.com", "pass1234")))
        .andExpect(status().isOk());
}
```

**特徴:**
- 🐢 遅い（数秒〜数十秒）
- ⚠️ 普通
- 💰💰 コストが中

**割合:** 20%

---

### 3. E2Eテスト（End-to-End Test）

**対象:** システム全体（ブラウザ操作）

```javascript
// Selenium や Playwright を使う
test('経費を提出できる', async () => {
    await page.goto('http://localhost:8080/login');
    await page.fill('#email', 'hikaru@example.com');
    await page.fill('#password', 'pass1234');
    await page.click('#login-button');
    await page.click('#submit-button');
    // ...
});
```

**特徴:**
- 🐌 とても遅い（数分）
- ❌ 不安定
- 💰💰💰 コストが高い

**割合:** 10%

---

## 🎯 なぜこのバランスが重要か？

### ❌ アンチパターン: E2Eテストばかり

```
すべてE2Eテストで確認
  ↓
実行に30分かかる
  ↓
開発者がテストを実行しなくなる
  ↓
バグが増える
  ↓
E2Eテストがさらに増える
  ↓
実行に1時間...
```

---

### ✅ 推奨パターン: ピラミッド型

```
単体テスト: 数秒で完了
  ↓
開発者が頻繁に実行
  ↓
バグを早期発見
  ↓
統合テストで連携確認
  ↓
E2Eテストで最終確認
  ↓
安心してデプロイ
```

---

## 📝 今日のまとめ

### わかったこと

✅ テストがないとバグが本番で発覚する
✅ テストがあればバグを開発中に発見できる
✅ テストはリファクタリングの安全網
✅ テストはドキュメント代わり
✅ テストの種類: 単体（70%）、統合（20%）、E2E（10%）

---

### 次のステップ

明日は実際にテストを書いてみます！

👉 [Day 2: 超シンプルなテストを書く](./day2_first_test.md)

---

## 🤔 理解度チェック

### Q1: テストを書くメリットは？

<details>
<summary>答えを見る</summary>

1. バグを早期発見
2. リファクタリングの安全性
3. ドキュメント代わり
4. 自信を持ってデプロイ

</details>

---

### Q2: テストピラミッドの割合は？

<details>
<summary>答えを見る</summary>

- 単体テスト: 70%
- 統合テスト: 20%
- E2Eテスト: 10%

</details>

---

### Q3: なぜE2Eテストばかり書くのは良くない？

<details>
<summary>答えを見る</summary>

- 実行が遅い（数分〜数十分）
- 開発者が実行しなくなる
- バグの発見が遅れる

</details>

---

**理解度: 3問中 ___問 正解**

- 3問正解: 完璧！次へ進みましょう 🎉
- 2問正解: 良好！もう一度読み直しましょう
- 0-1問正解: もう一度じっくり読みましょう

---

お疲れさまでした！明日は実際にテストを書いてみます！
