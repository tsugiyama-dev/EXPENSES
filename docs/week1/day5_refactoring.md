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

**現在のExpenseServiceTest.javaの状態:**

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseServiceTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void unAuthentcted_status_401() throws Exception {
        long expenseId = 99L;
        mockMvc.perform(post("/expenses/{id}/submit", expenseId))
            .andExpect(status().isUnauthorized());
    }

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
    void check_400() throws Exception {
        String json = """
            {
                "reason":""
            }
            """;
        long expenseId = 999L;
        mockMvc.perform(post("/expenses/{id}/reject", expenseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(httpBasic("approver@example.com", "1234")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
    }

    @Test
    void check_404() throws Exception {
        mockMvc.perform(post("/expenses/{id}/submit", 9999)
                .with(httpBasic("hikaru@example.com","pass1234")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
    }

    @Test
    void check_200_status() throws Exception {
        long id = 29L;
        String json = """
            {
                "reason":"申請期限の締め切り日が過ぎているため承認/申請できません"
            }
            """;
        mockMvc.perform(post("/expenses/{id}/reject", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(httpBasic("approver@example.com", "1234")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
```

**問題点:**
- テスト名が意味不明（check_403, check_409, unAuthentcted_status_401）
- マジックナンバーだらけ（32L, 99L, 999L, 29L）
- グループ化されていない
- Given-When-Thenパターンがない
- コメントアウトされたコードが大量（上記には省略）

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
        private long anyExpenseId;

        @BeforeEach
        void setUp() {
            draftExpenseId = 32L;       // hikaru さんの下書き
            notExistExpenseId = 9999L;  // 存在しないID
            anyExpenseId = 99L;         // 任意のID
        }

        @Test
        @DisplayName("認証なしで提出すると401エラー")
        void 認証なしで提出すると401エラー() throws Exception {
            // Given: 任意の経費ID
            long expenseId = anyExpenseId;

            // When: 認証なしで提出
            var result = mockMvc.perform(
                post("/expenses/{id}/submit", expenseId)
            );

            // Then: 401エラー
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("本人以外が提出すると409エラー")
        void 本人以外が提出すると409エラー() throws Exception {
            // Given: hikaru さんの経費
            long expenseId = draftExpenseId;

            // When: hikaru さん本人が提出しようとする（テストデータの都合で409になる）
            var result = mockMvc.perform(
                post("/expenses/{id}/submit", expenseId)
                    .with(httpBasic("hikaru@example.com", "pass1234"))
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

    @Nested
    @DisplayName("経費却下API")
    class RejectTest {

        private long submittedExpenseId;
        private long invalidExpenseId;

        @BeforeEach
        void setUp() {
            submittedExpenseId = 29L;  // 提出済み
            invalidExpenseId = 999L;   // 存在しないor不正なID
        }

        @Test
        @DisplayName("却下理由が空の場合は400エラー")
        void 却下理由が空の場合は400エラー() throws Exception {
            // Given: 空の却下理由
            String json = """
                {
                    "reason": ""
                }
                """;
            long expenseId = invalidExpenseId;

            // When: 却下
            var result = mockMvc.perform(
                post("/expenses/{id}/reject", expenseId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
                    .with(httpBasic("approver@example.com", "1234"))
            );

            // Then: 400エラー
            result.andExpect(status().isBadRequest())
                  .andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
        }

        @Test
        @DisplayName("提出済みの経費を却下できる")
        void 提出済みの経費を却下できる() throws Exception {
            // Given: 提出済みの経費と却下理由
            long expenseId = submittedExpenseId;
            String json = """
                {
                    "reason": "申請期限の締め切り日が過ぎているため承認/申請できません"
                }
                """;

            // When: 承認者が却下
            var result = mockMvc.perform(
                post("/expenses/{id}/reject", expenseId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
                    .with(httpBasic("approver@example.com", "1234"))
            );

            // Then: 成功
            result.andExpect(status().isOk())
                  .andExpect(jsonPath("$.status").value("REJECTED"));
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
