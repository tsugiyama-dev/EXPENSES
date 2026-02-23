# Day 2: 超シンプルなテストを書く ✍️

**学習時間:** 1.5時間
**難易度:** ⭐⭐☆☆☆

---

## 🎯 今日の目標

- 最もシンプルなテストを書ける
- JUnit 5の基本を理解する
- @Test アノテーションの使い方を覚える
- テストが通る/落ちるを体験する

---

## 📖 ステップバイステップで学ぶ

### ステップ1: テスト対象を見る 👀

まず、テストしたいコードを見てみましょう。

**ExpenseService.java** の `pageList` メソッド:

```java
private List<Integer> pageList(int currentPage, int totalPage, int displayPage) {
    int start = 0;
    int end = 0;

    if(totalPage < displayPage) {
        start = 1;
        end = totalPage;
    } else {
        start = Math.max(1, currentPage - 2);
        end = Math.min(totalPage, start + displayPage - 1);

        if(end == totalPage) {
            start = end - displayPage + 1;
        }
    }

    return java.util.stream.IntStream.rangeClosed(start, end)
            .boxed()
            .toList();
}
```

**このメソッドは何をするの？**

ページネーションの番号リストを生成します。

例:
- 現在ページ: 3
- 全体ページ: 10
- 表示ページ数: 5

→ `[1, 2, 3, 4, 5]` を返す

---

### ステップ2: テストクラスを作る 📁

**場所:** `src/test/java/com/example/expenses/service/ExpenseServicePageListTest.java`

```java
package com.example.expenses.service;

import org.junit.jupiter.api.Test;

class ExpenseServicePageListTest {

    // ここにテストを書く
}
```

**ポイント:**
- クラス名は `テスト対象 + Test`
- `src/test/java` の下に作る
- パッケージは対象と同じ

---

### ステップ3: 最もシンプルなテストを書く ✍️

```java
package com.example.expenses.service;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseServicePageListTest {

    @Test  // ← これがテストメソッドの印
    void test1() {
        // 1. テスト対象のオブジェクトを作る
        ExpenseService service = new ExpenseService(null, null, null, null, null);

        // 2. メソッドを呼ぶ（リフレクション使用）
        // ※ privateメソッドなので直接呼べない
        List<Integer> result = invokePageList(service, 1, 10, 5);

        // 3. 結果を確認
        assertEquals(5, result.size());  // サイズは5
        assertEquals(1, result.get(0));  // 最初は1
        assertEquals(5, result.get(4));  // 最後は5
    }

    // privateメソッドを呼ぶヘルパー
    private List<Integer> invokePageList(ExpenseService service, int currentPage, int totalPage, int displayPage) {
        try {
            var method = ExpenseService.class.getDeclaredMethod("pageList", int.class, int.class, int.class);
            method.setAccessible(true);
            return (List<Integer>) method.invoke(service, currentPage, totalPage, displayPage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

---

### ステップ4: テストを実行する 🏃

**コマンド:**
```bash
mvn test -Dtest=ExpenseServicePageListTest
```

**結果:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.expenses.service.ExpenseServicePageListTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] ✅ BUILD SUCCESS
```

**おめでとうございます！最初のテストが通りました！** 🎉

---

### ステップ5: わざと失敗させてみる 🔥

テストを変更して、わざと失敗させてみましょう。

```java
@Test
void test1() {
    ExpenseService service = new ExpenseService(null, null, null, null, null);
    List<Integer> result = invokePageList(service, 1, 10, 5);

    // わざと間違った値で検証
    assertEquals(10, result.size());  // ← 実際は5なのに10を期待
}
```

**実行:**
```bash
mvn test -Dtest=ExpenseServicePageListTest
```

**結果:**
```
[ERROR] test1  Time elapsed: 0.023 s  <<< FAILURE!
org.opentest4j.AssertionFailedError:
    expected: <10> but was: <5>
```

**学んだこと:**
- テストが失敗すると「expected（期待値）」と「actual（実際の値）」が表示される
- `assertEquals(期待値, 実際の値)` の順番が重要

---

## 🧪 JUnit 5の基本

### @Test アノテーション

```java
@Test  // ← このメソッドがテストであることを示す
void テストメソッド名() {
    // テストコード
}
```

**ルール:**
- メソッド名は自由（日本語OK）
- 戻り値は `void`
- 引数なし

---

### よく使うアサーション（検証）

#### 1. assertEquals - 値が等しいか

```java
@Test
void 足し算のテスト() {
    int result = 1 + 1;
    assertEquals(2, result);  // ← 2と等しいか
}
```

---

#### 2. assertTrue / assertFalse - 真偽値

```java
@Test
void 空文字チェック() {
    String str = "";
    assertTrue(str.isEmpty());  // ← trueか
}

@Test
void 非空文字チェック() {
    String str = "hello";
    assertFalse(str.isEmpty());  // ← falseか
}
```

---

#### 3. assertNull / assertNotNull - null チェック

```java
@Test
void nullチェック() {
    String str = null;
    assertNull(str);  // ← nullか
}

@Test
void 非nullチェック() {
    String str = "hello";
    assertNotNull(str);  // ← nullでないか
}
```

---

#### 4. assertThrows - 例外のテスト

```java
@Test
void ゼロ除算でエラー() {
    assertThrows(ArithmeticException.class, () -> {
        int result = 10 / 0;  // ← 例外が発生する
    });
}
```

---

## 📝 演習問題

### 演習1: 基本のテスト

**問題:** `pageList` メソッドのテストを書いてください。

**条件:**
- currentPage = 5
- totalPage = 10
- displayPage = 5

**期待値:**
- リストのサイズは5
- 最初の要素は 3
- 最後の要素は 7

<details>
<summary>ヒント</summary>

```java
@Test
void 中間ページのテスト() {
    // Given
    ExpenseService service = new ExpenseService(null, null, null, null, null);

    // When
    List<Integer> result = invokePageList(service, 5, 10, 5);

    // Then
    assertEquals(?, result.size());
    assertEquals(?, result.get(0));
    assertEquals(?, result.get(4));
}
```

</details>

<details>
<summary>解答</summary>

```java
@Test
void 中間ページのテスト() {
    // Given
    ExpenseService service = new ExpenseService(null, null, null, null, null);

    // When
    List<Integer> result = invokePageList(service, 5, 10, 5);

    // Then
    assertEquals(5, result.size());
    assertEquals(3, result.get(0));  // [3, 4, 5, 6, 7]
    assertEquals(7, result.get(4));
}
```

</details>

---

### 演習2: 境界値のテスト

**問題:** 最初のページのテストを書いてください。

**条件:**
- currentPage = 1
- totalPage = 10
- displayPage = 5

**期待値:**
- リストのサイズは5
- 最初の要素は 1
- 最後の要素は 5

<details>
<summary>解答</summary>

```java
@Test
void 最初のページのテスト() {
    // Given
    ExpenseService service = new ExpenseService(null, null, null, null, null);

    // When
    List<Integer> result = invokePageList(service, 1, 10, 5);

    // Then
    assertEquals(5, result.size());
    assertEquals(1, result.get(0));
    assertEquals(5, result.get(4));
}
```

</details>

---

### 演習3: エッジケースのテスト

**問題:** 最後のページのテストを書いてください。

**条件:**
- currentPage = 10
- totalPage = 10
- displayPage = 5

**期待値:**
- リストのサイズは5
- 最初の要素は 6
- 最後の要素は 10

<details>
<summary>解答</summary>

```java
@Test
void 最後のページのテスト() {
    // Given
    ExpenseService service = new ExpenseService(null, null, null, null, null);

    // When
    List<Integer> result = invokePageList(service, 10, 10, 5);

    // Then
    assertEquals(5, result.size());
    assertEquals(6, result.get(0));  // [6, 7, 8, 9, 10]
    assertEquals(10, result.get(4));
}
```

</details>

---

### 演習4: 総ページ数が少ない場合

**問題:** 総ページ数が表示ページ数より少ない場合のテストを書いてください。

**条件:**
- currentPage = 1
- totalPage = 3
- displayPage = 5

**期待値:**
- リストのサイズは3
- 最初の要素は 1
- 最後の要素は 3

<details>
<summary>解答</summary>

```java
@Test
void 総ページ数が少ない場合のテスト() {
    // Given
    ExpenseService service = new ExpenseService(null, null, null, null, null);

    // When
    List<Integer> result = invokePageList(service, 1, 3, 5);

    // Then
    assertEquals(3, result.size());
    assertEquals(1, result.get(0));  // [1, 2, 3]
    assertEquals(3, result.get(2));
}
```

</details>

---

## 🎓 完全版のテストクラス

すべての演習問題を含む完全版:

```java
package com.example.expenses.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExpenseService.pageList のテスト")
class ExpenseServicePageListTest {

    private ExpenseService service;
    private Method pageListMethod;

    @BeforeEach
    void setUp() throws Exception {
        service = new ExpenseService(null, null, null, null, null);
        pageListMethod = ExpenseService.class.getDeclaredMethod("pageList", int.class, int.class, int.class);
        pageListMethod.setAccessible(true);
    }

    @Test
    @DisplayName("最初のページ（1ページ目）")
    void 最初のページ() throws Exception {
        // When
        List<Integer> result = invokePageList(1, 10, 5);

        // Then
        assertEquals(5, result.size());
        assertEquals(1, result.get(0));
        assertEquals(5, result.get(4));
    }

    @Test
    @DisplayName("中間のページ（5ページ目）")
    void 中間のページ() throws Exception {
        // When
        List<Integer> result = invokePageList(5, 10, 5);

        // Then
        assertEquals(5, result.size());
        assertEquals(3, result.get(0));
        assertEquals(7, result.get(4));
    }

    @Test
    @DisplayName("最後のページ（10ページ目）")
    void 最後のページ() throws Exception {
        // When
        List<Integer> result = invokePageList(10, 10, 5);

        // Then
        assertEquals(5, result.size());
        assertEquals(6, result.get(0));
        assertEquals(10, result.get(4));
    }

    @Test
    @DisplayName("総ページ数が表示ページ数より少ない")
    void 総ページ数が少ない() throws Exception {
        // When
        List<Integer> result = invokePageList(1, 3, 5);

        // Then
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(3, result.get(2));
    }

    private List<Integer> invokePageList(int currentPage, int totalPage, int displayPage) throws Exception {
        return (List<Integer>) pageListMethod.invoke(service, currentPage, totalPage, displayPage);
    }
}
```

---

## 📝 今日のまとめ

### できるようになったこと

✅ テストクラスを作成できる
✅ @Test アノテーションを使える
✅ assertEquals で検証できる
✅ テストを実行できる
✅ テストが失敗したときのエラーメッセージを読める

---

### よくある間違い

#### ❌ 間違い1: assertEquals の順番が逆

```java
// ❌ 間違い
assertEquals(result, 5);

// ✅ 正しい
assertEquals(5, result);

// 順番: assertEquals(期待値, 実際の値)
```

---

#### ❌ 間違い2: @Test を忘れる

```java
// ❌ 間違い（実行されない）
void test1() {
    // ...
}

// ✅ 正しい
@Test
void test1() {
    // ...
}
```

---

#### ❌ 間違い3: assertEqualsのスペルミス

```java
// ❌ 間違い
asserEquals(5, result);  // t が1つ足りない

// ✅ 正しい
assertEquals(5, result);
```

---

### 次のステップ

明日はAssertJを使って、もっと読みやすいテストを書きます！

👉 [Day 3: AssertJで検証する](./day3_assertj.md)

---

## 🤔 理解度チェック

### Q1: テストメソッドに必要なアノテーションは？

<details>
<summary>答えを見る</summary>

`@Test`

</details>

---

### Q2: assertEquals の正しい順番は？

<details>
<summary>答えを見る</summary>

```java
assertEquals(期待値, 実際の値);
```

</details>

---

### Q3: テストクラスはどこに作る？

<details>
<summary>答えを見る</summary>

`src/test/java` の下に、対象クラスと同じパッケージで作る

</details>

---

**理解度: 3問中 ___問 正解**

- 3問正解: 完璧！次へ進みましょう 🎉
- 2問正解: 良好！もう一度読み直しましょう
- 0-1問正解: もう一度じっくり読みましょう

---

お疲れさまでした！明日はAssertJを学びます！
