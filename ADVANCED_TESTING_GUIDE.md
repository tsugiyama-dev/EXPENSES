# 実務レベルのテスト完全ガイド 🧪

## 📚 目次

1. [テストピラミッド](#1-テストピラミッド)
2. [単体テスト（Unit Test）の実装](#2-単体テストunit-testの実装)
3. [統合テスト（Integration Test）の実装](#3-統合テストintegration-testの実装)
4. [テストダブルの使い分け](#4-テストダブルの使い分け)
5. [Given-When-Then パターン](#5-given-when-then-パターン)
6. [パラメータ化テスト](#6-パラメータ化テスト)
7. [テストデータビルダーパターン](#7-テストデータビルダーパターン)
8. [例外のテスト](#8-例外のテスト)
9. [テストカバレッジ](#9-テストカバレッジ)
10. [実務でのテスト戦略](#10-実務でのテスト戦略)
11. [リファクタリング演習](#11-リファクタリング演習)

---

## 1. テストピラミッド

### テストピラミッドとは？

テストの種類と量のバランスを示すモデルです。

```
        /\
       /E2E\        ← 少ない（遅い、壊れやすい）
      /------\
     / 統合  \      ← 中程度
    /----------\
   /   単体    \    ← 多い（速い、安定）
  /--------------\
```

### テストの種類と特徴

| テスト | 対象 | 速度 | 安定性 | コスト | 割合 |
|--------|------|------|--------|--------|------|
| **単体テスト** | 1つのクラス・メソッド | ⚡ 速い | ✅ 安定 | 💰 低 | **70%** |
| **統合テスト** | 複数コンポーネント | 🐢 遅い | ⚠️ 普通 | 💰💰 中 | **20%** |
| **E2Eテスト** | システム全体 | 🐌 とても遅い | ❌ 不安定 | 💰💰💰 高 | **10%** |

### なぜこのバランスが重要か？

```
❌ アンチパターン：E2Eテストばかり書く
  ↓
実行に30分かかる
  ↓
開発者がテストを実行しなくなる
  ↓
バグが増える

✅ 推奨パターン：ピラミッド型
  ↓
単体テストが数秒で実行
  ↓
開発者が頻繁にテストを実行
  ↓
バグを早期発見
```

---

## 2. 単体テスト（Unit Test）の実装

### 単体テストとは？

- **1つのクラス・メソッド**だけをテスト
- **外部依存をモック化**（DB、外部API、他のサービス）
- **高速に実行**できる

---

### 実例1: normalizedOrderBy メソッドのテスト

**ExpenseService.java:241-272**

```java
private String normalizedOrderBy(String sort) {
    if(sort == null || sort.isBlank()) {
        return "created_at";
    }

    String[] parts = sort.split(",");
    String key = parts[0].trim();

    String column = switch(key) {
        case "created_at" -> "created_at";
        case "updated_at" -> "updated_at";
        case "submitted_at" -> "submitted_at";
        case "amount" -> "amount";
        case "id" -> "id";
        default -> "created_at";
    };

    if(!ALLOWED_SORTS.contains(column)) {
        return "created_at";
    }
    return column;
}
```

**単体テストの実装:**

```java
package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("ExpenseService 単体テスト")
class ExpenseServiceUnitTest {

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        // モックを使わずに直接インスタンス化
        // privateメソッドはリフレクションでテスト
        expenseService = new ExpenseService(null, null, null, null, null);
    }

    @Nested
    @DisplayName("normalizedOrderBy のテスト")
    class NormalizedOrderByTest {

        @Test
        @DisplayName("nullの場合はcreated_atを返す")
        void nullを渡すとcreated_atを返す() throws Exception {
            // Given
            String sort = null;

            // When
            String result = invokePrivateMethod("normalizedOrderBy", sort);

            // Then
            assertThat(result).isEqualTo("created_at");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("nullまたは空文字の場合はcreated_atを返す")
        void nullまたは空文字の場合はデフォルト値(String sort) throws Exception {
            // When
            String result = invokePrivateMethod("normalizedOrderBy", sort);

            // Then
            assertThat(result).isEqualTo("created_at");
        }

        @ParameterizedTest
        @CsvSource({
            "created_at,    created_at",
            "updated_at,    updated_at",
            "submitted_at,  submitted_at",
            "amount,        amount",
            "id,            id"
        })
        @DisplayName("許可されたソート条件は正しく変換される")
        void 許可されたソート条件(String input, String expected) throws Exception {
            // When
            String result = invokePrivateMethod("normalizedOrderBy", input);

            // Then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
            "invalid",
            "DROP TABLE expenses",
            "'; DROP TABLE expenses; --"
        })
        @DisplayName("不正なソート条件はcreated_atにフォールバック")
        void 不正なソート条件はデフォルト値(String input) throws Exception {
            // When
            String result = invokePrivateMethod("normalizedOrderBy", input);

            // Then
            assertThat(result).isEqualTo("created_at");
        }

        @Test
        @DisplayName("カンマ区切りの場合は最初の値を使う")
        void カンマ区切りの場合() throws Exception {
            // Given
            String sort = "amount,DESC";

            // When
            String result = invokePrivateMethod("normalizedOrderBy", sort);

            // Then
            assertThat(result).isEqualTo("amount");
        }

        @Test
        @DisplayName("前後の空白は削除される")
        void 前後の空白は削除される() throws Exception {
            // Given
            String sort = "  amount  ,  DESC  ";

            // When
            String result = invokePrivateMethod("normalizedOrderBy", sort);

            // Then
            assertThat(result).isEqualTo("amount");
        }
    }

    // privateメソッドをテストするヘルパー（リフレクション）
    private String invokePrivateMethod(String methodName, String arg) throws Exception {
        var method = ExpenseService.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (String) method.invoke(expenseService, arg);
    }
}
```

**ポイント:**
- ✅ `@DisplayName` で日本語のテスト名
- ✅ `@Nested` でテストをグループ化
- ✅ `@ParameterizedTest` で複数パターンを1つのテストで検証
- ✅ AssertJの流暢なAPI（`assertThat().isEqualTo()`）

---

### 実例2: approve メソッドのテスト（Mockitoを使用）

**ExpenseService.java:138-176**

```java
@Transactional
public ExpenseResponse approve(long expenseId, int version, Long actorId) {
    Expense expense = expenseMapper.findById(expenseId);

    if(expense == null) {
        throw new BusinessException("NOT_FOUND", "経費申請が見つかりません: EXPENSEID ：" + expenseId, traceId());
    }

    if(expense.getStatus() != ExpenseStatus.SUBMITTED) {
        throw new BusinessException("INVALID_STATUS_TRANSITION", "提出済み以外は承認できません", traceId());
    }

    if(expense.getVersion()!= version) {
        throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
    }

    int updated = expenseMapper.approve(expenseId, version);
    if(updated == 0) {
        throw new BusinessException("CONCURRENT_MODIFICATION", "他のユーザに更新されています", traceId());
    }

    auditLogMapper.insert(ExpenseAuditLog.createApprove(expenseId, actorId, traceId()));
    notificationService.notifyApproved(getApplicantAddress(expense.getApplicantId()), expenseId, traceId());

    var saved = expenseMapper.findById(expenseId);
    return ExpenseResponse.toResponse(saved);
}
```

**単体テストの実装:**

```java
package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.notification.NotificationService;
import com.example.expenses.repository.ExpenseAuditLogMapper;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.repository.UserMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService.approve() のテスト")
class ExpenseServiceApproveTest {

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ExpenseAuditLogMapper auditLogMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CreateCsvService createCsvService;

    @InjectMocks
    private ExpenseService expenseService;

    private Expense submittedExpense;
    private Expense approvedExpense;

    @BeforeEach
    void setUp() {
        // テストデータの準備
        submittedExpense = new Expense(
            1L,                      // id
            100L,                    // applicantId
            "タクシー代",
            new BigDecimal("5000"),
            "JPY",
            ExpenseStatus.SUBMITTED, // status
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            1                        // version
        );

        approvedExpense = new Expense(
            1L,
            100L,
            "タクシー代",
            new BigDecimal("5000"),
            "JPY",
            ExpenseStatus.APPROVED,  // status
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            2                        // version
        );
    }

    @Nested
    @DisplayName("正常系")
    class SuccessCase {

        @Test
        @DisplayName("提出済みの経費を承認できる")
        void 提出済みの経費を承認できる() {
            // Given
            long expenseId = 1L;
            int version = 1;
            long actorId = 999L;

            given(expenseMapper.findById(expenseId))
                .willReturn(submittedExpense)    // 1回目: 提出済み
                .willReturn(approvedExpense);    // 2回目: 承認済み

            given(expenseMapper.approve(expenseId, version)).willReturn(1);
            given(userMapper.findEmailById(100L)).willReturn("applicant@example.com");

            // When
            ExpenseResponse result = expenseService.approve(expenseId, version, actorId);

            // Then
            assertThat(result.status()).isEqualTo(ExpenseStatus.APPROVED);
            assertThat(result.version()).isEqualTo(2);

            // モックの検証
            then(expenseMapper).should(times(2)).findById(expenseId);
            then(expenseMapper).should().approve(expenseId, version);
            then(auditLogMapper).should().insert(any());
            then(notificationService).should().notifyApproved(
                eq("applicant@example.com"),
                eq(expenseId),
                anyString()
            );
        }
    }

    @Nested
    @DisplayName("異常系")
    class ErrorCase {

        @Test
        @DisplayName("経費が存在しない場合はBusinessExceptionをスロー")
        void 経費が存在しない場合() {
            // Given
            long expenseId = 9999L;
            int version = 1;
            long actorId = 999L;

            given(expenseMapper.findById(expenseId)).willReturn(null);

            // When & Then
            assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("経費申請が見つかりません");

            // モックの検証
            then(expenseMapper).should().findById(expenseId);
            then(expenseMapper).should(never()).approve(anyLong(), anyInt());
        }

        @Test
        @DisplayName("ステータスがSUBMITTED以外の場合はBusinessExceptionをスロー")
        void ステータスが提出済み以外の場合() {
            // Given
            long expenseId = 1L;
            int version = 1;
            long actorId = 999L;

            Expense draftExpense = new Expense(
                1L, 100L, "タクシー代", new BigDecimal("5000"), "JPY",
                ExpenseStatus.DRAFT,  // DRAFT状態
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 1
            );

            given(expenseMapper.findById(expenseId)).willReturn(draftExpense);

            // When & Then
            assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("提出済み以外は承認できません");
        }

        @Test
        @DisplayName("バージョンが一致しない場合はBusinessExceptionをスロー")
        void バージョンが一致しない場合() {
            // Given
            long expenseId = 1L;
            int version = 999;  // 不一致
            long actorId = 999L;

            given(expenseMapper.findById(expenseId)).willReturn(submittedExpense);

            // When & Then
            assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("他のユーザに更新されています");
        }

        @Test
        @DisplayName("更新件数が0の場合はBusinessExceptionをスロー")
        void 更新件数が0の場合() {
            // Given
            long expenseId = 1L;
            int version = 1;
            long actorId = 999L;

            given(expenseMapper.findById(expenseId)).willReturn(submittedExpense);
            given(expenseMapper.approve(expenseId, version)).willReturn(0);  // 更新失敗

            // When & Then
            assertThatThrownBy(() -> expenseService.approve(expenseId, version, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("他のユーザに更新されています");
        }
    }

    @Nested
    @DisplayName("境界値")
    class BoundaryCase {

        @Test
        @DisplayName("メール送信が失敗しても処理は継続する")
        void メール送信失敗時の挙動() {
            // Given
            long expenseId = 1L;
            int version = 1;
            long actorId = 999L;

            given(expenseMapper.findById(expenseId))
                .willReturn(submittedExpense)
                .willReturn(approvedExpense);
            given(expenseMapper.approve(expenseId, version)).willReturn(1);
            given(userMapper.findEmailById(100L)).willReturn("applicant@example.com");

            // メール送信で例外をスロー
            willThrow(new RuntimeException("Mail server error"))
                .given(notificationService).notifyApproved(anyString(), anyLong(), anyString());

            // When
            ExpenseResponse result = expenseService.approve(expenseId, version, actorId);

            // Then
            assertThat(result.status()).isEqualTo(ExpenseStatus.APPROVED);

            // メール送信は試みられるが、例外は握りつぶされる
            then(notificationService).should().notifyApproved(anyString(), anyLong(), anyString());
        }
    }
}
```

**ポイント:**
- ✅ `@Mock` で依存を全てモック化
- ✅ `@InjectMocks` でテスト対象にモックを注入
- ✅ `given().willReturn()` でモックの振る舞いを定義
- ✅ `then().should()` でモックが呼ばれたことを検証
- ✅ `@Nested` で正常系・異常系・境界値をグループ化

---

## 3. 統合テスト（Integration Test）の実装

### 統合テストとは？

- **複数のコンポーネント**を組み合わせてテスト
- **実際のDB**を使う（@Transactionalでロールバック）
- **外部サービスはモック化**（NotificationServiceなど）

---

### 実例: 統合テストの実装

**現在のコードの改善版:**

```java
package com.example.expenses.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.repository.ExpenseMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("経費申請API 統合テスト")
class ExpenseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseMapper expenseMapper;

    private Expense draftExpense;
    private Expense submittedExpense;
    private Expense approvedExpense;

    @BeforeEach
    void setUp() {
        // テストデータの準備（実際のDBに挿入）
        draftExpense = expenseMapper.findById(32L);      // 下書き
        submittedExpense = expenseMapper.findById(29L);  // 提出済み
        approvedExpense = expenseMapper.findById(25L);   // 承認済み
    }

    @Nested
    @DisplayName("経費提出API")
    class SubmitExpenseTest {

        @Test
        @DisplayName("下書きの経費を提出できる")
        void 下書きの経費を提出できる() throws Exception {
            // Given
            long expenseId = draftExpense.getId();

            // When & Then
            mockMvc.perform(post("/expenses/{id}/submit", expenseId)
                    .with(httpBasic("hikaru@example.com", "pass1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

            // DBの状態を検証
            Expense updated = expenseMapper.findById(expenseId);
            assertThat(updated.getStatus()).isEqualTo(ExpenseStatus.SUBMITTED);
        }

        @Test
        @DisplayName("認証なしで提出すると401エラー")
        void 認証なしで提出すると401エラー() throws Exception {
            // When & Then
            mockMvc.perform(post("/expenses/{id}/submit", draftExpense.getId()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("本人以外が提出すると409エラー")
        void 本人以外が提出すると409エラー() throws Exception {
            // Given
            long expenseId = draftExpense.getId();

            // When & Then
            mockMvc.perform(post("/expenses/{id}/submit", expenseId)
                    .with(httpBasic("yasuko@example.com", "pass1234")))  // 他人
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
        }

        @Test
        @DisplayName("存在しない経費を提出すると404エラー")
        void 存在しない経費を提出すると404エラー() throws Exception {
            // When & Then
            mockMvc.perform(post("/expenses/{id}/submit", 9999L)
                    .with(httpBasic("hikaru@example.com", "pass1234")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("対象データが見つかりません"));
        }

        @Test
        @DisplayName("下書き以外を提出すると409エラー")
        void 下書き以外を提出すると409エラー() throws Exception {
            // Given
            long expenseId = submittedExpense.getId();  // 既に提出済み

            // When & Then
            mockMvc.perform(post("/expenses/{id}/submit", expenseId)
                    .with(httpBasic("hikaru@example.com", "pass1234")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("下書き以外提出できません"));
        }
    }

    @Nested
    @DisplayName("経費承認API")
    class ApproveExpenseTest {

        @Test
        @DisplayName("提出済みの経費を承認できる")
        void 提出済みの経費を承認できる() throws Exception {
            // Given
            long expenseId = submittedExpense.getId();
            int version = submittedExpense.getVersion();

            // When & Then
            mockMvc.perform(post("/expenses/{id}/approve", expenseId)
                    .param("version", String.valueOf(version))
                    .with(httpBasic("approver@example.com", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

            // DBの状態を検証
            Expense updated = expenseMapper.findById(expenseId);
            assertThat(updated.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
            assertThat(updated.getVersion()).isEqualTo(version + 1);
        }

        @Test
        @DisplayName("一般ユーザーが承認すると403エラー")
        void 一般ユーザーが承認すると403エラー() throws Exception {
            // Given
            long expenseId = submittedExpense.getId();

            // When & Then
            mockMvc.perform(post("/expenses/{id}/approve", expenseId)
                    .with(httpBasic("hikaru@example.com", "pass1234")))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("提出済み以外を承認すると409エラー")
        void 提出済み以外を承認すると409エラー() throws Exception {
            // Given
            long expenseId = draftExpense.getId();  // 下書き

            // When & Then
            mockMvc.perform(post("/expenses/{id}/approve", expenseId)
                    .with(httpBasic("approver@example.com", "1234")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("提出済み以外は承認できません"));
        }

        @Test
        @DisplayName("バージョン不一致で409エラー")
        void バージョン不一致で409エラー() throws Exception {
            // Given
            long expenseId = submittedExpense.getId();
            int wrongVersion = 999;  // 不正なバージョン

            // When & Then
            mockMvc.perform(post("/expenses/{id}/approve", expenseId)
                    .param("version", String.valueOf(wrongVersion))
                    .with(httpBasic("approver@example.com", "1234")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("他のユーザに更新されています"));
        }
    }

    @Nested
    @DisplayName("経費却下API")
    class RejectExpenseTest {

        @Test
        @DisplayName("提出済みの経費を却下できる")
        void 提出済みの経費を却下できる() throws Exception {
            // Given
            long expenseId = submittedExpense.getId();
            int version = submittedExpense.getVersion();
            String json = """
                {
                    "reason": "申請期限切れ"
                }
                """;

            // When & Then
            mockMvc.perform(post("/expenses/{id}/reject", expenseId)
                    .param("version", String.valueOf(version))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
                    .with(httpBasic("approver@example.com", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

            // DBの状態を検証
            Expense updated = expenseMapper.findById(expenseId);
            assertThat(updated.getStatus()).isEqualTo(ExpenseStatus.REJECTED);
        }

        @Test
        @DisplayName("却下理由が空の場合は400エラー")
        void 却下理由が空の場合は400エラー() throws Exception {
            // Given
            String json = """
                {
                    "reason": ""
                }
                """;

            // When & Then
            mockMvc.perform(post("/expenses/{id}/reject", submittedExpense.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
                    .with(httpBasic("approver@example.com", "1234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].message").value("却下理由は必須です"));
        }
    }
}
```

**改善ポイント:**
- ✅ `@BeforeEach` でテストデータを準備
- ✅ `@Nested` で機能ごとにグループ化
- ✅ `@DisplayName` で日本語のテスト名
- ✅ AssertJでDBの状態を検証
- ✅ テストメソッド名が具体的

---

## 4. テストダブルの使い分け

### テストダブルとは？

テスト対象の**依存オブジェクトを置き換える**ための仕組みです。

| 種類 | 用途 | 特徴 |
|------|------|------|
| **Mock** | 振る舞いを検証 | `verify()` で呼び出しを検証 |
| **Stub** | 固定値を返す | `when().thenReturn()` |
| **Spy** | 一部だけモック化 | 実際のオブジェクトをラップ |
| **Fake** | 簡易実装 | メモリ上のDB（H2など） |

---

### 実例1: Mock（振る舞いを検証）

```java
@Test
void 承認時に監査ログが記録される() {
    // Given
    given(expenseMapper.findById(1L)).willReturn(submittedExpense);
    given(expenseMapper.approve(1L, 1)).willReturn(1);

    // When
    expenseService.approve(1L, 1, 999L);

    // Then
    // モックが呼ばれたことを検証
    then(auditLogMapper).should().insert(any());
}
```

**ポイント:**
- `should()` で呼び出しを検証
- `times()`, `never()` で呼び出し回数を指定

---

### 実例2: Stub（固定値を返す）

```java
@Test
void ユーザーのメールアドレスを取得できる() {
    // Given
    given(userMapper.findEmailById(100L)).willReturn("user@example.com");

    // When
    String email = expenseService.getApplicantAddress(100L);

    // Then
    assertThat(email).isEqualTo("user@example.com");
}
```

**ポイント:**
- `willReturn()` で固定値を返す
- 呼び出しの検証は不要

---

### 実例3: Spy（一部だけモック化）

```java
@Test
void privateメソッドだけモック化() {
    // Given
    ExpenseService spy = spy(expenseService);
    doReturn("created_at").when(spy).normalizedOrderBy(anyString());

    // When
    // 実際のメソッドを呼び出すが、normalizedOrderByだけモック化
    var result = spy.search(...);

    // Then
    assertThat(result).isNotNull();
}
```

**ポイント:**
- 実際のオブジェクトをラップ
- 一部のメソッドだけモック化

---

### 実例4: Fake（簡易実装）

```java
// メモリ上のDB（H2）を使った統合テスト
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseIntegrationTest {
    // H2がFakeとして動作
    @Autowired
    private ExpenseMapper expenseMapper;

    @Test
    void 経費を作成できる() {
        Expense expense = Expense.createDraft(...);
        expenseMapper.insert(expense);

        Expense saved = expenseMapper.findById(expense.getId());
        assertThat(saved).isNotNull();
    }
}
```

**ポイント:**
- H2データベースが実際のDBの代わり
- テスト後に自動でロールバック

---

## 5. Given-When-Then パターン

### BDD（振る舞い駆動開発）のパターン

```java
@Test
void テスト名() {
    // Given（前提条件）: テストに必要なデータを準備
    long expenseId = 1L;
    int version = 1;

    // When（実行）: テスト対象のメソッドを実行
    ExpenseResponse result = expenseService.approve(expenseId, version, 999L);

    // Then（検証）: 結果を検証
    assertThat(result.status()).isEqualTo(ExpenseStatus.APPROVED);
}
```

### Mockitoの場合

```java
@Test
void モックを使ったテスト() {
    // Given: モックの振る舞いを定義
    given(expenseMapper.findById(1L)).willReturn(submittedExpense);

    // When: テスト対象を実行
    expenseService.approve(1L, 1, 999L);

    // Then: モックの呼び出しを検証
    then(auditLogMapper).should().insert(any());
}
```

---

## 6. パラメータ化テスト

### 同じロジックで異なる入力値をテスト

```java
@ParameterizedTest
@CsvSource({
    "created_at,    created_at",
    "updated_at,    updated_at",
    "submitted_at,  submitted_at",
    "amount,        amount",
    "id,            id"
})
@DisplayName("許可されたソート条件は正しく変換される")
void 許可されたソート条件(String input, String expected) {
    // When
    String result = expenseService.normalizedOrderBy(input);

    // Then
    assertThat(result).isEqualTo(expected);
}
```

### よく使うアノテーション

| アノテーション | 用途 |
|---------------|------|
| `@ValueSource` | 単一の値のリスト |
| `@CsvSource` | CSV形式の複数値 |
| `@MethodSource` | メソッドから値を取得 |
| `@NullAndEmptySource` | nullと空文字列 |
| `@EnumSource` | Enumの全値 |

---

### 実例: @MethodSource

```java
@ParameterizedTest
@MethodSource("invalidSortValues")
@DisplayName("不正なソート条件はデフォルト値にフォールバック")
void 不正なソート条件(String input) {
    String result = expenseService.normalizedOrderBy(input);
    assertThat(result).isEqualTo("created_at");
}

static Stream<String> invalidSortValues() {
    return Stream.of(
        "invalid",
        "DROP TABLE expenses",
        "'; DROP TABLE expenses; --",
        "1' OR '1'='1"
    );
}
```

---

## 7. テストデータビルダーパターン

### 問題: テストデータの作成が冗長

```java
// ❌ 悪い例: 毎回全部書く
Expense expense1 = new Expense(
    1L, 100L, "タクシー代", new BigDecimal("5000"), "JPY",
    ExpenseStatus.SUBMITTED, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 1
);

Expense expense2 = new Expense(
    2L, 100L, "ランチ代", new BigDecimal("1000"), "JPY",
    ExpenseStatus.SUBMITTED, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 1
);
```

### 解決策: ビルダーパターン

```java
// ✅ 良い例: ビルダーを使う
class ExpenseTestBuilder {

    private Long id = 1L;
    private Long applicantId = 100L;
    private String title = "テスト経費";
    private BigDecimal amount = new BigDecimal("5000");
    private String currency = "JPY";
    private ExpenseStatus status = ExpenseStatus.DRAFT;
    private LocalDateTime submittedAt = LocalDateTime.now();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private int version = 1;

    public static ExpenseTestBuilder builder() {
        return new ExpenseTestBuilder();
    }

    public ExpenseTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public ExpenseTestBuilder applicantId(Long applicantId) {
        this.applicantId = applicantId;
        return this;
    }

    public ExpenseTestBuilder title(String title) {
        this.title = title;
        return this;
    }

    public ExpenseTestBuilder amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public ExpenseTestBuilder status(ExpenseStatus status) {
        this.status = status;
        return this;
    }

    public ExpenseTestBuilder version(int version) {
        this.version = version;
        return this;
    }

    public Expense build() {
        return new Expense(
            id, applicantId, title, amount, currency,
            status, submittedAt, createdAt, updatedAt, version
        );
    }

    // よく使うパターンを定義
    public static Expense createDraft() {
        return builder()
            .status(ExpenseStatus.DRAFT)
            .build();
    }

    public static Expense createSubmitted() {
        return builder()
            .status(ExpenseStatus.SUBMITTED)
            .build();
    }

    public static Expense createApproved() {
        return builder()
            .status(ExpenseStatus.APPROVED)
            .build();
    }
}
```

**使い方:**

```java
@Test
void テスト() {
    // デフォルト値でビルド
    Expense expense1 = ExpenseTestBuilder.createDraft();

    // 一部だけカスタマイズ
    Expense expense2 = ExpenseTestBuilder.builder()
        .title("ランチ代")
        .amount(new BigDecimal("1000"))
        .status(ExpenseStatus.SUBMITTED)
        .build();

    // テスト実行
}
```

---

## 8. 例外のテスト

### AssertJを使った例外のテスト

```java
@Test
@DisplayName("経費が存在しない場合はBusinessExceptionをスロー")
void 経費が存在しない場合() {
    // Given
    given(expenseMapper.findById(9999L)).willReturn(null);

    // When & Then
    assertThatThrownBy(() -> expenseService.approve(9999L, 1, 999L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("経費申請が見つかりません")
        .hasFieldOrPropertyWithValue("errorCode", "NOT_FOUND");
}
```

### JUnit 5の assertThrows

```java
@Test
void 例外をスロー() {
    // When & Then
    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> expenseService.approve(9999L, 1, 999L)
    );

    assertThat(exception.getMessage()).contains("経費申請が見つかりません");
}
```

---

## 9. テストカバレッジ

### カバレッジとは？

テストがコードの何%をカバーしているかを示す指標です。

| 種類 | 意味 |
|------|------|
| **行カバレッジ** | 実行された行の割合 |
| **分岐カバレッジ** | 実行された条件分岐の割合 |
| **メソッドカバレッジ** | 実行されたメソッドの割合 |

### カバレッジの目安

| レイヤー | 目標 |
|---------|------|
| **ドメインロジック** | 90%以上 |
| **サービス層** | 80%以上 |
| **コントローラー** | 60%以上 |
| **設定クラス** | 測定不要 |

### カバレッジを計測

```bash
# Mavenの場合
mvn test jacoco:report

# レポートを確認
open target/site/jacoco/index.html
```

### カバレッジ100%は必要か？

**答え: NO**

```
❌ 100%を目指す
  ↓
getter/setterまでテストを書く
  ↓
メンテナンスコストが増える
  ↓
テストが足枷になる

✅ 重要なロジックを優先
  ↓
ビジネスロジックを重点的にテスト
  ↓
バグの多い箇所を重点的にテスト
  ↓
費用対効果が高い
```

---

## 10. 実務でのテスト戦略

### テストを書く優先順位

| 優先度 | 対象 | 理由 |
|--------|------|------|
| **1** | ビジネスロジック | バグの影響が大きい |
| **2** | バリデーション | ユーザー入力は信頼できない |
| **3** | 状態遷移 | ステータス管理は複雑 |
| **4** | 例外処理 | エラーハンドリングは重要 |
| **5** | 境界値 | バグが起きやすい |

### 書かなくてもよいテスト

- getter/setterのテスト
- フレームワークのテスト（Spring Securityなど）
- 定数のテスト

### テストの命名規則

```java
// ❌ 悪い例
@Test
void test1() { ... }

// ✅ 良い例（日本語）
@Test
@DisplayName("提出済みの経費を承認できる")
void 提出済みの経費を承認できる() { ... }

// ✅ 良い例（英語）
@Test
@DisplayName("Should approve submitted expense")
void shouldApproveSubmittedExpense() { ... }
```

### テストのメンテナンス

```
仕様変更
  ↓
テストが失敗
  ↓
テストを修正
  ↓
リファクタリング
  ↓
テストが失敗しない
  ↓
安全にリファクタリングできる
```

---

## 11. リファクタリング演習

### 課題1: 現在の統合テストをリファクタリング

**Before:**

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

**After:**

```java
@Nested
@DisplayName("経費提出API")
class SubmitExpenseTest {

    @Test
    @DisplayName("本人以外が提出すると409エラー")
    void 本人以外が提出すると409エラー() throws Exception {
        // Given
        long othersExpenseId = 32L;  // 他人の経費

        // When & Then
        mockMvc.perform(post("/expenses/{id}/submit", othersExpenseId)
                .with(httpBasic("hikaru@example.com", "pass1234")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("本人以外は提出できません"));
    }
}
```

---

### 課題2: 単体テストを追加

**normalizedDirection メソッド:**

```java
private String normalizedDirection(String sort) {
    if(sort == null || sort.isBlank()) return "DESC";

    String[] parts = sort.split(",");
    if(parts.length < 2) return "DESC";

    String dir = parts[1].trim();
    return "asc".equalsIgnoreCase(dir) ? "ASC" : "DESC";
}
```

**テスト:**

```java
@Nested
@DisplayName("normalizedDirection のテスト")
class NormalizedDirectionTest {

    @ParameterizedTest
    @CsvSource({
        "created_at,asc,  ASC",
        "created_at,ASC,  ASC",
        "created_at,desc, DESC",
        "created_at,DESC, DESC",
        "created_at,invalid, DESC"
    })
    @DisplayName("ソート方向を正しく正規化する")
    void ソート方向を正しく正規化(String input, String expected) {
        String result = expenseService.normalizedDirection(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("nullの場合はDESCを返す")
    void nullの場合はDESC() {
        assertThat(expenseService.normalizedDirection(null)).isEqualTo("DESC");
    }

    @Test
    @DisplayName("カンマがない場合はDESCを返す")
    void カンマがない場合はDESC() {
        assertThat(expenseService.normalizedDirection("created_at")).isEqualTo("DESC");
    }
}
```

---

### 課題3: テストデータビルダーを作成

```java
class ExpenseResponseTestBuilder {
    // 実装してみよう！
}
```

---

## 📝 まとめ

### テスト駆動開発のサイクル

```
1. Red（失敗するテストを書く）
   ↓
2. Green（最小限のコードで通す）
   ↓
3. Refactor（リファクタリング）
   ↓
1に戻る
```

### テストを書くメリット

- ✅ バグの早期発見
- ✅ リファクタリングの安全性
- ✅ ドキュメント代わり
- ✅ 設計の改善

### 実務で重要なこと

1. **テストピラミッドを守る** - 単体70%, 統合20%, E2E10%
2. **重要なロジックを優先** - カバレッジ100%は不要
3. **テストを読みやすく** - Given-When-Then, @Nested, @DisplayName
4. **テストも保守する** - 壊れたテストは信頼を失う

---

🎉 **おめでとうございます！実務レベルのテストスキルを学びました。**

次は実際にテストを書いて、品質の高いコードを目指しましょう！
