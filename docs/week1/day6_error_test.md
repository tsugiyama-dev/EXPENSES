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

### HTTPステータスコードの検証

```java
@Nested
@DisplayName("HTTPエラー")
class HttpErrorTest {

    @Test
    @DisplayName("認証なしで経費を取得すると401エラー")
    void 認証なしで取得すると401エラー() throws Exception {
        mockMvc.perform(get("/expenses/1"))  // 認証なし
            .andExpect(status().isUnauthorized())  // 401
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("一般ユーザーが承認すると403エラー")
    void 一般ユーザーが承認すると403エラー() throws Exception {
        mockMvc.perform(
            post("/expenses/{id}/approve", 29L)
                .with(httpBasic("hikaru@example.com", "pass1234"))  // 一般ユーザー
        )
        .andExpect(status().isForbidden())  // 403
        .andExpect(jsonPath("$.message").value("アクセスが拒否されました"));
    }

    @Test
    @DisplayName("存在しない経費を取得すると404エラー")
    void 存在しない経費を取得すると404エラー() throws Exception {
        mockMvc.perform(
            get("/expenses/9999")
                .with(httpBasic("hikaru@example.com", "pass1234"))
        )
        .andExpect(status().isNotFound())  // 404
        .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
    }

    @Test
    @DisplayName("バリデーションエラーで400エラー")
    void バリデーションエラーで400エラー() throws Exception {
        String json = """
            {
                "title": "",
                "amount": -1000,
                "currency": "INVALID"
            }
            """;

        mockMvc.perform(
            post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(httpBasic("hikaru@example.com", "pass1234"))
        )
        .andExpect(status().isBadRequest())  // 400
        .andExpect(jsonPath("$.details").isArray())
        .andExpect(jsonPath("$.details[0].field").exists())
        .andExpect(jsonPath("$.details[0].message").exists());
    }

    @Test
    @DisplayName("楽観的ロックエラーで409エラー")
    void 楽観的ロックエラーで409エラー() throws Exception {
        mockMvc.perform(
            post("/expenses/{id}/approve", 29L)
                .param("version", "999")  // 不正なバージョン
                .with(httpBasic("approver@example.com", "1234"))
        )
        .andExpect(status().isConflict())  // 409
        .andExpect(jsonPath("$.message").value("他のユーザに更新されています"));
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
