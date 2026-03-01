# Day 7: 演習問題と総復習 📝

**学習時間:** 2時間
**難易度:** ⭐⭐⭐⭐☆

---

## 🎯 今日の目標

- 1週間の総復習
- 総合演習問題に挑戦
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

### Day 4: HTTPリクエストのテスト

- MockMvcの使い方
- httpBasic（認証）
- jsonPath（JSONレスポンスの検証）

### Day 5: テストの整理とリファクタリング

- @Nested でグループ化
- @DisplayName で日本語のテスト名
- @BeforeEach でテストデータを準備
- Given-When-Thenパターン

### Day 6: エラーケースのテスト

- assertThatThrownBy で例外をテスト
- HTTPエラーステータスの検証
- エラーメッセージの検証

---

## 📝 総合演習問題

### 演習1: 経費承認APIの完全なテストを書く

**要件:**
- @Nested でグループ化
- @DisplayName で日本語のテスト名
- Given-When-Thenパターン
- 正常系、異常系、境界値をすべてテスト

**テストケース:**

1. **正常系**
   - 提出済みの経費を承認できる

2. **異常系 - 認証**
   - 認証なしで承認すると401エラー
   - 一般ユーザーが承認すると403エラー

3. **異常系 - データ**
   - 存在しない経費を承認すると404エラー

4. **異常系 - ビジネスロジック**
   - 下書きの経費を承認すると409エラー
   - 既に承認済みの経費を承認すると409エラー
   - 却下済みの経費を承認すると409エラー

5. **異常系 - 楽観的ロック**
   - バージョンが一致しない場合は409エラー

<details>
<summary>ヒント</summary>

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("経費承認API")
class ExpenseApproveApiTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("正常系")
    class SuccessCase {
        // ここに実装
    }

    @Nested
    @DisplayName("異常系 - 認証")
    class AuthenticationError {
        // ここに実装
    }

    @Nested
    @DisplayName("異常系 - データ")
    class DataError {
        // ここに実装
    }

    @Nested
    @DisplayName("異常系 - ビジネスロジック")
    class BusinessLogicError {
        // ここに実装
    }

    @Nested
    @DisplayName("異常系 - 楽観的ロック")
    class OptimisticLockError {
        // ここに実装
    }
}
```

</details>

<details>
<summary>解答</summary>

`solutions/Day7Exercise1.java` を参照

</details>

---

### 演習2: 既存のテストコードをリファクタリング

**Before:**

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

**要件:**
- @Nested でグループ化
- @DisplayName で日本語のテスト名
- @BeforeEach でテストデータを準備
- Given-When-Thenパターン
- マジックナンバーを変数に

<details>
<summary>解答</summary>

`solutions/Day7Exercise2.java` を参照

</details>

---

### 演習3: normalizedOrderBy と normalizedDirection のテストを完成させる

**要件:**
- すべてのパターンをテスト
- パラメータ化テスト（@ParameterizedTest）を使う
- @Nested でグループ化

**テストケース:**

1. **normalizedOrderBy**
   - null → "created_at"
   - 空文字 → "created_at"
   - 許可された値 → そのまま
   - 不正な値 → "created_at"
   - カンマ区切り → 最初の値を使う

2. **normalizedDirection**
   - null → "DESC"
   - 空文字 → "DESC"
   - "asc" → "ASC"
   - "desc" → "DESC"
   - 不正な値 → "DESC"

<details>
<summary>解答</summary>

`solutions/Day7Exercise3.java` を参照

</details>

---

## 🎓 理解度チェック（最終テスト）

### Q1: テストピラミッドの割合は？

<details>
<summary>答えを見る</summary>

- 単体テスト: 70%
- 統合テスト: 20%
- E2Eテスト: 10%

</details>

---

### Q2: MockMvcでPOSTリクエストを送る方法は？

<details>
<summary>答えを見る</summary>

```java
mockMvc.perform(
    post("/path")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json)
        .with(httpBasic("email", "password"))
)
```

</details>

---

### Q3: 例外をテストする方法は？

<details>
<summary>答えを見る</summary>

```java
assertThatThrownBy(() -> service.method())
    .isInstanceOf(BusinessException.class)
    .hasMessageContaining("エラーメッセージ");
```

</details>

---

### Q4: JSONレスポンスを検証する方法は？

<details>
<summary>答えを見る</summary>

```java
.andExpect(jsonPath("$.id").value(1))
.andExpect(jsonPath("$.title").value("タクシー代"))
.andExpect(jsonPath("$.amount").exists())
```

</details>

---

### Q5: テストをグループ化する方法は？

<details>
<summary>答えを見る</summary>

```java
@Nested
@DisplayName("経費提出API")
class SubmitTest {
    // テスト
}
```

</details>

---

## 🎉 1週間お疲れさまでした！

### できるようになったこと

✅ テストの重要性を理解した
✅ JUnit 5で基本的なテストを書ける
✅ AssertJで検証を書ける
✅ MockMvcでAPIのテストを書ける
✅ @Nested, @DisplayNameで読みやすいテストを書ける
✅ 異常系のテストを書ける
✅ Given-When-Thenパターンでテストを構造化できる

---

### 次のステップ

#### 2週目で学ぶこと

- モックを使った単体テスト（@Mock, @InjectMocks）
- テストデータビルダーパターン
- パラメータ化テスト（@ParameterizedTest）
- トランザクションのテスト
- カバレッジの測定

---

### 実践

1. **既存のテストコードを改善**
   - `ExpenseServiceTest.java`
   - `ExpenseAuditLogServiceTest.java`

2. **新しいテストを書く**
   - `submit()` メソッドの単体テスト
   - `reject()` メソッドの単体テスト

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
2. 解答例を見る
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
