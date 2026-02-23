# 既存テストコードの分析と問題点

## 🔍 現在のテストコード分析

### ExpenseServiceTest.java の問題点

#### ❌ 問題1: テスト名が意味不明

```java
@Test
void check_403() throws Exception {
    // 何をチェックしているの？
}

@Test
void check_409() throws Exception {
    // 何をチェックしているの？
}
```

**問題点:**
- HTTPステータスコードしか書いていない
- 何をテストしているか全くわからない
- 後で読んだときに理解できない

**改善案:**
```java
@Test
@DisplayName("本人以外が経費を提出すると409エラーが返る")
void 本人以外が経費を提出すると409エラー() throws Exception {
    // テスト内容が一目でわかる！
}
```

---

#### ❌ 問題2: マジックナンバーだらけ

```java
@Test
void check_403() throws Exception {
    long expenseId = 32L;  // ← これは何のID？
    // ...
}

@Test
void check_404() throws Exception {
    mockMvc.perform(post("/expenses/{id}/submit", 9999))  // ← 9999は何？
}
```

**問題点:**
- `32L`, `29L`, `9999`が何を意味するかわからない
- テストデータの準備が不明確
- 他の人が見ても理解できない

**改善案:**
```java
@Test
void 本人以外が経費を提出すると409エラー() throws Exception {
    // Given: 他人の経費（ID=32は hikaru さんの経費）
    long othersExpenseId = 32L;

    // When: yasuko さんが提出しようとする
    mockMvc.perform(post("/expenses/{id}/submit", othersExpenseId)
            .with(httpBasic("yasuko@example.com", "pass1234")))

    // Then: 403エラーが返る
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
}
```

---

#### ❌ 問題3: コメントアウトされたコードが大量

```java
//	@Test
//	void testSubmit() throws Exception {
//		long expenseId = 24L;
//		mockMvc.perform(post("/expenses/{id}/approve", expenseId)
//				.with(httpBasic("hikaru@example.com", "pass1234")))
//				.andExpect(status().isForbidden());
//	}
//	@Test
//	void testApprove() throws Exception{
//		long expenseId = 19L;
//		mockMvc.perform(
//				post("/expenses/{id}/approve", expenseId)
//				.with(httpBasic("approver@example.com", "1234"))
//				).andExpect(status().isOk());
//	}
```

**問題点:**
- 使わないコードが大量に残っている
- ファイルが読みにくい
- どれが有効なテストかわからない

**改善案:**
- コメントアウトは削除する
- 必要なら別ブランチに退避
- Gitで管理しているので消しても大丈夫！

---

#### ❌ 問題4: テストの構造化がない

```java
class ExpenseServiceTest {
    @Test
    void test1() { ... }

    @Test
    void test2() { ... }

    @Test
    void test3() { ... }
    // ← 機能ごとのグループ化なし
}
```

**問題点:**
- どのテストがどの機能をテストしているかわからない
- テストが増えると管理できない

**改善案:**
```java
class ExpenseServiceTest {

    @Nested
    @DisplayName("経費提出API")
    class SubmitTest {
        @Test
        void 正常に提出できる() { ... }

        @Test
        void 本人以外は提出できない() { ... }
    }

    @Nested
    @DisplayName("経費承認API")
    class ApproveTest {
        @Test
        void 正常に承認できる() { ... }

        @Test
        void 一般ユーザーは承認できない() { ... }
    }
}
```

---

#### ❌ 問題5: Given-When-Thenパターンがない

```java
@Test
void check_403() throws Exception {
    long expenseId = 32L;
    mockMvc.perform(post("/expenses/{id}/submit", expenseId)
            .with(httpBasic("hikaru@example.com", "pass1234")))
    .andExpect(status().isConflict())
    .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
}
```

**問題点:**
- 何を準備して（Given）
- 何を実行して（When）
- 何を確認するか（Then）
が不明確

**改善案:**
```java
@Test
void 本人以外が経費を提出すると409エラー() throws Exception {
    // Given: 他人の経費
    long othersExpenseId = 32L;

    // When: 別のユーザーが提出しようとする
    var result = mockMvc.perform(
        post("/expenses/{id}/submit", othersExpenseId)
            .with(httpBasic("yasuko@example.com", "pass1234"))
    );

    // Then: 403エラーが返る
    result.andExpect(status().isConflict())
          .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
}
```

---

#### ❌ 問題6: 検証が不十分

```java
@Test
void success_approve() throws Exception {
    long expenseId = 38L;
    mockMvc.perform(post("/expenses/{id}/approve", expenseId)
            .with(httpBasic("approver@example.com","1234")))
    .andExpect(status().isOk());  // ← ステータスコードだけ？
}
```

**問題点:**
- HTTPステータスだけ確認
- レスポンスボディを見ていない
- 本当に承認されたか確認していない

**改善案:**
```java
@Test
void 提出済みの経費を承認できる() throws Exception {
    // Given
    long expenseId = 38L;

    // When
    var result = mockMvc.perform(
        post("/expenses/{id}/approve", expenseId)
            .with(httpBasic("approver@example.com","1234"))
    );

    // Then: ステータスコードとレスポンスボディを確認
    result.andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("APPROVED"))
          .andExpect(jsonPath("$.id").value(38))
          .andExpect(jsonPath("$.version").exists());
}
```

---

## 🎯 初学者がつまずきやすいポイント

### 1. 何をテストすればいいかわからない

**つまずきポイント:**
- どのケースをテストすべき？
- 正常系と異常系って何？
- どこまでテストを書けばいい？

**解決策:**
1. **正常系** - 正しい入力で正しい結果が返る
2. **異常系** - エラーが正しく返る
3. **境界値** - 0件、最大件、nullなど

---

### 2. テストの書き方がわからない

**つまずきポイント:**
- MockMvcって何？
- httpBasicって何？
- jsonPathって何？

**解決策:**
- **MockMvc** - HTTPリクエストをテストするツール
- **httpBasic** - Basic認証（ユーザー名とパスワード）
- **jsonPath** - JSONレスポンスを検証

---

### 3. AssertJの使い方がわからない

**つまずきポイント:**
- assertThat().isEqualTo()の意味は？
- どんな検証メソッドがある？

**解決策:**
```java
// 値の検証
assertThat(result).isEqualTo(expected);
assertThat(result).isNotNull();
assertThat(result).isTrue();

// コレクションの検証
assertThat(list).hasSize(3);
assertThat(list).contains("A", "B");

// 例外の検証
assertThatThrownBy(() -> service.method())
    .isInstanceOf(BusinessException.class)
    .hasMessage("エラーメッセージ");
```

---

### 4. モックとスタブの違いがわからない

**つまずきポイント:**
- @Mockって何？
- given().willReturn()って何？

**解決策:**
- **Mock** - 偽物のオブジェクト
- **Stub** - 決まった値を返す偽物
- **given().willReturn()** - モックの振る舞いを定義

---

### 5. テストデータの準備が面倒

**つまずきポイント:**
- 毎回データを作るのが大変
- コピペばかりになる

**解決策:**
- **ビルダーパターン** - テストデータを簡単に作成
- **ファクトリーメソッド** - よく使うパターンを定義

---

### 6. エラーのテストの書き方がわからない

**つまずきポイント:**
- 例外をどうテストする？
- assertThrowsの使い方は？

**解決策:**
```java
// AssertJを使う
assertThatThrownBy(() -> service.approve(9999L, 1, 999L))
    .isInstanceOf(BusinessException.class)
    .hasMessageContaining("経費申請が見つかりません");

// JUnit 5を使う
BusinessException ex = assertThrows(
    BusinessException.class,
    () -> service.approve(9999L, 1, 999L)
);
assertThat(ex.getMessage()).contains("経費申請が見つかりません");
```

---

### 7. 統合テストと単体テストの違いがわからない

**つまずきポイント:**
- どっちを書けばいい？
- どう使い分ける？

**解決策:**

| テスト | 対象 | 速度 | 例 |
|--------|------|------|-----|
| **単体テスト** | 1つのクラス | 速い | ビジネスロジック |
| **統合テスト** | 複数のコンポーネント | 遅い | API全体 |

**目安:**
- 単体テスト: 70%
- 統合テスト: 20%
- E2Eテスト: 10%

---

## 📝 次のステップ

この分析をもとに、**1週間のカリキュラム**を作成します：

- **Day 1**: テストとは何か？なぜ必要か？
- **Day 2**: 超シンプルなテストを書く
- **Day 3**: AssertJで検証する
- **Day 4**: HTTPリクエストのテスト
- **Day 5**: テストの整理とリファクタリング
- **Day 6**: エラーケースのテスト
- **Day 7**: 演習問題と総復習

各Dayで以下を提供：
1. **なぜ学ぶのか** - モチベーション
2. **理論** - 基礎知識
3. **実例** - 実際のコード
4. **演習** - 自分で書いてみる
5. **解答** - 答え合わせ
6. **よくある間違い** - つまずきポイント
