# Day 6: エラーケースとモックの検証 ⚠️

**学習時間:** 1.5時間
**難易度:** ⭐⭐⭐☆☆

---

## 🎯 今日の目標

- Mockitoで例外をスローする方法を学ぶ
- assertThatThrownBy の使い方を覚える
- verify() で呼び出し回数を検証できる
- エラーケースのテストを書ける

---

## 📖 なぜエラーケースのテストが重要か？

### シーン: エラーハンドリングがない場合

```java
public ExpenseResponse approve(Long expenseId, int version, Long actorId) {
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
    // Given: 存在しないID
    long expenseId = 9999L;
    when(expenseMapper.findById(expenseId)).thenReturn(null);

    // When & Then: 例外がスローされる
    assertThatThrownBy(() -> expenseService.approve(expenseId, 1, 999L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("経費申請が見つかりません");
}
```

**このテストを書くと:**
1. 実装前にエラーハンドリングの必要性に気づく
2. 実装時にnullチェックを追加
3. 適切なエラーメッセージを返すようになる

---

## 🧪 Mockitoで例外をスローする

### パターン1: when().thenThrow()

```java
@Test
void データベースエラーの場合() {
    // Given: データベースがエラーをスロー
    when(expenseMapper.findById(anyLong()))
        .thenThrow(new DataAccessException("DB接続エラー"));

    // When & Then: 例外が伝播する
    assertThatThrownBy(() -> expenseService.getById(1L))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("DB接続エラー");
}
```

### パターン2: voidメソッドの場合

```java
@Test
void 監査ログ登録エラーの場合() {
    // Given: 監査ログ登録時にエラー
    doThrow(new RuntimeException("ログ登録失敗"))
        .when(auditLogMapper).insert(any(ExpenseAuditLog.class));

    // When & Then: 例外が伝播する
    ExpenseCreateRequest request = new ExpenseCreateRequest("出張費",
                                         new BigDecimal("10000"), "JPY");

    assertThatThrownBy(() -> expenseService.create(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("ログ登録失敗");
}
```

---

## 🔬 AssertJを使った例外のテスト

### 基本形

```java
@Test
void 例外をスローする() {
    // When & Then
    assertThatThrownBy(() -> {
        // 例外をスローするコード
        expenseService.approve(9999L, 1, 999L);
    })
    .isInstanceOf(BusinessException.class)         // 例外の型
    .hasMessage("経費申請が見つかりません")           // 完全一致
    .hasMessageContaining("経費申請")               // 部分一致
    .hasFieldOrPropertyWithValue("errorCode", "NOT_FOUND");  // フィールド
}
```

### JUnit 5 の assertThrows も使える

```java
@Test
void 例外をスローする() {
    // When & Then
    BusinessException ex = assertThrows(
        BusinessException.class,
        () -> expenseService.approve(9999L, 1, 999L)
    );

    // 例外の内容を検証
    assertThat(ex.getMessage()).contains("経費申請が見つかりません");
    assertThat(ex.getErrorCode()).isEqualTo("NOT_FOUND");
}
```

---

## 📝 実例: searchメソッドのエラーケース

### テスト対象のコード

```java
public List<ExpenseResponse> search(ExpenseSearchCriteria criteria,
                                   int page, int limit) {
    // ページング検証
    if (page < 1) {
        throw new IllegalArgumentException("ページは1以上である必要があります");
    }
    if (limit < 1 || limit > 100) {
        throw new IllegalArgumentException("limitは1-100の範囲である必要があります");
    }

    // 権限によるフィルタリング
    if (!authenticationContext.isApprover()) {
        criteria = criteria.withApplicantId(
            authenticationContext.getCurrentUserId()
        );
    }

    // 検索実行
    return expenseMapper.search(criteria, ...);
}
```

---

### エラーケースのテスト

```java
@Nested
@DisplayName("経費検索 (search) - 異常系")
class SearchErrorTest {

    @Test
    @DisplayName("pageが0の場合はIllegalArgumentExceptionをスロー")
    void pageが0の場合() {
        // Given
        ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
            null, null, null, null, null, null, null, null
        );

        // When & Then
        assertThatThrownBy(() -> expenseService.search(criteria, 0, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ページは1以上");
    }

    @Test
    @DisplayName("limitが0の場合はIllegalArgumentExceptionをスロー")
    void limitが0の場合() {
        // Given
        ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
            null, null, null, null, null, null, null, null
        );

        // When & Then
        assertThatThrownBy(() -> expenseService.search(criteria, 1, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("limitは1-100の範囲");
    }

    @Test
    @DisplayName("limitが101の場合はIllegalArgumentExceptionをスロー")
    void limitが101の場合() {
        // Given
        ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
            null, null, null, null, null, null, null, null
        );

        // When & Then
        assertThatThrownBy(() -> expenseService.search(criteria, 1, 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("limitは1-100の範囲");
    }
}
```

---

## 🔍 verify() でモックの呼び出しを検証

### 基本形: 呼ばれたことを確認

```java
@Test
void メソッドが呼ばれたことを確認() {
    // When
    expenseService.create(request);

    // Then: insert()が呼ばれた
    verify(expenseMapper).insert(any(Expense.class));
    verify(auditLogMapper).insert(any(ExpenseAuditLog.class));
}
```

### 呼び出し回数を確認

```java
@Test
void 呼び出し回数を確認() {
    // When
    expenseService.create(request);

    // Then
    verify(expenseMapper, times(1)).insert(any(Expense.class));
    verify(expenseMapper, never()).update(any(Expense.class));
}
```

### 引数の中身を確認

```java
@Test
void 引数の中身を確認() {
    // When
    Long userId = 123L;
    when(authenticationContext.getCurrentUserId()).thenReturn(userId);
    when(authenticationContext.isApprover()).thenReturn(false);

    expenseService.search(criteria, 1, 10);

    // Then: applicantIdに正しいuserIdが設定されている
    verify(expenseMapper).search(
        argThat(c -> c.getApplicantId().equals(userId)),
        anyString(),
        anyString(),
        anyInt(),
        anyInt()
    );
}
```

### 呼ばれていないことを確認

```java
@Test
void 承認者の場合はapplicantIdでフィルタしない() {
    // Given: 承認者
    when(authenticationContext.isApprover()).thenReturn(true);

    // When
    expenseService.search(criteria, 1, 10);

    // Then: applicantIdでフィルタされていない
    verify(expenseMapper).search(
        argThat(c -> c.getApplicantId() == null),
        anyString(),
        anyString(),
        anyInt(),
        anyInt()
    );
}
```

---

## 📝 演習問題

### 演習1: createメソッドのエラーケース

**問題:** 以下のエラーケースのテストを書いてください。

1. authenticationContext.getCurrentUserId() が null を返す場合
2. expenseMapper.insert() が例外をスローする場合
3. auditLogMapper.insert() が例外をスローする場合

<details>
<summary>解答例</summary>

```java
@Nested
@DisplayName("経費作成 (create) - 異常系")
class CreateErrorTest {

    @Test
    @DisplayName("ユーザーIDが取得できない場合はNullPointerExceptionをスロー")
    void ユーザーIDが取得できない場合() {
        // Given
        when(authenticationContext.getCurrentUserId()).thenReturn(null);
        ExpenseCreateRequest request = new ExpenseCreateRequest(
            "出張費", new BigDecimal("10000"), "JPY"
        );

        // When & Then
        assertThatThrownBy(() -> expenseService.create(request))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("経費登録に失敗した場合はDataAccessExceptionをスロー")
    void 経費登録に失敗した場合() {
        // Given
        when(authenticationContext.getCurrentUserId()).thenReturn(123L);
        doThrow(new DataAccessException("DB接続エラー"))
            .when(expenseMapper).insert(any(Expense.class));

        ExpenseCreateRequest request = new ExpenseCreateRequest(
            "出張費", new BigDecimal("10000"), "JPY"
        );

        // When & Then
        assertThatThrownBy(() -> expenseService.create(request))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("DB接続エラー");
    }
}
```

</details>

---

## 📝 今日のまとめ

### できるようになったこと

✅ Mockitoで例外をスローできる
✅ assertThatThrownBy で例外をテストできる
✅ verify() でモックの呼び出しを検証できる
✅ エラーケースのテストを書ける

---

### テストのチェックリスト

- [ ] nullチェックのテストを書いたか？
- [ ] バリデーションエラーのテストを書いたか？
- [ ] モックが正しく呼ばれたか verify() で確認したか？
- [ ] エラーメッセージを検証しているか？

---

### 次のステップ

明日は総合演習問題に挑戦します！

👉 [Day 7: 演習問題と総復習](./day7_exercises.md)

---

お疲れさまでした！
