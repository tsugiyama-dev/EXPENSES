# Day 4: HTTPリクエストのテスト 🌐

**学習時間:** 2時間
**難易度:** ⭐⭐⭐☆☆

---

## 🎯 今日の目標

- MockMvcの使い方を理解する
- GET/POSTリクエストのテストを書ける
- httpBasic（認証）の使い方を覚える
- jsonPath（JSONレスポンスの検証）を使える

---

## 📖 MockMvcとは？

**MockMvc** = HTTP

リクエストをテストするツール

**特徴:**
- 実際のサーバーを起動しない
- 高速にテストできる
- Spring MVCのコントローラーをテスト

---

## 🧪 基本の使い方

### 1. セットアップ

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest              // Springアプリケーション全体を起動
@AutoConfigureMockMvc        // MockMvcを有効化
@Transactional               // テスト後にロールバック
class ExpenseApiTest {

    @Autowired
    MockMvc mockMvc;  // MockMvcを注入
}
```

---

### 2. GETリクエストのテスト

```java
@Test
@DisplayName("経費一覧を取得できる")
void 経費一覧を取得できる() throws Exception {
    // When & Then
    mockMvc.perform(
        get("/expenses")  // GETリクエスト
            .with(httpBasic("hikaru@example.com", "pass1234"))  // 認証
    )
    .andExpect(status().isOk())  // ステータスコード200
    .andExpect(jsonPath("$.items").isArray());  // JSONレスポンスを検証
}
```

---

### 3. POSTリクエストのテスト

```java
@Test
@DisplayName("経費を作成できる")
void 経費を作成できる() throws Exception {
    // Given: リクエストボディ
    String json = """
        {
            "title": "タクシー代",
            "amount": 5000,
            "currency": "JPY"
        }
        """;

    // When & Then
    mockMvc.perform(
        post("/expenses")  // POSTリクエスト
            .contentType(MediaType.APPLICATION_JSON)  // Content-Type
            .content(json)  // リクエストボディ
            .with(httpBasic("hikaru@example.com", "pass1234"))
    )
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.id").exists())
    .andExpect(jsonPath("$.title").value("タクシー代"))
    .andExpect(jsonPath("$.amount").value(5000));
}
```

---

## 🔐 認証のテスト

### httpBasic - Basic認証

```java
mockMvc.perform(
    get("/expenses")
        .with(httpBasic("email", "password"))  // 認証情報
)
```

---

### 認証なしのテスト

```java
@Test
@DisplayName("認証なしで経費一覧を取得すると401エラー")
void 認証なしで取得すると401エラー() throws Exception {
    mockMvc.perform(get("/expenses"))  // 認証なし
        .andExpect(status().isUnauthorized());  // 401
}
```

---

### 権限のテスト

```java
@Test
@DisplayName("一般ユーザーが承認すると403エラー")
void 一般ユーザーが承認すると403エラー() throws Exception {
    mockMvc.perform(
        post("/expenses/{id}/approve", 29L)
            .with(httpBasic("hikaru@example.com", "pass1234"))  // 一般ユーザー
    )
    .andExpect(status().isForbidden());  // 403
}

@Test
@DisplayName("承認者が承認できる")
void 承認者が承認できる() throws Exception {
    mockMvc.perform(
        post("/expenses/{id}/approve", 29L)
            .with(httpBasic("approver@example.com", "1234"))  // 承認者
    )
    .andExpect(status().isOk());  // 200
}
```

---

## 📄 JSONレスポンスの検証

### jsonPath - JSONを検証

```java
// レスポンス例
{
    "id": 1,
    "title": "タクシー代",
    "amount": 5000,
    "status": "SUBMITTED",
    "applicant": {
        "id": 100,
        "name": "Hikaru"
    }
}
```

```java
@Test
void JSONレスポンスの検証() throws Exception {
    mockMvc.perform(get("/expenses/1")
            .with(httpBasic("hikaru@example.com", "pass1234")))
        // トップレベルのフィールド
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("タクシー代"))
        .andExpect(jsonPath("$.amount").value(5000))
        .andExpect(jsonPath("$.status").value("SUBMITTED"))

        // ネストしたフィールド
        .andExpect(jsonPath("$.applicant.id").value(100))
        .andExpect(jsonPath("$.applicant.name").value("Hikaru"))

        // 存在確認
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.createdAt").exists());
}
```

---

## 📝 実例: 経費提出APIのテスト

### API仕様

- **エンドポイント**: `POST /expenses/{id}/submit`
- **認証**: 必須（本人のみ）
- **成功**: 200 OK
- **エラー**:
  - 401: 認証なし
  - 403: 本人以外
  - 404: 経費が存在しない
  - 409: 下書き以外

---

### テストコード

```java
@Nested
@DisplayName("経費提出API")
class SubmitExpenseTest {

    @Test
    @DisplayName("下書きの経費を提出できる")
    void 下書きの経費を提出できる() throws Exception {
        // Given: 下書きの経費（ID=32）
        long expenseId = 32L;

        // When & Then
        mockMvc.perform(
            post("/expenses/{id}/submit", expenseId)
                .with(httpBasic("hikaru@example.com", "pass1234"))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("認証なしで提出すると401エラー")
    void 認証なしで提出すると401エラー() throws Exception {
        mockMvc.perform(post("/expenses/{id}/submit", 32L))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("本人以外が提出すると409エラー")
    void 本人以外が提出すると409エラー() throws Exception {
        // Given: hikaru さんの経費
        long expenseId = 32L;

        // When: yasuko さんが提出
        mockMvc.perform(
            post("/expenses/{id}/submit", expenseId)
                .with(httpBasic("yasuko@example.com", "pass1234"))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
    }

    @Test
    @DisplayName("存在しない経費を提出すると404エラー")
    void 存在しない経費を提出すると404エラー() throws Exception {
        mockMvc.perform(
            post("/expenses/{id}/submit", 9999L)
                .with(httpBasic("hikaru@example.com", "pass1234"))
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
    }

    @Test
    @DisplayName("下書き以外を提出すると409エラー")
    void 下書き以外を提出すると409エラー() throws Exception {
        // Given: 既に提出済みの経費
        long expenseId = 29L;

        mockMvc.perform(
            post("/expenses/{id}/submit", expenseId)
                .with(httpBasic("hikaru@example.com", "pass1234"))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("下書き以外提出できません"));
    }
}
```

---

## 📝 演習問題

### 演習1: 経費却下APIのテスト

**API仕様:**
- **エンドポイント**: `POST /expenses/{id}/reject`
- **リクエストボディ**: `{ "reason": "却下理由" }`
- **認証**: 承認者のみ
- **成功**: 200 OK
- **エラー**:
  - 400: 却下理由が空
  - 401: 認証なし
  - 403: 承認者以外
  - 409: 提出済み以外

**問題:** 5つのテストケースを書いてください。

<details>
<summary>ヒント</summary>

1. 正常系: 提出済みの経費を却下できる
2. 異常系: 却下理由が空の場合は400エラー
3. 異常系: 認証なしの場合は401エラー
4. 異常系: 一般ユーザーの場合は403エラー
5. 異常系: 提出済み以外の場合は409エラー

</details>

<details>
<summary>解答</summary>

```java
@Nested
@DisplayName("経費却下API")
class RejectExpenseTest {

    @Test
    @DisplayName("提出済みの経費を却下できる")
    void 提出済みの経費を却下できる() throws Exception {
        String json = """
            {
                "reason": "申請期限切れ"
            }
            """;

        mockMvc.perform(
            post("/expenses/{id}/reject", 29L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(httpBasic("approver@example.com", "1234"))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("却下理由が空の場合は400エラー")
    void 却下理由が空の場合は400エラー() throws Exception {
        String json = """
            {
                "reason": ""
            }
            """;

        mockMvc.perform(
            post("/expenses/{id}/reject", 29L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(httpBasic("approver@example.com", "1234"))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
    }

    @Test
    @DisplayName("認証なしで却下すると401エラー")
    void 認証なしで却下すると401エラー() throws Exception {
        String json = """
            {
                "reason": "申請期限切れ"
            }
            """;

        mockMvc.perform(
            post("/expenses/{id}/reject", 29L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("一般ユーザーが却下すると403エラー")
    void 一般ユーザーが却下すると403エラー() throws Exception {
        String json = """
            {
                "reason": "申請期限切れ"
            }
            """;

        mockMvc.perform(
            post("/expenses/{id}/reject", 29L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(httpBasic("hikaru@example.com", "pass1234"))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("提出済み以外を却下すると409エラー")
    void 提出済み以外を却下すると409エラー() throws Exception {
        String json = """
            {
                "reason": "申請期限切れ"
            }
            """;

        mockMvc.perform(
            post("/expenses/{id}/reject", 32L)  // 下書き
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(httpBasic("approver@example.com", "1234"))
        )
        .andExpect(status().isConflict());
    }
}
```

</details>

---

## 📝 今日のまとめ

### できるようになったこと

✅ MockMvcの基本を理解した
✅ GET/POSTリクエストのテストを書ける
✅ httpBasicで認証のテストができる
✅ jsonPathでJSONレスポンスを検証できる

---

### よく使うメソッド一覧

```java
// リクエスト
mockMvc.perform(
    get("/path")                                // GETリクエスト
    post("/path")                               // POSTリクエスト
    .contentType(MediaType.APPLICATION_JSON)    // Content-Type
    .content(json)                              // リクエストボディ
    .with(httpBasic("email", "password"))       // 認証
)

// ステータスコード
.andExpect(status().isOk())                     // 200
.andExpect(status().isCreated())                // 201
.andExpect(status().isBadRequest())             // 400
.andExpect(status().isUnauthorized())           // 401
.andExpect(status().isForbidden())              // 403
.andExpect(status().isNotFound())               // 404
.andExpect(status().isConflict())               // 409

// JSONレスポンス
.andExpect(jsonPath("$.id").value(1))           // 値の検証
.andExpect(jsonPath("$.id").exists())           // 存在確認
.andExpect(jsonPath("$.items").isArray())       // 配列の検証
```

---

### 次のステップ

明日はテストの整理とリファクタリングを学びます！

👉 [Day 5: テストの整理とリファクタリング](./day5_refactoring.md)

---

お疲れさまでした！
