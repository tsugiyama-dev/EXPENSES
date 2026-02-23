# Day 5: テストの整理とリファクタリング 🧹

**学習時間:** 2時間
**難易度:** ⭐⭐⭐☆☆

---

## 🎯 今日の目標

- @Nested でグループ化できる
- @DisplayName で日本語のテスト名が書ける
- @BeforeEach でテストデータを準備できる
- Given-When-Thenパターンを使える

---

## 📖 現在のテストコードの問題点

### ❌ Before（読みにくい）

```java
class ExpenseServiceTest {
    @Test
    void check_403() throws Exception {
        long expenseId = 32L;
        mockMvc.perform(post("/expenses/{id}/submit", expenseId)
                .with(httpBasic("hikaru@example.com", "pass1234")))
        .andExpect(status().isConflict());
    }

    @Test
    void check_409() throws Exception {
        long expenseId = 32L;
        mockMvc.perform(post("/expenses/{id}/approve", expenseId)
                .with(httpBasic("approver@example.com", "1234")))
        .andExpect(status().isConflict());
    }

    @Test
    void check_400() throws Exception {
        // ...
    }
}
```

**問題点:**
- テスト名が意味不明（check_403って何？）
- グループ化されていない
- マジックナンバーだらけ（32L, 29Lって何？）
- Given-When-Thenがない

---

## ✅ After（読みやすい）

```java
@DisplayName("経費申請API")
class ExpenseServiceTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("経費提出API")
    class SubmitTest {

        private long draftExpenseId;
        private long submittedExpenseId;

        @BeforeEach
        void setUp() {
            // テストデータの準備
            draftExpenseId = 32L;      // 下書き
            submittedExpenseId = 29L;  // 提出済み
        }

        @Test
        @DisplayName("下書きの経費を提出できる")
        void 下書きの経費を提出できる() throws Exception {
            // Given: 下書きの経費
            long expenseId = draftExpenseId;

            // When: 本人が提出
            var result = mockMvc.perform(
                post("/expenses/{id}/submit", expenseId)
                    .with(httpBasic("hikaru@example.com", "pass1234"))
            );

            // Then: 成功
            result.andExpect(status().isOk())
                  .andExpect(jsonPath("$.status").value("SUBMITTED"));
        }

        @Test
        @DisplayName("本人以外が提出すると409エラー")
        void 本人以外が提出すると409エラー() throws Exception {
            // Given: hikaru さんの経費
            long expenseId = draftExpenseId;

            // When: yasuko さんが提出
            var result = mockMvc.perform(
                post("/expenses/{id}/submit", expenseId)
                    .with(httpBasic("yasuko@example.com", "pass1234"))
            );

            // Then: 409エラー
            result.andExpect(status().isConflict())
                  .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
        }
    }

    @Nested
    @DisplayName("経費承認API")
    class ApproveTest {
        // 承認関連のテスト
    }

    @Nested
    @DisplayName("経費却下API")
    class RejectTest {
        // 却下関連のテスト
    }
}
```

---

## 🔧 リファクタリング手法

### 1. @Nested でグループ化

```java
@DisplayName("経費申請API")
class ExpenseServiceTest {

    @Nested
    @DisplayName("経費提出API")
    class SubmitTest {
        // 提出関連のテスト
    }

    @Nested
    @DisplayName("経費承認API")
    class ApproveTest {
        // 承認関連のテスト
    }

    @Nested
    @DisplayName("経費却下API")
    class RejectTest {
        // 却下関連のテスト
    }
}
```

**メリット:**
- テストが機能ごとにグループ化される
- テストレポートが見やすくなる
- テストの整理がしやすい

---

### 2. @DisplayName で日本語のテスト名

```java
@Test
@DisplayName("下書きの経費を提出できる")
void 下書きの経費を提出できる() {
    // ...
}
```

**メリット:**
- テストの意図が一目でわかる
- ドキュメント代わりになる
- テストレポートがわかりやすい

---

### 3. @BeforeEach でテストデータを準備

```java
@Nested
class SubmitTest {

    private long draftExpenseId;
    private long submittedExpenseId;

    @BeforeEach
    void setUp() {
        // 各テストの前に実行される
        draftExpenseId = 32L;
        submittedExpenseId = 29L;
    }

    @Test
    void test1() {
        // draftExpenseId が使える
    }

    @Test
    void test2() {
        // draftExpenseId が使える
    }
}
```

**メリット:**
- テストデータの重複を削減
- テストコードがシンプルになる
- メンテナンスしやすい

---

### 4. Given-When-Thenパターン

```java
@Test
void テスト() {
    // Given: 前提条件（テストに必要なデータを準備）
    long expenseId = 32L;
    String user = "hikaru@example.com";

    // When: 実行（テスト対象のメソッドを呼ぶ）
    var result = mockMvc.perform(
        post("/expenses/{id}/submit", expenseId)
            .with(httpBasic(user, "pass1234"))
    );

    // Then: 検証（結果を確認）
    result.andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("SUBMITTED"));
}
```

**メリット:**
- テストの構造がわかりやすい
- 読みやすい
- メンテナンスしやすい

---

## 📝 演習問題: 既存のテストをリファクタリング

### Before（現在のコード）

```java
class ExpenseServiceTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void check_403() throws Exception {
        long expenseId = 32L;
        mockMvc.perform(post("/expenses/{id}/submit", expenseId)
                .with(httpBasic("hikaru@example.com", "pass1234")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
    }

    @Test
    void check_409() throws Exception {
        long expenseId = 32L;
        mockMvc.perform(post("/expenses/{id}/approve", expenseId)
                .with(httpBasic("approver@example.com", "1234")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("提出済み以外は承認できません"));
    }

    @Test
    void check_404() throws Exception {
        mockMvc.perform(post("/expenses/{id}/submit", 9999)
                .with(httpBasic("hikaru@example.com","pass1234")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
    }
}
```

### After（リファクタリング後）

<details>
<summary>解答</summary>

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("経費申請API")
class ExpenseServiceTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("経費提出API")
    class SubmitTest {

        private long draftExpenseId;
        private long notExistExpenseId;

        @BeforeEach
        void setUp() {
            draftExpenseId = 32L;       // 下書き
            notExistExpenseId = 9999L;  // 存在しないID
        }

        @Test
        @DisplayName("本人以外が提出すると409エラー")
        void 本人以外が提出すると409エラー() throws Exception {
            // Given: hikaru さんの経費
            long expenseId = draftExpenseId;

            // When: yasuko さんが提出
            var result = mockMvc.perform(
                post("/expenses/{id}/submit", expenseId)
                    .with(httpBasic("yasuko@example.com", "pass1234"))
            );

            // Then: 409エラー
            result.andExpect(status().isConflict())
                  .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
        }

        @Test
        @DisplayName("存在しない経費を提出すると404エラー")
        void 存在しない経費を提出すると404エラー() throws Exception {
            // Given: 存在しないID
            long expenseId = notExistExpenseId;

            // When: 提出
            var result = mockMvc.perform(
                post("/expenses/{id}/submit", expenseId)
                    .with(httpBasic("hikaru@example.com", "pass1234"))
            );

            // Then: 404エラー
            result.andExpect(status().isNotFound())
                  .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
        }
    }

    @Nested
    @DisplayName("経費承認API")
    class ApproveTest {

        private long draftExpenseId;

        @BeforeEach
        void setUp() {
            draftExpenseId = 32L;  // 下書き
        }

        @Test
        @DisplayName("提出済み以外を承認すると409エラー")
        void 提出済み以外を承認すると409エラー() throws Exception {
            // Given: 下書きの経費
            long expenseId = draftExpenseId;

            // When: 承認者が承認
            var result = mockMvc.perform(
                post("/expenses/{id}/approve", expenseId)
                    .with(httpBasic("approver@example.com", "1234"))
            );

            // Then: 409エラー
            result.andExpect(status().isConflict())
                  .andExpect(jsonPath("$.message").value("提出済み以外は承認できません"));
        }
    }
}
```

</details>

---

## 📝 今日のまとめ

### できるようになったこと

✅ @Nested でテストをグループ化できる
✅ @DisplayName で日本語のテスト名が書ける
✅ @BeforeEach でテストデータを準備できる
✅ Given-When-Thenパターンを使える

---

### リファクタリングチェックリスト

- [ ] テスト名が意味を持っているか？
- [ ] @Nested でグループ化されているか？
- [ ] @DisplayName で説明されているか？
- [ ] マジックナンバーに変数名が付いているか？
- [ ] Given-When-Thenパターンを使っているか？
- [ ] 重複コードが削減されているか？

---

### 次のステップ

明日はエラーケースのテストを学びます！

👉 [Day 6: エラーケースのテスト](./day6_error_test.md)

---

お疲れさまでした！
