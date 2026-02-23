# テスト駆動開発（TDD）学習ガイド

## 📚 目次

1. [テストコードの基礎](#1-テストコードの基礎)
2. [JUnit 5のアノテーション](#2-junit-5のアノテーション)
3. [MockMvcを使った統合テスト](#3-mockmvcを使った統合テスト)
4. [AAAパターン](#4-aaaパターン)
5. [HTTPステータスコードのテスト](#5-httpステータスコードのテスト)
6. [認証・認可のテスト](#6-認証認可のテスト)
7. [JSONレスポンスの検証](#7-jsonレスポンスの検証)
8. [テストのベストプラクティス](#8-テストのベストプラクティス)
9. [次のステップ](#9-次のステップ)

---

## 1. テストコードの基礎

### なぜテストコードを書くのか？

| 理由 | 説明 |
|------|------|
| **バグの早期発見** | コードを書いた直後にバグを発見できる |
| **リファクタリングの安全性** | テストがあれば安心してコードを変更できる |
| **ドキュメント代わり** | テストコードが仕様書になる |
| **品質の向上** | テストを書くことで設計が良くなる |

### テストの種類

```
単体テスト（Unit Test）
  ↓ 1つのメソッドやクラスをテスト
  ↓ 外部依存をモック化

統合テスト（Integration Test）
  ↓ 複数のコンポーネントを組み合わせてテスト
  ↓ 実際のDBやHTTPリクエストを使う ← ExpenseServiceTest.javaはこれ！

E2Eテスト（End-to-End Test）
  ↓ ブラウザを使って画面操作をテスト
  ↓ ユーザーの操作フローをテスト
```

---

## 2. JUnit 5のアノテーション

### 現在のコードで使われているアノテーション

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseServiceTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void unAuthentcted_status_401() throws Exception {
        // テストコード
    }
}
```

### 📖 アノテーション解説

#### `@SpringBootTest`

**役割**: Spring Bootアプリケーション全体を起動してテスト（統合テスト）

```java
@SpringBootTest  // アプリケーション全体を起動
class ExpenseServiceTest {
    // 実際のDBに接続する
    // 実際のServiceやRepositoryを使う
}
```

**メリット:**
- ✅ 本番環境に近い状態でテストできる
- ✅ 複雑な依存関係も自動で解決

**デメリット:**
- ❌ 起動が遅い（全てのBeanを起動するため）
- ❌ テストが重くなる

---

#### `@AutoConfigureMockMvc`

**役割**: MockMvcを自動設定（HTTPリクエストをシミュレート）

```java
@AutoConfigureMockMvc  // MockMvcを使えるようにする
class ExpenseServiceTest {

    @Autowired
    MockMvc mockMvc;  // HTTPリクエストをシミュレート

    @Test
    void test() throws Exception {
        mockMvc.perform(post("/expenses/{id}/submit", 1L))
               .andExpect(status().isOk());
    }
}
```

**MockMvcでできること:**
- HTTPリクエストのシミュレート（GET, POST, PUT, DELETE）
- リクエストヘッダー・ボディの設定
- レスポンスの検証

---

#### `@Transactional`

**役割**: 各テスト後に自動ロールバック（DBをクリーンに保つ）

```java
@Transactional  // テスト後にロールバック
class ExpenseServiceTest {

    @Test
    void test() {
        // 経費を作成してDBに保存
        // → テスト終了後に自動でロールバックされる
    }
}
```

**動作の流れ:**

```
1. テスト開始
   ↓
2. トランザクション開始
   ↓
3. テスト実行（DBに書き込み）
   ↓
4. テスト終了
   ↓
5. ロールバック（DBの変更を破棄）← ここが重要！
```

**なぜ必要か？**
- テスト同士が影響し合わない（テストの独立性）
- テストを何度実行してもDBの状態が同じ

---

#### `@Test`

**役割**: テストメソッドを示す

```java
@Test  // これがテストメソッドであることを示す
void unAuthentcted_status_401() throws Exception {
    // テストコード
}
```

**JUnit 5の特徴:**
- メソッド名に日本語も使える（JUnit 4では不可）
- `public` 修飾子が不要（JUnit 4では必須）

---

#### `@Autowired`

**役割**: Springが管理するBeanを自動注入

```java
@Autowired
MockMvc mockMvc;  // SpringがMockMvcを自動で注入
```

---

## 3. MockMvcを使った統合テスト

### MockMvcの基本構文

```java
mockMvc.perform(リクエスト)
       .andExpect(検証1)
       .andExpect(検証2)
       .andExpect(検証3);
```

### 実例1: 認証なしで401エラー

```java
@Test
void unAuthentcted_status_401() throws Exception {
    long expenseId = 99L;

    mockMvc.perform(post("/expenses/{id}/submit", expenseId))
           .andExpect(status().isUnauthorized());  // 401
}
```

**解説:**
1. `post("/expenses/{id}/submit", expenseId)` - POSTリクエストを送る
2. `.andExpect(status().isUnauthorized())` - 401エラーを期待

---

### 実例2: 認証ありで正常処理

```java
@Test
void check_200_status() throws Exception {
    long id = 29L;

    String json = """
        {
            "reason":"申請期限の締め切り日が過ぎているため承認/申請できません"
        }
        """;

    mockMvc.perform(post("/expenses/{id}/reject", id)
            .contentType(MediaType.APPLICATION_JSON)  // Content-Typeヘッダー
            .content(json)                            // リクエストボディ
            .with(httpBasic("approver@example.com", "1234")))  // Basic認証
           .andExpect(status().isOk())                // 200 OK
           .andExpect(jsonPath("$.status").value("REJECTED"));  // JSON検証
}
```

**解説:**
1. `.contentType(MediaType.APPLICATION_JSON)` - JSONを送ることを宣言
2. `.content(json)` - リクエストボディにJSONを設定
3. `.with(httpBasic(...))` - Basic認証情報を付加
4. `.andExpect(jsonPath("$.status").value("REJECTED"))` - JSONの`status`フィールドを検証

---

### 実例3: バリデーションエラー（400）

```java
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
           .andExpect(status().isBadRequest())  // 400
           .andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
}
```

**解説:**
- `reason` が空文字列 → バリデーションエラー
- `jsonPath("$.details[0].message")` - エラーメッセージの配列の最初の要素を検証

---

## 4. AAAパターン

テストコードは **AAA（Arrange-Act-Assert）パターン** で書くのがベストプラクティスです。

```java
@Test
void testName() {
    // Arrange（準備）: テストに必要なデータを準備
    long expenseId = 29L;
    String json = """
        {
            "reason":"申請期限切れ"
        }
        """;

    // Act（実行）: テスト対象を実行
    var result = mockMvc.perform(post("/expenses/{id}/reject", expenseId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .with(httpBasic("approver@example.com", "1234")));

    // Assert（検証）: 結果を検証
    result.andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("REJECTED"));
}
```

### AAAパターンのメリット

| ステップ | 役割 | 例 |
|---------|------|-----|
| **Arrange** | テストデータの準備 | `long expenseId = 29L;` |
| **Act** | テスト対象の実行 | `mockMvc.perform(...)` |
| **Assert** | 結果の検証 | `.andExpect(status().isOk())` |

---

## 5. HTTPステータスコードのテスト

### 現在のコードでテストされているステータスコード

| コード | 意味 | テストメソッド | 状況 |
|--------|------|---------------|------|
| **200 OK** | 成功 | `check_200_status()` | 経費を正常に却下 |
| **400 Bad Request** | リクエストが不正 | `check_400()` | 却下理由が空 |
| **401 Unauthorized** | 認証なし | `unAuthentcted_status_401()` | ログインしていない |
| **403 Forbidden** | 権限なし | `check_403()`（コメント内） | 本人以外が操作 |
| **404 Not Found** | リソースなし | `check_404()` | 経費IDが存在しない |
| **409 Conflict** | 状態競合 | `check_403()`, `check_409()` | ステータスが不正 |

### ステータスコードの使い分け

```
401 Unauthorized
  → ログインしていない
  → Basic認証のヘッダーがない

403 Forbidden
  → ログインはしているが、権限がない
  → 例: 一般ユーザーが承認しようとする

404 Not Found
  → リソースが存在しない
  → 例: 経費ID=9999が存在しない

409 Conflict
  → リクエストは正しいが、現在の状態では実行できない
  → 例: 下書き状態の経費を承認しようとする
  → 例: 本人以外が提出しようとする

400 Bad Request
  → リクエストのパラメータが不正
  → 例: バリデーションエラー（却下理由が空）
```

---

## 6. 認証・認可のテスト

### Basic認証のシミュレート

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

mockMvc.perform(post("/expenses/{id}/submit", expenseId)
        .with(httpBasic("hikaru@example.com", "pass1234")))  // Basic認証
       .andExpect(status().isConflict());
```

### 認証なしのテスト

```java
@Test
void unAuthentcted_status_401() throws Exception {
    mockMvc.perform(post("/expenses/{id}/submit", 99L))  // 認証情報なし
           .andExpect(status().isUnauthorized());  // 401エラー
}
```

### 権限チェックのテスト

```java
@Test
void check_403() throws Exception {
    long expenseId = 32L;

    // 一般ユーザー（hikaru）が他人の経費を提出しようとする
    mockMvc.perform(post("/expenses/{id}/submit", expenseId)
            .with(httpBasic("hikaru@example.com", "pass1234")))
           .andExpect(status().isConflict())  // 409エラー
           .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
}
```

---

## 7. JSONレスポンスの検証

### JsonPathの使い方

```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 単一フィールドの検証
.andExpect(jsonPath("$.status").value("REJECTED"))

// 配列の要素の検証
.andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"))

// ネストしたフィールドの検証
.andExpect(jsonPath("$.data.expense.id").value(29))
```

### JsonPath構文

| 構文 | 意味 | 例 |
|------|------|-----|
| `$.fieldName` | ルートのフィールド | `$.status` → `{"status": "APPROVED"}` |
| `$.array[0]` | 配列の最初の要素 | `$.details[0]` |
| `$.nested.field` | ネストしたフィールド | `$.data.expense.id` |

### 実例: JSONレスポンスの検証

```java
@Test
void check_200_status() throws Exception {
    mockMvc.perform(post("/expenses/{id}/reject", 29L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "reason":"申請期限切れ"
                }
                """)
            .with(httpBasic("approver@example.com", "1234")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("REJECTED"));
           // ↑ レスポンスJSONの status フィールドが "REJECTED" であることを検証
}
```

**期待されるレスポンス:**

```json
{
  "id": 29,
  "applicantId": 1,
  "title": "タクシー代",
  "amount": 5000,
  "status": "REJECTED",  ← これを検証
  "version": 2
}
```

---

## 8. テストのベストプラクティス

### ✅ 良いテストコードの特徴

#### 1. テストメソッド名は明確に

```java
// ❌ 悪い例
@Test
void test1() { ... }

// ✅ 良い例
@Test
void 認証なしで経費を提出すると401エラーが返る() { ... }

// ✅ 良い例（英語）
@Test
void submitExpense_withoutAuthentication_returns401() { ... }
```

**命名規則:**
- `メソッド名_条件_期待結果`
- 例: `submitExpense_byNonOwner_returnsConflict`

---

#### 2. テストは独立させる

```java
// ❌ 悪い例: テスト同士が依存
@Test
void test1() {
    // 経費ID=1を作成
}

@Test
void test2() {
    // 経費ID=1を使う（test1に依存）← これはNG！
}

// ✅ 良い例: 各テストで独立してデータを準備
@Test
void test1() {
    long expenseId = createExpense();  // テスト内でデータ作成
    // テスト実行
}

@Test
void test2() {
    long expenseId = createExpense();  // テスト内でデータ作成
    // テスト実行
}
```

---

#### 3. テストデータは意味のある名前で

```java
// ❌ 悪い例: マジックナンバー
@Test
void test() {
    long expenseId = 29L;  // 29って何？
}

// ✅ 良い例: 定数で意味を明確に
@Test
void test() {
    long SUBMITTED_EXPENSE_ID = 29L;  // 提出済み経費のID
}

// ✅ さらに良い例: セットアップメソッドでデータ作成
@BeforeEach
void setUp() {
    submittedExpenseId = createSubmittedExpense();
}

@Test
void test() {
    // submittedExpenseIdを使う
}
```

---

#### 4. テストの目的を1つに絞る

```java
// ❌ 悪い例: 1つのテストで複数のことを検証
@Test
void test() {
    // 経費作成をテスト
    mockMvc.perform(post("/expenses")).andExpect(status().isOk());

    // 経費提出をテスト
    mockMvc.perform(post("/expenses/1/submit")).andExpect(status().isOk());

    // 経費承認をテスト
    mockMvc.perform(post("/expenses/1/approve")).andExpect(status().isOk());
}

// ✅ 良い例: 1つのテストで1つのことを検証
@Test
void 経費作成が成功する() {
    mockMvc.perform(post("/expenses")).andExpect(status().isOk());
}

@Test
void 経費提出が成功する() {
    mockMvc.perform(post("/expenses/1/submit")).andExpect(status().isOk());
}

@Test
void 経費承認が成功する() {
    mockMvc.perform(post("/expenses/1/approve")).andExpect(status().isOk());
}
```

---

### ❌ 現在のコードの改善点

#### 問題1: コメントアウトされたコードが多い

```java
// コメントアウトされたテスト（15行目〜86行目）
//	@Test
//	void testSubmit() throws Exception { ... }
```

**改善策:**
- 不要なコードは削除する
- 必要なら別ブランチやgitの履歴で管理

---

#### 問題2: テストデータがハードコード

```java
@Test
void check_403() throws Exception {
    long expenseId = 32L;  // 32って何の経費？
}
```

**改善策:**

```java
@BeforeEach
void setUp() {
    // テストデータを準備
    draftExpense = createDraftExpense(userId: 1L);
    submittedExpense = createSubmittedExpense(userId: 1L);
    anotherUserExpense = createDraftExpense(userId: 2L);
}

@Test
void 本人以外が経費を提出すると409エラー() {
    mockMvc.perform(post("/expenses/{id}/submit", anotherUserExpense.getId())
            .with(httpBasic("user1@example.com", "pass")))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
}
```

---

#### 問題3: テストメソッド名が統一されていない

```java
// 英語と日本語が混在
void unAuthentcted_status_401() { ... }  // 英語（typo: unAuthenticated）
void check_403() { ... }  // 抽象的
```

**改善策:**

```java
// 日本語で統一
@Test
void 認証なしで経費を提出すると401エラーが返る() { ... }

@Test
void 本人以外が経費を提出すると409エラーが返る() { ... }

// または英語で統一
@Test
void submitExpense_withoutAuthentication_returns401() { ... }

@Test
void submitExpense_byNonOwner_returns409() { ... }
```

---

## 9. 次のステップ

### 🎯 学習課題

#### 課題1: テストメソッドをリファクタリング

現在のコードをベストプラクティスに沿って改善してみましょう。

**やること:**
1. テストメソッド名を統一する
2. テストデータをハードコードから定数に変更
3. AAAパターンに従ってコードを整理

---

#### 課題2: 新しいテストケースを追加

以下のテストケースを追加してみましょう：

```java
@Test
void 承認済みの経費を再度承認すると409エラー() {
    // Arrange: 承認済みの経費を準備

    // Act: 承認APIを呼び出し

    // Assert: 409エラーとエラーメッセージを検証
}

@Test
void 楽観的ロックでバージョン不一致の場合は409エラー() {
    // Arrange: version=1の経費を準備

    // Act: version=0で承認APIを呼び出し

    // Assert: 409エラーとエラーメッセージを検証
}
```

---

#### 課題3: 単体テストを書く

現在は統合テストのみですが、単体テストも書いてみましょう。

```java
@ExtendWith(MockitoExtension.class)  // Mockitoを使う
class ExpenseServiceUnitTest {

    @Mock
    ExpenseMapper expenseMapper;  // モック化

    @InjectMocks
    ExpenseService expenseService;  // テスト対象

    @Test
    void 経費を提出するとステータスがSUBMITTEDになる() {
        // Arrange
        Expense expense = Expense.createDraft(1L, "タイトル", 1000, "JPY");
        when(expenseMapper.findById(1L)).thenReturn(expense);

        // Act
        expenseService.submit(1L);

        // Assert
        verify(expenseMapper).submitDraft(1L);  // submitDraftが呼ばれたことを検証
    }
}
```

---

### 📚 参考資料

- [JUnit 5 公式ドキュメント](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/MockMvc.html)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

---

## 📝 まとめ

### テスト駆動開発で覚えておくべきこと

1. ✅ **テストは独立させる** - 他のテストに依存しない
2. ✅ **AAAパターン** - Arrange, Act, Assert
3. ✅ **テストメソッド名は明確に** - 何をテストしているか一目でわかる
4. ✅ **1つのテストで1つのことを検証** - テストの目的を絞る
5. ✅ **テストデータは意味のある名前で** - マジックナンバーを避ける

### 統合テスト vs 単体テスト

| 項目 | 統合テスト | 単体テスト |
|------|----------|----------|
| **対象** | 複数のコンポーネント | 1つのメソッド/クラス |
| **外部依存** | 実際のDB、HTTP | モック化 |
| **速度** | 遅い | 速い |
| **信頼性** | 高い（本番に近い） | 低い（モックに依存） |
| **アノテーション** | `@SpringBootTest` | `@ExtendWith(MockitoExtension.class)` |

### HTTPステータスコードの使い分け

```
200 OK         → 成功
400 Bad Request → バリデーションエラー
401 Unauthorized → 認証なし
403 Forbidden   → 権限なし
404 Not Found   → リソースなし
409 Conflict    → 状態が不正
```

---

🎉 **おめでとうございます！テスト駆動開発の基礎を学びました。**

次は実際にテストコードを書いて、品質の高いコードを目指しましょう！
