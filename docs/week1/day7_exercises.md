# Day 7: 演習問題と総復習 📝

**学習時間:** 2時間
**難易度:** ⭐⭐⭐⭐☆

---

## 🎯 今日の目標

- 1週間の総復習
- Mockitoユニットテストの総合演習
- 既存のテストコードを改善
- 自分でテストを書けるようになる

---

## 📚 1週間の復習

### Day 1: テストとは何か？

- テストの重要性
- テストピラミッド（単体70%, 統合20%, E2E10%）
- テストのメリット

### Day 2: 超シンプルなテストを書く

- JUnit 5の基本
- @Test アノテーション
- assertEquals, assertTrue

### Day 3: AssertJで検証する

- assertThat().isEqualTo()
- 文字列、数値、コレクションの検証
- エラーメッセージの読み方

### Day 4: Mockitoの基礎

- @Mock, @InjectMocks
- when().thenReturn() でモックの振る舞いを定義
- verify() でメソッド呼び出しを検証

### Day 5: テストの整理とリファクタリング

- @Nested でグループ化
- @DisplayName で日本語のテスト名
- Given-When-Thenパターン

### Day 6: エラーケースのテスト

- assertThatThrownBy で例外をテスト
- when().thenThrow() で例外をスロー
- verify() で呼び出し回数を検証

---

## 📝 総合演習問題

### 演習1: submit メソッドの完全なテストを書く

**要件:**
- @Nested でグループ化
- @DisplayName で日本語のテスト名
- Given-When-Thenパターン
- 正常系、異常系をすべてテスト

**テストケース:**

1. **正常系**
   - 下書き状態の経費を提出できる

2. **異常系**
   - 経費が存在しない場合はBusinessExceptionをスロー
   - 経費の所有者でない場合はBusinessExceptionをスロー
   - すでに提出済みの場合はBusinessExceptionをスロー
   - バージョンが一致しない場合はBusinessExceptionをスロー

<details>
<summary>ヒント</summary>

```java
@Nested
@DisplayName("経費提出 (submit)")
class SubmitTest {

    @Test
    @DisplayName("正常系: 下書き状態の経費を提出できる")
    void 下書き状態の経費を提出できる() {
        // Given: 下書きの経費
        Long expenseId = 1L;
        Long userId = 123L;
        Expense expense = Expense.create(userId, "出張費",
                             new BigDecimal("10000"), "JPY");

        when(authenticationContext.getCurrentUserId()).thenReturn(userId);
        when(expenseMapper.findById(expenseId)).thenReturn(expense);

        // When: 提出
        ExpenseResponse response = expenseService.submit(expenseId, 1);

        // Then: ステータスがSUBMITTEDになる
        assertThat(response.status()).isEqualTo(ExpenseStatus.SUBMITTED);

        // Then: 必要なメソッドが呼ばれた
        verify(expenseMapper).update(any(Expense.class));
        verify(auditLogMapper).insert(any(ExpenseAuditLog.class));
    }

    @Test
    @DisplayName("異常系: 経費が存在しない場合はBusinessExceptionをスロー")
    void 経費が存在しない場合() {
        // Given: 存在しないID
        Long expenseId = 9999L;
        when(expenseMapper.findById(expenseId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> expenseService.submit(expenseId, 1))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("経費申請が見つかりません");
    }

    // 他のテストケースを実装...
}
```

</details>

---

### 演習2: approve メソッドの完全なテストを書く

**要件:**
- @Nested でグループ化
- @DisplayName で日本語のテスト名
- Given-When-Thenパターン
- 正常系、異常系、境界値をすべてテスト

**テストケース:**

1. **正常系**
   - 提出済みの経費を承認できる

2. **異常系 - データ**
   - 経費が存在しない場合はBusinessExceptionをスロー

3. **異常系 - ビジネスロジック**
   - 下書きの経費を承認すると BusinessExceptionをスロー
   - 既に承認済みの経費を承認すると BusinessExceptionをスロー
   - 却下済みの経費を承認すると BusinessExceptionをスロー

4. **異常系 - 楽観的ロック**
   - バージョンが一致しない場合は BusinessExceptionをスロー

<details>
<summary>解答の構造</summary>

```java
@Nested
@DisplayName("経費承認 (approve)")
class ApproveTest {

    @Nested
    @DisplayName("正常系")
    class SuccessCase {
        @Test
        @DisplayName("提出済みの経費を承認できる")
        void 提出済みの経費を承認できる() {
            // ここに実装
        }
    }

    @Nested
    @DisplayName("異常系 - データ")
    class DataError {
        @Test
        @DisplayName("経費が存在しない場合はBusinessExceptionをスロー")
        void 経費が存在しない場合() {
            // ここに実装
        }
    }

    @Nested
    @DisplayName("異常系 - ビジネスロジック")
    class BusinessLogicError {
        @Test
        @DisplayName("下書きの経費を承認するとBusinessExceptionをスロー")
        void 下書きの経費を承認する() {
            // ここに実装
        }

        @Test
        @DisplayName("既に承認済みの経費を承認するとBusinessExceptionをスロー")
        void 既に承認済みの経費を承認する() {
            // ここに実装
        }
    }

    @Nested
    @DisplayName("異常系 - 楽観的ロック")
    class OptimisticLockError {
        @Test
        @DisplayName("バージョンが一致しない場合はBusinessExceptionをスロー")
        void バージョンが一致しない場合() {
            // ここに実装
        }
    }
}
```

</details>

---

### 演習3: reject メソッドの完全なテストを書く

**問題:** rejectメソッドのユニットテストを完成させてください。

**テストケース:**

1. **正常系**
   - 提出済みの経費を却下できる

2. **異常系**
   - 経費が存在しない場合はBusinessExceptionをスロー
   - 下書きの経費を却下するとBusinessExceptionをスロー
   - バージョンが一致しない場合はBusinessExceptionをスロー
   - 却下理由がnullの場合はIllegalArgumentExceptionをスロー
   - 却下理由が空文字の場合はIllegalArgumentExceptionをスロー

---

### 演習4: 既存のテストをリファクタリング

**Before（現在のコード）:**

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
        // 長いテストコード...
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

**課題:**

以下の改善を行ってください：

1. @Nested でメソッドごとにグループ化
2. @DisplayName で日本語の説明を追加
3. Given-When-Thenコメントを明確に追加
4. テストメソッド名を日本語に変更
5. 各テストケースに不足しているエラーケースを追加

---

## 🎓 理解度チェック（最終テスト）

### Q1: Mockitoで依存オブジェクトをモックにするアノテーションは？

<details>
<summary>答えを見る</summary>

```java
@Mock
private ExpenseMapper expenseMapper;

@InjectMocks
private ExpenseService expenseService;
```

</details>

---

### Q2: モックの振る舞いを定義する方法は？

<details>
<summary>答えを見る</summary>

```java
when(authenticationContext.getCurrentUserId()).thenReturn(123L);

// または例外をスロー
when(expenseMapper.findById(anyLong()))
    .thenThrow(new BusinessException("エラー"));
```

</details>

---

### Q3: 例外をテストする方法は？

<details>
<summary>答えを見る</summary>

```java
assertThatThrownBy(() -> expenseService.submit(9999L, 1))
    .isInstanceOf(BusinessException.class)
    .hasMessageContaining("経費申請が見つかりません");
```

</details>

---

### Q4: モックのメソッド呼び出しを検証する方法は？

<details>
<summary>答えを見る</summary>

```java
// 呼ばれたことを確認
verify(expenseMapper).insert(any(Expense.class));

// 呼び出し回数を確認
verify(expenseMapper, times(1)).insert(any(Expense.class));

// 呼ばれていないことを確認
verify(expenseMapper, never()).delete(anyLong());

// 引数の中身を確認
verify(expenseMapper).search(
    argThat(c -> c.getApplicantId().equals(123L)),
    anyString(),
    anyString(),
    anyInt(),
    anyInt()
);
```

</details>

---

### Q5: テストをグループ化する方法は？

<details>
<summary>答えを見る</summary>

```java
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
```

</details>

---

## 🎉 1週間お疲れさまでした！

### できるようになったこと

✅ テストの重要性を理解した
✅ JUnit 5で基本的なテストを書ける
✅ AssertJで検証を書ける
✅ Mockitoでモックを使ったユニットテストを書ける
✅ @Nested, @DisplayNameで読みやすいテストを書ける
✅ 異常系のテストを書ける
✅ verify()でモックの呼び出しを検証できる
✅ Given-When-Thenパターンでテストを構造化できる

---

### 次のステップ

#### 2週目で学ぶこと

- テストデータビルダーパターン
- パラメータ化テスト（@ParameterizedTest）
- カバレッジの測定
- リファクタリングとテスト
- 統合テスト（MockMvc, @SpringBootTest）

---

### 実践

1. **既存のテストコードを改善**
   - `ExpenseServiceTest.java`をリファクタリング
   - 不足しているテストケースを追加

2. **新しいテストを書く**
   - `submit()` メソッドの完全なテスト
   - `approve()` メソッドの完全なテスト
   - `reject()` メソッドの完全なテスト

3. **テストを実行**
   ```bash
   mvn test
   ```

---

### テストを書く習慣をつけよう

```
コードを書く
  ↓
テストを書く  ← これを習慣に！
  ↓
リファクタリング
  ↓
テストが通ることを確認
  ↓
コミット
```

---

## 📞 サポート

わからないことがあれば：
1. その日の資料を見直す
2. 解答例を見る（solutionsディレクトリ）
3. Googleで検索
4. 先輩エンジニアに聞く

---

## 🏆 修了証

すべてのテストが通ったら修了です！

```bash
# テストを実行
mvn test

# すべてのテストが通ったら
[INFO] Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
[INFO] ✅ BUILD SUCCESS

🎉 おめでとうございます！1週目を修了しました！
```

---

**次は2週目に進むか、実際のプロジェクトでテストを書いてみましょう！**
