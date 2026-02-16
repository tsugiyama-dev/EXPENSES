# リッチドメインモデル - 正しい実装（修正版）

## 🚨 重要な修正点

ご指摘の通り、以前のコードには**設計上の矛盾**がありました：

1. **バージョン管理の重複**: ドメインとSQLの両方でversion++していた
2. **ステータス更新の無意味さ**: ドメインでstatusを変更してもSQLが固定値を使っていた
3. **UPSERT的な不自然さ**: ドメインで状態を作るのにSQLが特定操作専用だった

この修正版では**真のリッチドメインモデル**を実装します。

---

## ✅ 正しい実装

### 設計思想

**ドメインが真実の源泉（Single Source of Truth）**

- ドメインで完全に状態を管理
- SQLは汎用的な更新処理
- ドメインの状態をそのまま永続化

---

## 1. ドメインエンティティ（完全版）

```java
// src/main/java/com/example/expenses/domain/Expense.java
package com.example.expenses.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 経費エンティティ（リッチドメインモデル）
 *
 * 重要な設計原則:
 * 1. ドメインが状態の真実の源泉
 * 2. ビジネスロジックはドメイン内に実装
 * 3. 不正な状態遷移を防ぐ
 * 4. バージョン管理もドメインで行う
 */
@Getter
public class Expense {
    private Long id;
    private Long applicantId;
    private String title;
    private BigDecimal amount;
    private String currency;
    private ExpenseStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    /**
     * 経費を新規作成（ファクトリーメソッド）
     * 必ず下書き状態で作成される
     */
    public static Expense create(Long applicantId, String title,
                                BigDecimal amount, String currency) {
        // バリデーション
        if (applicantId == null) {
            throw new IllegalArgumentException("申請者IDは必須です");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("タイトルは必須です");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金額は正の数である必要があります");
        }

        Expense expense = new Expense();
        expense.applicantId = applicantId;
        expense.title = title;
        expense.amount = amount;
        expense.currency = currency != null ? currency : "JPY";
        expense.status = ExpenseStatus.DRAFT;  // 必ず下書きから開始
        expense.version = 0;                   // 初期バージョン
        expense.createdAt = LocalDateTime.now();
        expense.updatedAt = LocalDateTime.now();

        return expense;
    }

    /**
     * 経費を提出
     * ビジネスルール: 下書き状態のみ提出可能
     *
     * 重要: このメソッドでversionを加算する
     * SQLでは加算しない（ドメインの値をそのまま使う）
     */
    public void submit() {
        // ビジネスルールチェック
        if (this.status != ExpenseStatus.DRAFT) {
            throw new IllegalStateException(
                String.format("下書き状態の経費のみ提出可能です。現在: %s", this.status)
            );
        }

        // 状態を完全に更新
        this.status = ExpenseStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version++;  // ドメインでバージョン管理
    }

    /**
     * 経費を承認
     * ビジネスルール: 提出済み状態のみ承認可能
     *
     * 重要: このメソッドでversionを加算する
     * ドメインの状態がデータベースにそのまま反映される
     */
    public void approve() {
        if (this.status != ExpenseStatus.SUBMITTED) {
            throw new IllegalStateException(
                String.format("提出済みの経費のみ承認可能です。現在: %s", this.status)
            );
        }

        // 状態を完全に更新
        this.status = ExpenseStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
        this.version++;  // バージョン加算
    }

    /**
     * 経費を却下
     * ビジネスルール: 提出済み状態のみ却下可能
     *
     * @param reason 却下理由（必須）
     */
    public void reject(String reason) {
        if (this.status != ExpenseStatus.SUBMITTED) {
            throw new IllegalStateException(
                String.format("提出済みの経費のみ却下可能です。現在: %s", this.status)
            );
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("却下理由は必須です");
        }

        // 状態を完全に更新
        this.status = ExpenseStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
        this.version++;  // バージョン加算
    }

    /**
     * 指定ユーザーが提出可能か
     */
    public boolean canBeSubmittedBy(Long userId) {
        return this.applicantId.equals(userId) &&
               this.status == ExpenseStatus.DRAFT;
    }

    /**
     * 承認可能な状態か
     */
    public boolean canBeApproved() {
        return this.status == ExpenseStatus.SUBMITTED;
    }

    /**
     * 却下可能な状態か
     */
    public boolean canBeRejected() {
        return this.status == ExpenseStatus.SUBMITTED;
    }

    /**
     * 高額経費か（10万円以上）
     */
    public boolean isHighValue() {
        BigDecimal threshold = new BigDecimal("100000");
        return this.amount.compareTo(threshold) >= 0;
    }

    /**
     * 古い経費か（提出から30日以上経過）
     */
    public boolean isOld() {
        if (this.submittedAt == null) {
            return false;
        }
        return this.submittedAt.isBefore(LocalDateTime.now().minusDays(30));
    }

    // MyBatis用のpackage-private setter
    // データベースからの読み込み時のみ使用
    void setId(Long id) { this.id = id; }
    void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    void setTitle(String title) { this.title = title; }
    void setAmount(BigDecimal amount) { this.amount = amount; }
    void setCurrency(String currency) { this.currency = currency; }
    void setStatus(ExpenseStatus status) { this.status = status; }
    void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    void setVersion(int version) { this.version = version; }
}
```

---

## 2. Repository（汎用更新メソッド）

```java
// src/main/java/com/example/expenses/repository/ExpenseMapper.java
package com.example.expenses.repository;

import com.example.expenses.domain.Expense;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ExpenseMapper {

    /**
     * 経費を新規登録
     */
    @Insert("""
        INSERT INTO expenses
            (applicant_id, title, amount, currency, status, version)
        VALUES
            (#{applicantId}, #{title}, #{amount}, #{currency}, #{status}, #{version})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Expense expense);

    /**
     * IDで経費を取得
     */
    @Select("""
        SELECT id, applicant_id, title, amount, currency, status,
               submitted_at, created_at, updated_at, version
        FROM expenses
        WHERE id = #{expenseId}
        """)
    Expense findById(Long expenseId);

    /**
     * 楽観的ロック付き汎用更新メソッド
     *
     * 重要ポイント:
     * 1. ドメインオブジェクトの状態をそのまま永続化
     * 2. SET句: ドメインのversionをそのまま使用（version++しない）
     * 3. WHERE句: version = #{version} - 1 で楽観的ロックを実現
     *
     * なぜ version - 1 なのか:
     * - ドメインメソッド（approve()等）で既に version++ されている
     * - 例: DB上のversion=5、ドメインでversion++して6になっている
     * - WHERE version = 6 - 1 (=5) で元のバージョンと照合
     * - 更新成功後、DBのversionは6になる（ドメインと一致）
     */
    @Update("""
        UPDATE expenses
        SET applicant_id = #{applicantId},
            title = #{title},
            amount = #{amount},
            currency = #{currency},
            status = #{status},
            submitted_at = #{submittedAt},
            updated_at = #{updatedAt},
            version = #{version}
        WHERE id = #{id}
            AND version = #{version} - 1
        """)
    int updateWithOptimisticLock(Expense expense);

    // approve/reject専用メソッドは不要！
    // 汎用のupdateWithOptimisticLockで全ての更新をカバー
}
```

### 📚 SQL学習ポイント

**WHERE version = #{version} - 1 の仕組み**

```
初期状態（DB）:
  id=1, status=SUBMITTED, version=5

処理フロー:
1. findById(1) で取得
   → expense.version = 5

2. ドメインメソッド呼び出し
   expense.approve()
   → expense.status = APPROVED
   → expense.version = 6 (5 + 1)

3. updateWithOptimisticLock(expense) 実行
   SQL:
   UPDATE expenses
   SET status = 'APPROVED',
       version = 6           ← ドメインの値（既に+1済み）
   WHERE id = 1
       AND version = 6 - 1   ← つまり version = 5

4. 結果:
   - WHERE version = 5 → マッチ（更新成功）
   - SET version = 6   → DBのversionが6に
   - ドメインとDBが一致

競合時:
  他のユーザーが先に更新済み（version=6になっている）
  → WHERE version = 6 - 1 (=5) → マッチせず
  → 更新件数0 → 例外発生
```

---

## 3. Service（ドメイン主導）

```java
// src/main/java/com/example/expenses/service/ExpenseService.java
package com.example.expenses.service;

import com.example.expenses.domain.Expense;
import com.example.expenses.event.*;
import com.example.expenses.security.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseMapper expenseMapper;
    private final AuthenticationContext authContext;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 経費を新規作成
     */
    @Transactional
    public ExpenseResponse create(ExpenseCreateRequest req) {
        Long userId = authContext.getCurrentUserId();

        // ドメインのファクトリーメソッドで作成
        Expense expense = Expense.create(
            userId,
            req.title(),
            req.amount(),
            req.currency()
        );

        // 永続化
        expenseMapper.insert(expense);

        log.info("経費作成: id={}, userId={}", expense.getId(), userId);

        // イベント発行
        eventPublisher.publishEvent(
            new ExpenseCreatedEvent(expense.getId(), userId, traceId())
        );

        // ドメインオブジェクトが最新状態なのでそのまま返せる
        return ExpenseResponse.toResponse(expense);
    }

    /**
     * 経費を提出
     */
    @Transactional
    public ExpenseResponse submit(Long expenseId) {
        Long userId = authContext.getCurrentUserId();

        // 経費を取得
        Expense expense = expenseMapper.findById(expenseId);
        if (expense == null) {
            throw new NoSuchElementException("経費が見つかりません: " + expenseId);
        }

        // ドメインで権限チェック
        if (!expense.canBeSubmittedBy(userId)) {
            throw new BusinessException(
                "NOT_AUTHORIZED",
                "この経費を提出する権限がありません"
            );
        }

        // ドメインメソッドで完全に状態を更新
        // 注: この時点で version++ される
        expense.submit();

        // ドメインの状態をそのまま永続化
        int updated = expenseMapper.updateWithOptimisticLock(expense);
        if (updated == 0) {
            // WHEREの条件（version）にマッチしなかった
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています"
            );
        }

        log.info("経費提出: id={}, userId={}", expenseId, userId);

        // イベント発行
        eventPublisher.publishEvent(
            new ExpenseSubmittedEvent(expenseId, userId, traceId())
        );

        // ドメインオブジェクトが最新なので再取得不要
        return ExpenseResponse.toResponse(expense);
    }

    /**
     * 経費を承認
     */
    @Transactional
    public ExpenseResponse approve(Long expenseId, int version) {
        Long userId = authContext.getCurrentUserId();

        // 経費を取得
        Expense expense = expenseMapper.findById(expenseId);
        if (expense == null) {
            throw new NoSuchElementException("経費が見つかりません: " + expenseId);
        }

        // リクエスト時のバージョンと現在のバージョンを比較
        if (expense.getVersion() != version) {
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています（取得後に変更あり）"
            );
        }

        // ドメインメソッドで完全に状態を更新
        // 注: この時点で status=APPROVED, version++ される
        expense.approve();

        // ドメインの状態をそのまま永続化
        int updated = expenseMapper.updateWithOptimisticLock(expense);
        if (updated == 0) {
            // SQL実行時に他のユーザーが更新した場合
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています（更新直前に変更あり）"
            );
        }

        log.info("経費承認: id={}, approverId={}", expenseId, userId);

        // イベント発行
        eventPublisher.publishEvent(
            new ExpenseApprovedEvent(
                expenseId, userId, expense.getApplicantId(), traceId()
            )
        );

        // ドメインオブジェクトが最新（再取得不要）
        return ExpenseResponse.toResponse(expense);
    }

    /**
     * 経費を却下
     */
    @Transactional
    public ExpenseResponse reject(Long expenseId, String reason, int version) {
        Long userId = authContext.getCurrentUserId();

        // 経費を取得
        Expense expense = expenseMapper.findById(expenseId);
        if (expense == null) {
            throw new NoSuchElementException("経費が見つかりません: " + expenseId);
        }

        // バージョンチェック
        if (expense.getVersion() != version) {
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています"
            );
        }

        // ドメインメソッドで完全に状態を更新
        // 注: この時点で status=REJECTED, version++ される
        expense.reject(reason);

        // ドメインの状態をそのまま永続化
        int updated = expenseMapper.updateWithOptimisticLock(expense);
        if (updated == 0) {
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています"
            );
        }

        log.info("経費却下: id={}, rejectorId={}, reason={}", expenseId, userId, reason);

        // イベント発行
        eventPublisher.publishEvent(
            new ExpenseRejectedEvent(
                expenseId, userId, expense.getApplicantId(), reason, traceId()
            )
        );

        // ドメインオブジェクトが最新
        return ExpenseResponse.toResponse(expense);
    }

    private String traceId() {
        String tid = MDC.get(TraceIdFilter.TRACE_ID_KEY);
        return tid == null ? "" : tid;
    }
}
```

---

## 📚 学習ポイント

### 1. 真のリッチドメインモデル

```java
// ✅ 正しい: ドメインが真実の源泉
expense.approve();         // ドメインで完全に状態を更新
repository.update(expense); // ドメインの状態をそのまま永続化

// ❌ 間違い: ドメインとSQLが分離
expense.approve();          // ドメインで status = APPROVED にするが...
repository.approve(id);     // SQLが SET status = 'APPROVED' を実行
                           // → ドメインの状態変更が無意味
```

### 2. 責務の明確化

| コンポーネント | 責務 | 例 |
|--------------|------|-----|
| **ドメイン** | ビジネスルール + 状態管理 | `expense.approve()` → status変更 + version++ |
| **Repository** | 永続化のみ | `updateWithOptimisticLock(expense)` → ドメインの状態をDBへ |
| **Service** | オーケストレーション | トランザクション + イベント発行 |

### 3. 楽観的ロックの仕組み

```java
// ステップ1: 取得
Expense expense = repository.findById(1);
// expense.version = 5

// ステップ2: ドメインで更新
expense.approve();
// expense.version = 6 (5 + 1)
// expense.status = APPROVED

// ステップ3: 永続化
repository.updateWithOptimisticLock(expense);
// SQL: UPDATE ... SET version=6 WHERE version=6-1
//      → WHERE version=5 でマッチ
//      → 更新成功

// 競合時:
// 他のユーザーが先に更新（DB version=6）
// WHERE version=6-1 (=5) → マッチせず → 更新失敗
```

### 4. メリット

| メリット | 説明 |
|---------|------|
| **一貫性** | ドメインの状態 = DBの状態（常に一致） |
| **テスト容易** | ドメインだけでビジネスロジックをテスト可能 |
| **再利用性** | ドメインロジックを別の場所でも使える |
| **保守性** | ビジネスルールが1箇所に集約 |
| **パフォーマンス** | 再取得不要（ドメインが最新） |

---

## まとめ

### 修正前の問題

1. ❌ ドメインで`status`設定 → SQLが固定値使用 → 無意味
2. ❌ ドメインで`version++` → SQLでも`version+1` → 二重加算
3. ❌ approve/reject専用SQL → 汎用性なし
4. ❌ 更新後に再取得 → 無駄なクエリ

### 修正後の利点

1. ✅ ドメインで完全に状態管理
2. ✅ SQLは汎用的な更新のみ
3. ✅ `WHERE version = #{version} - 1` で楽観的ロック
4. ✅ 再取得不要（ドメインが真実）

これが**真のリッチドメインモデル**です！
