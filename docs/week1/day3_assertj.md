# Day 3: AssertJで検証する 🔍

**学習時間:** 1.5時間
**難易度:** ⭐⭐☆☆☆

---

## 🎯 今日の目標

- AssertJとは何かを理解する
- assertThat().isEqualTo() を使える
- よく使うアサーションメソッドを覚える
- エラーメッセージをわかりやすくする

---

## 📖 AssertJとは？

### JUnit の assertEquals vs AssertJ の assertThat

**JUnit 5 の場合:**
```java
assertEquals(5, result.size());
assertEquals("created_at", result);
assertTrue(result.isEmpty());
```

**AssertJ の場合:**
```java
assertThat(result.size()).isEqualTo(5);
assertThat(result).isEqualTo("created_at");
assertThat(result).isEmpty();
```

**違いは？**
- AssertJの方が**自然な英語**
- AssertJの方が**エラーメッセージがわかりやすい**
- AssertJの方が**メソッドチェーンが使える**

---

## 🧪 AssertJの基本

### 1. 基本の検証

```java
import static org.assertj.core.api.Assertions.*;

@Test
void 基本の検証() {
    String result = "created_at";

    // 値が等しい
    assertThat(result).isEqualTo("created_at");

    // 値が等しくない
    assertThat(result).isNotEqualTo("invalid");

    // nullではない
    assertThat(result).isNotNull();

    // nullである
    String nullValue = null;
    assertThat(nullValue).isNull();
}
```

---

### 2. 文字列の検証

```java
@Test
void 文字列の検証() {
    String result = "created_at";

    // 特定の文字列を含む
    assertThat(result).contains("created");

    // 特定の文字列で始まる
    assertThat(result).startsWith("created");

    // 特定の文字列で終わる
    assertThat(result).endsWith("_at");

    // 空文字列ではない
    assertThat(result).isNotEmpty();

    // ブランクではない
    assertThat(result).isNotBlank();
}
```

---

### 3. 数値の検証

```java
@Test
void 数値の検証() {
    int result = 5;

    // 等しい
    assertThat(result).isEqualTo(5);

    // より大きい
    assertThat(result).isGreaterThan(3);

    // 以上
    assertThat(result).isGreaterThanOrEqualTo(5);

    // より小さい
    assertThat(result).isLessThan(10);

    // 以下
    assertThat(result).isLessThanOrEqualTo(5);

    // 範囲内
    assertThat(result).isBetween(1, 10);
}
```

---

### 4. コレクションの検証

```java
@Test
void コレクションの検証() {
    List<Integer> result = List.of(1, 2, 3, 4, 5);

    // サイズ
    assertThat(result).hasSize(5);

    // 要素を含む
    assertThat(result).contains(3);

    // 複数の要素を含む
    assertThat(result).contains(1, 2, 3);

    // 要素を含まない
    assertThat(result).doesNotContain(6);

    // 空ではない
    assertThat(result).isNotEmpty();

    // 最初の要素
    assertThat(result).first().isEqualTo(1);

    // 最後の要素
    assertThat(result).last().isEqualTo(5);
}
```

---

### 5. boolean の検証

```java
@Test
void boolean の検証() {
    boolean result = true;

    // trueである
    assertThat(result).isTrue();

    // falseである
    assertThat(!result).isFalse();
}
```

---

## 📝 実例: normalizedOrderBy のテスト

### テスト対象のコード

```java
private String normalizedOrderBy(String sort) {
    if(sort == null || sort.isBlank()) {
        return "created_at";
    }

    String[] parts = sort.split(",");
    String key = parts[0].trim();

    String column = switch(key) {
        case "created_at" -> "created_at";
        case "updated_at" -> "updated_at";
        case "submitted_at" -> "submitted_at";
        case "amount" -> "amount";
        case "id" -> "id";
        default -> "created_at";
    };

    if(!ALLOWED_SORTS.contains(column)) {
        return "created_at";
    }
    return column;
}
```

---

### AssertJを使ったテスト

```java
import static org.assertj.core.api.Assertions.*;

@Test
@DisplayName("nullの場合はcreated_atを返す")
void nullの場合はデフォルト値() throws Exception {
    // Given
    String sort = null;

    // When
    String result = invokeNormalizedOrderBy(sort);

    // Then
    assertThat(result).isEqualTo("created_at");
}

@Test
@DisplayName("空文字の場合はcreated_atを返す")
void 空文字の場合はデフォルト値() throws Exception {
    // Given
    String sort = "";

    // When
    String result = invokeNormalizedOrderBy(sort);

    // Then
    assertThat(result).isEqualTo("created_at");
}

@Test
@DisplayName("不正な値の場合はcreated_atを返す")
void 不正な値の場合はデフォルト値() throws Exception {
    // Given
    String sort = "invalid";

    // When
    String result = invokeNormalizedOrderBy(sort);

    // Then
    assertThat(result)
        .isNotNull()
        .isEqualTo("created_at");
}

@Test
@DisplayName("有効な値の場合はそのまま返す")
void 有効な値の場合() throws Exception {
    // Given
    String sort = "amount";

    // When
    String result = invokeNormalizedOrderBy(sort);

    // Then
    assertThat(result)
        .isNotNull()
        .isEqualTo("amount")
        .isNotEqualTo("created_at");
}
```

---

## 🎓 エラーメッセージの違い

### JUnit 5 の場合

```java
assertEquals("amount", result);
```

**エラーメッセージ:**
```
expected: <amount> but was: <created_at>
```

---

### AssertJ の場合

```java
assertThat(result).isEqualTo("amount");
```

**エラーメッセージ:**
```
Expecting:
 <"created_at">
to be equal to:
 <"amount">
but was not.
```

**AssertJの方がわかりやすい！**

---

## 📝 演習問題

### 演習1: normalizedDirection のテスト

**テスト対象:**
```java
private String normalizedDirection(String sort) {
    if(sort == null || sort.isBlank()) return "DESC";

    String[] parts = sort.split(",");
    if(parts.length < 2) return "DESC";

    String dir = parts[1].trim();
    return "asc".equalsIgnoreCase(dir) ? "ASC" : "DESC";
}
```

**問題:** 以下のテストケースを書いてください。

1. `null` → `"DESC"`
2. `"created_at"` (カンマなし) → `"DESC"`
3. `"created_at,asc"` → `"ASC"`
4. `"created_at,desc"` → `"DESC"`
5. `"created_at,invalid"` → `"DESC"`

<details>
<summary>解答</summary>

```java
@Test
@DisplayName("nullの場合はDESCを返す")
void nullの場合はDESC() throws Exception {
    String result = invokeNormalizedDirection(null);
    assertThat(result).isEqualTo("DESC");
}

@Test
@DisplayName("カンマがない場合はDESCを返す")
void カンマがない場合はDESC() throws Exception {
    String result = invokeNormalizedDirection("created_at");
    assertThat(result).isEqualTo("DESC");
}

@Test
@DisplayName("ascの場合はASCを返す")
void ascの場合はASC() throws Exception {
    String result = invokeNormalizedDirection("created_at,asc");
    assertThat(result).isEqualTo("ASC");
}

@Test
@DisplayName("descの場合はDESCを返す")
void descの場合はDESC() throws Exception {
    String result = invokeNormalizedDirection("created_at,desc");
    assertThat(result).isEqualTo("DESC");
}

@Test
@DisplayName("不正な値の場合はDESCを返す")
void 不正な値の場合はDESC() throws Exception {
    String result = invokeNormalizedDirection("created_at,invalid");
    assertThat(result).isEqualTo("DESC");
}
```

</details>

---

## 📝 今日のまとめ

### できるようになったこと

✅ AssertJの基本を理解した
✅ assertThat().isEqualTo() を使える
✅ 文字列、数値、コレクションの検証ができる
✅ エラーメッセージが読みやすくなった

---

### よく使うメソッド一覧

```java
// 基本
assertThat(value).isEqualTo(expected);
assertThat(value).isNotEqualTo(expected);
assertThat(value).isNull();
assertThat(value).isNotNull();

// 文字列
assertThat(str).contains("text");
assertThat(str).startsWith("text");
assertThat(str).endsWith("text");
assertThat(str).isEmpty();

// 数値
assertThat(num).isGreaterThan(5);
assertThat(num).isLessThan(10);
assertThat(num).isBetween(1, 10);

// コレクション
assertThat(list).hasSize(5);
assertThat(list).contains(element);
assertThat(list).isEmpty();

// boolean
assertThat(bool).isTrue();
assertThat(bool).isFalse();
```

---

### 次のステップ

明日はHTTPリクエストのテストを書きます！

👉 [Day 4: HTTPリクエストのテスト](./day4_http_test.md)

---

お疲れさまでした！
