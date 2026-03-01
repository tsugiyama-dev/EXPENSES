# Day 5: ユニットテストのリファクタリング 🧹

**学習時間:** 2時間
**難易度:** ⭐⭐⭐☆☆

---

## 🎯 今日の目標

- Mockitoを使ったユニットテストの構造を理解する
- テストメソッド名を改善できる
- @Nested でグループ化できる
- @DisplayName で日本語のテスト名が書ける
- Given-When-Thenパターンを使える

---

## 📖 現在のテストコードの確認

### ✅ 現在のExpenseServiceTest.java（main branch）

```java
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseMapper expenseMapper;
    @Mock
    private ExpenseAuditLogMapper auditLogMapper;
    @Mock
    private AuthenticationContext authenticationContext;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void expenseCreate_Success() {
        // テストコード
        Long expectedUserId = 123L;
        ExpenseCreateRequest request = new ExpenseCreateRequest(
            "出張費",
            new BigDecimal("10000"),
            "JPY"
        );

        when(authenticationContext.getCurrentUserId()).thenReturn(expectedUserId);

        doAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense = Expense.create(expectedUserId, expense.getTitle(),
                                    expense.getAmount(), expense.getCurrency());
            return null;
        }).when(expenseMapper).insert(any(Expense.class));

        ExpenseResponse response = expenseService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.applicantId()).isEqualTo(expectedUserId);
        assertThat(response.title()).isEqualTo("出張費");
        assertThat(response.amount()).isEqualTo(new BigDecimal("10000"));

        verify(authenticationContext).getCurrentUserId();
        verify(expenseMapper).insert(any(Expense.class));
        verify(auditLogMapper).insert(any(ExpenseAuditLog.class));
    }

    @Test
    void expenseSearch_other_than_approver_only_owners_expenses_get() {
        // ...
    }

    @Test
    void expenseSearch_approver_all_expenses_get() {
        // ...
    }
}
```

**改善点:**
- ✅ Mockitoを使っているので高速
- ✅ データベース不要
- ❌ テスト名が英語（日本人チームには読みにくい）
- ❌ グループ化されていない
- ❌ Given-When-Thenのコメントが不十分

---

## ✨ 改善版: 読みやすいユニットテスト

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService ユニットテスト")
class ExpenseServiceTest {

    @Mock
    private ExpenseMapper expenseMapper;
    @Mock
    private ExpenseAuditLogMapper auditLogMapper;
    @Mock
    private AuthenticationContext authenticationContext;

    @InjectMocks
    private ExpenseService expenseService;

    @Nested
    @DisplayName("経費作成 (create)")
    class CreateTest {

        @Test
        @DisplayName("正常系: 経費を作成できる")
        void 経費を作成できる() {
            // Given: リクエストデータとユーザーID
            Long userId = 123L;
            ExpenseCreateRequest request = new ExpenseCreateRequest(
                "出張費",
                new BigDecimal("10000"),
                "JPY"
            );

            // モックの設定
            when(authenticationContext.getCurrentUserId()).thenReturn(userId);
            doAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                return null;
            }).when(expenseMapper).insert(any(Expense.class));

            // When: 経費を作成
            ExpenseResponse response = expenseService.create(request);

            // Then: 正しい値が返される
            assertThat(response).isNotNull();
            assertThat(response.applicantId()).isEqualTo(userId);
            assertThat(response.title()).isEqualTo("出張費");
            assertThat(response.amount()).isEqualTo(new BigDecimal("10000"));

            // Then: 必要なメソッドが呼ばれた
            verify(authenticationContext).getCurrentUserId();
            verify(expenseMapper).insert(any(Expense.class));
            verify(auditLogMapper).insert(any(ExpenseAuditLog.class));
        }
    }

    @Nested
    @DisplayName("経費検索 (search)")
    class SearchTest {

        @Test
        @DisplayName("一般ユーザー: 自分の経費のみ取得できる")
        void 一般ユーザーは自分の経費のみ取得() {
            // Given: 一般ユーザー
            Long userId = 123L;
            when(authenticationContext.getCurrentUserId()).thenReturn(userId);
            when(authenticationContext.isApprover()).thenReturn(false);

            ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
                null, null, null, null, null, null, null, null
            );

            // When: 検索実行
            expenseService.search(criteria, 1, 10);

            // Then: applicantIdでフィルタされている
            verify(expenseMapper).search(
                argThat(c -> c.getApplicantId().equals(userId)),
                anyString(),
                anyString(),
                anyInt(),
                anyInt()
            );
        }

        @Test
        @DisplayName("承認者: 全ての経費を取得できる")
        void 承認者は全ての経費を取得() {
            // Given: 承認者
            Long approverId = 456L;
            when(authenticationContext.getCurrentUserId()).thenReturn(approverId);
            when(authenticationContext.isApprover()).thenReturn(true);

            ExpenseSearchCriteria criteria = new ExpenseSearchCriteria(
                null, null, null, null, null, null, null, null
            );

            // When: 検索実行
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
    }
}
```

---

## 🔧 リファクタリング手法

### 1. @Nested でグループ化

```java
@DisplayName("ExpenseService ユニットテスト")
class ExpenseServiceTest {

    @Nested
    @DisplayName("経費作成 (create)")
    class CreateTest {
        // 作成関連のテスト
    }

    @Nested
    @DisplayName("経費検索 (search)")
    class SearchTest {
        // 検索関連のテスト
    }
}
```

**メリット:**
- テストがメソッドごとにグループ化される
- テストレポートが見やすくなる
- テストの整理がしやすい

---

### 2. @DisplayName で日本語のテスト名

```java
@Test
@DisplayName("一般ユーザー: 自分の経費のみ取得できる")
void 一般ユーザーは自分の経費のみ取得() {
    // ...
}
```

**メリット:**
- テストの意図が一目でわかる
- チーム全員が読みやすい
- テストレポートがわかりやすい

---

### 3. Given-When-Thenパターン

```java
@Test
void テスト() {
    // Given: 前提条件（モックの設定、テストデータ）
    Long userId = 123L;
    when(authenticationContext.getCurrentUserId()).thenReturn(userId);

    // When: 実行（テスト対象のメソッドを呼ぶ）
    ExpenseResponse response = expenseService.create(request);

    // Then: 検証（結果の確認、モックの呼び出し確認）
    assertThat(response).isNotNull();
    verify(expenseMapper).insert(any(Expense.class));
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
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseMapper expenseMapper;
    @Mock
    private ExpenseAuditLogMapper auditLogMapper;
    @Mock
    private AuthenticationContext authenticationContext;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void expenseCreate_Success() {
        // ... 長いテストコード
    }

    @Test
    void expenseSearch_other_than_approver_only_owners_expenses_get() {
        // ...
    }
}
```

**問題点:**
- テスト名が英語で長い
- グループ化されていない
- Given-When-Thenコメントが不十分

### 課題

以下の改善を行ってください：

1. @Nested でメソッドごとにグループ化
2. @DisplayName で日本語の説明を追加
3. Given-When-Thenコメントを明確に追加
4. テストメソッド名を日本語に変更

---

## 📝 今日のまとめ

### できるようになったこと

✅ Mockitoを使ったユニットテストを理解した
✅ @Nested でテストをグループ化できる
✅ @DisplayName で日本語のテスト名が書ける
✅ Given-When-Thenパターンを使える
✅ verify() でモックの呼び出しを検証できる

---

### リファクタリングチェックリスト

- [ ] テスト名が意味を持っているか？
- [ ] @Nested でメソッドごとにグループ化されているか？
- [ ] @DisplayName で説明されているか？
- [ ] Given-When-Thenパターンを使っているか？
- [ ] verify() で必要な呼び出しを確認しているか？

---

### 次のステップ

明日はエラーケースとモックの高度な使い方を学びます！

👉 [Day 6: エラーケースとモックの検証](./day6_error_test.md)

---

お疲れさまでした！
