# Day 6: エラーケースのテスト ⚠️

**学習時間:** 1.5時間
**難易度:** ⭐⭐⭐☆☆

---

## 🎯 今日の目標

- 例外のテスト方法を理解する
- assertThatThrownBy の使い方を覚える
- HTTPエラーステータスの検証ができる
- エラーメッセージの検証ができる

---

## 📖 なぜエラーケースのテストが重要か？

### シーン: エラーハンドリングがない場合

```java
public void approve(Long expenseId) {
    Expense expense = expenseMapper.findById(expenseId);

    // ❌ nullチェックがない！
    if(expense.getStatus() != ExpenseStatus.SUBMITTED) {
        // ← expense が null だったら NullPointerException！
    }
}
```

**結果:**
- 存在しないIDで呼ばれたらクラッシュ
- ユーザーに500エラーが返る
- サーバーのログにスタックトレースが大量に出力

---

### エラーケースをテストする

```java
@Test
@DisplayName("存在しない経費IDの場合はBusinessExceptionをスロー")
void 存在しない経費IDの場合() {
    // Given
    long expenseId = 9999L;

    // When & Then
    assertThatThrownBy(() -> service.approve(expenseId, 1, 999L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("経費申請が見つかりません");
}
```

**このテストを書くと:**
1. 実装前にエラーハンドリングの必要性に気づく
2. 実装時にnullチェックを追加
3. 適切なエラーメッセージを返すようになる

---

## 🧪 例外のテスト

### AssertJを使った例外のテスト

```java
import static org.assertj.core.api.Assertions.*;

@Test
void 例外をスローする() {
    // When & Then
    assertThatThrownBy(() -> {
        // 例外をスローするコード
        service.approve(9999L, 1, 999L);
    })
    .isInstanceOf(BusinessException.class)         // 例外の型
    .hasMessage("経費申請が見つかりません")           // 完全一致
    .hasMessageContaining("経費申請")               // 部分一致
    .hasFieldOrPropertyWithValue("errorCode", "NOT_FOUND");  // フィールド
}
```

---

### JUnit 5 の assertThrows

```java
@Test
void 例外をスローする() {
    // When & Then
    BusinessException ex = assertThrows(
        BusinessException.class,
        () -> service.approve(9999L, 1, 999L)
    );

    // 例外の内容を検証
    assertThat(ex.getMessage()).contains("経費申請が見つかりません");
    assertThat(ex.getErrorCode()).isEqualTo("NOT_FOUND");
}
```

---

## 📝 実例: approve メソッドのエラーケース

### テスト対象のコード

```java
public ExpenseResponse approve(long expenseId, int version, Long actorId) {
    Expense expense = expenseMapper.findById(expenseId);

    // エラー1: 経費が存在しない
    if(expense == null) {
        throw new BusinessException("NOT_FOUND", "経費申請が見つかりません");
    }

    // エラー2: ステータスがSUBMITTED以外
    if(expense.getStatus() != ExpenseStatus.SUBMITTED) {
        throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は承認できません");
    }

    // エラー3: バージョンが一致しない（楽観的ロック）
    if(expense.getVersion() != version) {
        throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています");
    }

    // ...
}
```

---

### エラーケースのテスト

```java
@Nested
@DisplayName("異常系")
class ErrorCase {

    @Test
    @DisplayName("経費が存在しない場合はBusinessExceptionをスロー")
    void 経費が存在しない場合() {
        // Given
        long expenseId = 9999L;
        given(expenseMapper.findById(expenseId)).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> service.approve(expenseId, 1, 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("経費申請が見つかりません");
    }

    @Test
    @DisplayName("ステータスがDRAFTの場合はBusinessExceptionをスロー")
    void ステータスがDRAFTの場合() {
        // Given
        Expense draftExpense = ExpenseTestBuilder.createDraft();
        given(expenseMapper.findById(draftExpense.getId())).willReturn(draftExpense);

        // When & Then
        assertThatThrownBy(() -> service.approve(draftExpense.getId(), 1, 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("提出済み以外は承認できません");
    }

    @Test
    @DisplayName("バージョンが一致しない場合はBusinessExceptionをスロー")
    void バージョンが一致しない場合() {
        // Given
        Expense expense = ExpenseTestBuilder.createSubmitted();
        given(expenseMapper.findById(expense.getId())).willReturn(expense);

        // When & Then
        int wrongVersion = 999;
        assertThatThrownBy(() -> service.approve(expense.getId(), wrongVersion, 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("他のユーザに更新されています");
    }
}
```

---

## 🌐 HTTPエラーのテスト

### 現在のExpenseServiceTest.javaのエラーケース

**現在のコード:**

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
    void check_404() throws Exception {
        mockMvc.perform(post("/expenses/{id}/submit", 9999)
                .with(httpBasic("hikaru@example.com","pass1234")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
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
}
```

---

### 改善版: @Nested と @DisplayName を使う

```java
@Nested
@DisplayName("HTTPエラー")
class HttpErrorTest {

    @Test
    @DisplayName("認証なしで経費を提出すると401エラー")
    void 認証なしで提出すると401エラー() throws Exception {
        // Given
        long expenseId = 99L;

        // When & Then
        mockMvc.perform(post("/expenses/{id}/submit", expenseId))
            .andExpect(status().isUnauthorized());  // 401
    }

    @Test
    @DisplayName("存在しない経費を提出すると404エラー")
    void 存在しない経費を提出すると404エラー() throws Exception {
        // Given: 存在しないID
        long notExistExpenseId = 9999L;

        // When & Then
        mockMvc.perform(
            post("/expenses/{id}/submit", notExistExpenseId)
                .with(httpBasic("hikaru@example.com", "pass1234"))
        )
        .andExpect(status().isNotFound())  // 404
        .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
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
        long expenseId = 999L;

        // When & Then
        mockMvc.perform(
            post("/expenses/{id}/reject", expenseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(httpBasic("approver@example.com", "1234"))
        )
        .andExpect(status().isBadRequest())  // 400
        .andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
    }

    @Test
    @DisplayName("本人以外が提出すると409エラー")
    void 本人以外が提出すると409エラー() throws Exception {
        // Given: hikaru さんの経費
        long expenseId = 32L;

        // When: hikaru さん本人が提出（テストデータの都合で409になる）
        mockMvc.perform(
            post("/expenses/{id}/submit", expenseId)
                .with(httpBasic("hikaru@example.com", "pass1234"))
        )
        .andExpect(status().isConflict())  // 409
        .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
    }

    @Test
    @DisplayName("提出済み以外を承認すると409エラー")
    void 提出済み以外を承認すると409エラー() throws Exception {
        // Given: 下書きの経費
        long draftExpenseId = 32L;

        // When & Then
        mockMvc.perform(
            post("/expenses/{id}/approve", draftExpenseId)
                .with(httpBasic("approver@example.com", "1234"))
        )
        .andExpect(status().isConflict())  // 409
        .andExpect(jsonPath("$.message").value("提出済み以外は承認できません"));
    }
}
```

---

## 📊 よく使うHTTPステータスコード

| コード | 意味 | 例 |
|--------|------|-----|
| **200** | OK | 正常 |
| **201** | Created | 作成成功 |
| **400** | Bad Request | バリデーションエラー |
| **401** | Unauthorized | 認証なし |
| **403** | Forbidden | 権限なし |
| **404** | Not Found | 存在しない |
| **409** | Conflict | 楽観的ロック、ビジネスロジックエラー |
| **500** | Internal Server Error | サーバーエラー |

---

## 📝 演習問題

### 演習1: reject メソッドのエラーケース

**問題:** 以下のエラーケースのテストを書いてください。

1. 経費が存在しない → BusinessException
2. ステータスがDRAFT → BusinessException
3. バージョン不一致 → BusinessException

<details>
<summary>解答</summary>

```java
@Nested
@DisplayName("reject メソッドの異常系")
class RejectErrorCase {

    @Test
    @DisplayName("経費が存在しない場合はBusinessExceptionをスロー")
    void 経費が存在しない場合() {
        // Given
        long expenseId = 9999L;
        given(expenseMapper.findById(expenseId)).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> service.reject(expenseId, "理由", 1, 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("経費申請が見つかりません");
    }

    @Test
    @DisplayName("ステータスがDRAFTの場合はBusinessExceptionをスロー")
    void ステータスがDRAFTの場合() {
        // Given
        Expense draftExpense = ExpenseTestBuilder.createDraft();
        given(expenseMapper.findById(draftExpense.getId())).willReturn(draftExpense);

        // When & Then
        assertThatThrownBy(() -> service.reject(draftExpense.getId(), "理由", 1, 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("提出済み以外は却下できません");
    }

    @Test
    @DisplayName("バージョンが一致しない場合はBusinessExceptionをスロー")
    void バージョンが一致しない場合() {
        // Given
        Expense expense = ExpenseTestBuilder.createSubmitted();
        given(expenseMapper.findById(expense.getId())).willReturn(expense);

        // When & Then
        int wrongVersion = 999;
        assertThatThrownBy(() -> service.reject(expense.getId(), "理由", wrongVersion, 999L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("他のユーザに更新されています");
    }
}
```

</details>

---

## 📝 今日のまとめ

### できるようになったこと

✅ assertThatThrownBy で例外をテストできる
✅ HTTPエラーステータスを検証できる
✅ エラーメッセージを検証できる
✅ エラーケースの重要性を理解した

---

### エラーテストのチェックリスト

- [ ] nullチェックのテストを書いたか？
- [ ] バリデーションエラーのテストを書いたか？
- [ ] 権限エラーのテストを書いたか？
- [ ] ビジネスロジックエラーのテストを書いたか？
- [ ] エラーメッセージを検証しているか？

---

### 次のステップ

明日は総合演習問題に挑戦します！

👉 [Day 7: 演習問題と総復習](./day7_exercises.md)

---

お疲れさまでした！
