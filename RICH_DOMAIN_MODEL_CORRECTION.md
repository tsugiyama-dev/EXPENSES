# リッチドメインモデル - コンストラクタマッピング実装版

## 🎯 実装アプローチ

このドキュメントでは、**コンストラクタマッピング**を使ったリッチドメインモデルの実装方法を説明します。

### 設計思想

**完全なカプセル化とイミュータビリティ**

- ✅ publicなsetterを一切持たない
- ✅ MyBatisはコンストラクタ経由でオブジェクトを生成
- ✅ ビジネスロジックはドメイン内に実装
- ✅ 不正な状態遷移を防ぐ

---

## 📌 package-privateなsetterではなくコンストラクタマッピングを選んだ理由

### ❌ package-privateなsetterの問題点

```java
// package-privateなsetter
void setStatus(ExpenseStatus status) { this.status = status; }
```

**問題:**
1. MyBatisが同じパッケージにないとアクセスできない
2. ドメインモデルをrepository層に移動する必要がある（アーキテクチャ違反）
3. 完全なカプセル化ではない

### ✅ コンストラクタマッピングのメリット

```java
@AllArgsConstructor  // 全フィールドのコンストラクタ
public class Expense {
    // setterなし！
}
```

**メリット:**
1. ✅ setterが一切存在しない（完全カプセル化）
2. ✅ イミュータブルなオブジェクト
3. ✅ ドメイン層をそのまま維持できる
4. ✅ MyBatisが`@ConstructorArgs`や`<constructor>`でマッピング

---

## 1. ドメインエンティティ（コンストラクタマッピング版）

```java
// src/main/java/com/example/expenses/domain/Expense.java
package com.example.expenses.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 経費エンティティ（リッチドメインモデル - コンストラクタマッピング版）
 *
 * 重要な設計原則:
 * 1. setterを一切持たない（完全カプセル化）
 * 2. コンストラクタ経由でのみオブジェクト生成
 * 3. ビジネスロジックはドメイン内に実装
 * 4. 不正な状態遷移を防ぐ
 */
@Getter
@AllArgsConstructor  // 全フィールドのコンストラクタを自動生成
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
     * 下書き状態の経費を作成（ファクトリーメソッド）
     * 必ず下書き状態で作成される
     */
    public static Expense createDraft(Long applicantId, String title,
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

        LocalDateTime now = LocalDateTime.now();
        return new Expense(
            null,                    // idはDBが自動採番
            applicantId,
            title,
            amount,
            currency != null ? currency : "JPY",
            ExpenseStatus.DRAFT,     // 必ず下書きから開始
            null,                    // submittedAtは提出時に設定
            now,                     // createdAt
            now,                     // updatedAt
            0                        // 初期バージョン
        );
    }

    /**
     * 提出可能かチェック
     */
    public boolean canBeSubmitted() {
        return this.status == ExpenseStatus.DRAFT;
    }

    /**
     * 承認可能かチェック
     */
    public boolean canBeApproved() {
        return this.status == ExpenseStatus.SUBMITTED;
    }

    /**
     * 却下可能かチェック
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

    /**
     * 指定ユーザーが提出可能か
     */
    public boolean canBeSubmittedBy(Long userId) {
        return this.applicantId.equals(userId) && canBeSubmitted();
    }
}
```

### 🔑 重要ポイント

1. **`@AllArgsConstructor`**: 全フィールドのコンストラクタを自動生成
2. **setterなし**: 外部から状態を変更できない
3. **ファクトリメソッド**: `createDraft()`で安全にオブジェクト生成
4. **ビジネスメソッド**: `canBeSubmitted()`などでビジネスルールをカプセル化

---

## 2. MyBatisのコンストラクタマッピング設定

### 2.1 XMLマッピング（ExpenseMapper.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.expenses.repository.ExpenseMapper">

  <!-- コンストラクタマッピング用のresultMap -->
  <resultMap id="expenseResultMap" type="com.example.expenses.domain.Expense">
    <constructor>
      <idArg column="id" javaType="Long"/>
      <arg column="applicant_id" javaType="Long"/>
      <arg column="title" javaType="String"/>
      <arg column="amount" javaType="java.math.BigDecimal"/>
      <arg column="currency" javaType="String"/>
      <arg column="status" javaType="com.example.expenses.domain.ExpenseStatus"/>
      <arg column="submitted_at" javaType="java.time.LocalDateTime"/>
      <arg column="created_at" javaType="java.time.LocalDateTime"/>
      <arg column="updated_at" javaType="java.time.LocalDateTime"/>
      <arg column="version" javaType="int"/>
    </constructor>
  </resultMap>

  <!-- 検索クエリ（resultMapを使用） -->
  <select id="search" resultMap="expenseResultMap">
    SELECT
      id, applicant_id, title, amount, currency, status,
      submitted_at, created_at, updated_at, version
    FROM expenses
    WHERE 1 = 1
    <if test="criteria.applicantId != null">
      AND applicant_id = #{criteria.applicantId}
    </if>
    <if test="criteria.title != null and criteria.title != ''">
      AND title LIKE CONCAT('%', #{criteria.title}, '%')
    </if>
    <!-- 省略 -->
    ORDER BY ${orderBy} ${direction}
    LIMIT #{size} OFFSET #{offset}
  </select>

  <!-- フィルタークエリ -->
  <select id="filter" resultMap="expenseResultMap">
    SELECT
      id, applicant_id, title, amount, currency, status,
      submitted_at, created_at, updated_at, version
    FROM expenses
    <trim prefix="WHERE" prefixOverrides="AND |OR ">
      <if test="criteria.status != ''">
        AND status = #{criteria.status}
      </if>
      <!-- 省略 -->
    </trim>
    ORDER BY
    <choose>
      <when test="orderBy == 'title'">title</when>
      <when test="orderBy == 'submitted_at'">submitted_at</when>
      <when test="orderBy == 'updated_at'">updated_at</when>
      <otherwise>created_at</otherwise>
    </choose>
    <if test="direction == 'ASC' or direction == 'DESC'">
      ${direction}
    </if>
  </select>

</mapper>
```

### 📚 XML学習ポイント

**`<constructor>`タグの仕組み:**

```xml
<constructor>
  <idArg column="id" javaType="Long"/>      <!-- 主キー -->
  <arg column="applicant_id" javaType="Long"/>  <!-- 通常フィールド -->
  <!-- ... -->
</constructor>
```

- `<idArg>`: 主キーカラム
- `<arg>`: 通常のカラム
- **引数の順番**: `@AllArgsConstructor`のフィールド宣言順と一致させる
- **javaType**: 完全修飾名またはシンプル名

---

### 2.2 アノテーションマッピング（ExpenseMapper.java）

```java
package com.example.expenses.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.dto.request.ExpenseSearchCriteriaEntity;

@Mapper
public interface ExpenseMapper {

    /**
     * 経費を新規登録
     */
    @Insert("""
        INSERT INTO expenses
            (applicant_id, title, amount, currency, status)
        VALUES
            (#{applicantId}, #{title}, #{amount}, #{currency}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Expense expense);

    /**
     * IDで経費を取得（コンストラクタマッピング）
     */
    @ConstructorArgs({
        @Arg(column = "id", javaType = Long.class, id = true),
        @Arg(column = "applicant_id", javaType = Long.class),
        @Arg(column = "title", javaType = String.class),
        @Arg(column = "amount", javaType = BigDecimal.class),
        @Arg(column = "currency", javaType = String.class),
        @Arg(column = "status", javaType = ExpenseStatus.class),
        @Arg(column = "submitted_at", javaType = LocalDateTime.class),
        @Arg(column = "created_at", javaType = LocalDateTime.class),
        @Arg(column = "updated_at", javaType = LocalDateTime.class),
        @Arg(column = "version", javaType = int.class)
    })
    @Select("""
        SELECT id, applicant_id, title, amount, currency, status,
               submitted_at, created_at, updated_at, version
        FROM expenses
        WHERE id = #{expenseId}
        """)
    Expense findById(Long expenseId);

    /**
     * ユーザーIDで経費リストを取得
     */
    @ConstructorArgs({
        @Arg(column = "id", javaType = Long.class, id = true),
        @Arg(column = "applicant_id", javaType = Long.class),
        @Arg(column = "title", javaType = String.class),
        @Arg(column = "amount", javaType = BigDecimal.class),
        @Arg(column = "currency", javaType = String.class),
        @Arg(column = "status", javaType = ExpenseStatus.class),
        @Arg(column = "submitted_at", javaType = LocalDateTime.class),
        @Arg(column = "created_at", javaType = LocalDateTime.class),
        @Arg(column = "updated_at", javaType = LocalDateTime.class),
        @Arg(column = "version", javaType = int.class)
    })
    @Select("""
        SELECT id, applicant_id, title, amount, currency, status,
               submitted_at, created_at, updated_at, version
        FROM expenses
        WHERE applicant_id = #{applicantId}
        LIMIT 5 OFFSET 0
        """)
    List<Expense> findByUserId(@Param("applicantId") Long applicantId);

    /**
     * 下書きを提出状態に変更
     */
    @Update("""
        UPDATE expenses
        SET status = 'SUBMITTED',
            submitted_at = NOW()
        WHERE id = #{expenseId}
            AND status = 'DRAFT'
        """)
    int submitDraft(@Param("expenseId") Long expenseId);

    /**
     * 経費を承認（楽観的ロック付き）
     */
    @Update("""
        UPDATE expenses
        SET status = 'APPROVED',
            updated_at = NOW(),
            version = version + 1
        WHERE id = #{id}
            AND version = #{version}
            AND status = 'SUBMITTED'
        """)
    int approve(@Param("id") long id, @Param("version") int version);

    /**
     * 経費を却下（楽観的ロック付き）
     */
    @Update("""
        UPDATE expenses
        SET status = 'REJECTED',
            updated_at = NOW(),
            version = version + 1
        WHERE id = #{id}
            AND version = #{version}
            AND status = 'SUBMITTED'
        """)
    int reject(@Param("id") long id, @Param("version") int version);

    // XMLで定義されているメソッド
    List<Expense> search(
        @Param("criteria") ExpenseSearchCriteriaEntity criteria,
        @Param("orderBy") String orderBy,
        @Param("direction") String direction,
        @Param("size") int size,
        @Param("offset") int offset);

    long count(@Param("criteria") ExpenseSearchCriteriaEntity criteria);

    List<Expense> filter(
        @Param("criteria") ExpenseSearchCriteriaEntity criteria,
        @Param("orderBy") String orderBy,
        @Param("direction") String direction);
}
```

### 📚 アノテーション学習ポイント

**`@ConstructorArgs`の仕組み:**

```java
@ConstructorArgs({
    @Arg(column = "id", javaType = Long.class, id = true),  // 主キー
    @Arg(column = "applicant_id", javaType = Long.class),   // 通常フィールド
    // ...
})
@Select("SELECT id, applicant_id, ... FROM expenses WHERE ...")
Expense findById(Long expenseId);
```

- **`@Arg`**: 各コンストラクタ引数を定義
- **`id = true`**: 主キーを示す
- **`column`**: データベースのカラム名
- **`javaType`**: Javaの型
- **順番**: `@AllArgsConstructor`のフィールド宣言順と一致させる

---

## 3. Service層（ドメイン主導）

```java
// src/main/java/com/example/expenses/service/ExpenseService.java
package com.example.expenses.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.expenses.domain.Expense;
import com.example.expenses.domain.ExpenseStatus;
import com.example.expenses.dto.request.ExpenseCreateRequest;
import com.example.expenses.dto.response.ExpenseResponse;
import com.example.expenses.exception.BusinessException;
import com.example.expenses.repository.ExpenseMapper;
import com.example.expenses.util.CurrentUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseMapper expenseMapper;

    /**
     * 経費を新規作成
     */
    @Transactional
    public ExpenseResponse create(ExpenseCreateRequest req) {
        Long userId = CurrentUser.actorId();

        // ドメインのファクトリーメソッドで作成（バリデーション含む）
        Expense expense = Expense.createDraft(
            userId,
            req.title(),
            req.amount(),
            req.currency()
        );

        // 永続化
        expenseMapper.insert(expense);

        log.info("経費作成: id={}, userId={}", expense.getId(), userId);

        // 作成後のデータを取得して返す
        Expense saved = expenseMapper.findById(expense.getId());
        return ExpenseResponse.toResponse(saved);
    }

    /**
     * 経費を提出
     */
    @Transactional
    public ExpenseResponse submit(Long expenseId) {
        Long userId = CurrentUser.actorId();

        // 経費を取得
        Expense expense = expenseMapper.findById(expenseId);
        if (expense == null) {
            throw new NoSuchElementException("経費が見つかりません: " + expenseId);
        }

        // ドメインで権限チェック
        if (!expense.canBeSubmittedBy(userId)) {
            throw new BusinessException(
                "NOT_AUTHORIZED",
                "本人以外は提出できません"
            );
        }

        // 提出処理（SQLで実行）
        int updated = expenseMapper.submitDraft(expenseId);
        if (updated == 0) {
            throw new BusinessException(
                "INVALID_STATUS_TRANSITION",
                "下書き以外は提出できません"
            );
        }

        log.info("経費提出: id={}, userId={}", expenseId, userId);

        // 更新後のデータを取得
        Expense saved = expenseMapper.findById(expenseId);
        return ExpenseResponse.toResponse(saved);
    }

    /**
     * 経費を承認
     */
    @Transactional
    public ExpenseResponse approve(Long expenseId, int version) {
        Long approverId = CurrentUser.actorId();

        // 経費を取得
        Expense expense = expenseMapper.findById(expenseId);
        if (expense == null) {
            throw new NoSuchElementException("経費が見つかりません: " + expenseId);
        }

        // ドメインでビジネスルールチェック
        if (!expense.canBeApproved()) {
            throw new BusinessException(
                "INVALID_STATUS_TRANSITION",
                "提出済み以外は承認できません"
            );
        }

        // バージョンチェック（楽観的ロック）
        if (expense.getVersion() != version) {
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています"
            );
        }

        // 承認処理（SQLで実行）
        int updated = expenseMapper.approve(expenseId, version);
        if (updated == 0) {
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています"
            );
        }

        log.info("経費承認: id={}, approverId={}", expenseId, approverId);

        // 更新後のデータを取得
        Expense saved = expenseMapper.findById(expenseId);
        return ExpenseResponse.toResponse(saved);
    }

    /**
     * 経費を却下
     */
    @Transactional
    public ExpenseResponse reject(Long expenseId, String reason, int version) {
        Long rejectorId = CurrentUser.actorId();

        // 経費を取得
        Expense expense = expenseMapper.findById(expenseId);
        if (expense == null) {
            throw new NoSuchElementException("経費が見つかりません: " + expenseId);
        }

        // ドメインでビジネスルールチェック
        if (!expense.canBeRejected()) {
            throw new BusinessException(
                "INVALID_STATUS_TRANSITION",
                "提出済み以外は却下できません"
            );
        }

        // バージョンチェック
        if (expense.getVersion() != version) {
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています"
            );
        }

        // 却下処理（SQLで実行）
        int updated = expenseMapper.reject(expenseId, version);
        if (updated == 0) {
            throw new BusinessException(
                "CONCURRENT_MODIFICATION",
                "他のユーザーに更新されています"
            );
        }

        log.info("経費却下: id={}, rejectorId={}, reason={}", expenseId, rejectorId, reason);

        // 更新後のデータを取得
        Expense saved = expenseMapper.findById(expenseId);
        return ExpenseResponse.toResponse(saved);
    }
}
```

---

## 📚 学習ポイント

### 1. コンストラクタマッピングのメリット

| メリット | 説明 |
|---------|------|
| **完全カプセル化** | setterが一切存在しない |
| **イミュータビリティ** | オブジェクト生成後は変更不可 |
| **安全性** | 不正な状態を持つオブジェクトを生成できない |
| **明確性** | ファクトリメソッドで生成方法が明確 |

### 2. ドメイン駆動設計（DDD）との相性

```java
// ✅ 良い例: ドメインでビジネスルールをチェック
if (!expense.canBeSubmittedBy(userId)) {
    throw new BusinessException("本人以外は提出できません");
}

// ❌ 悪い例: サービス層でビジネスロジック
if (!expense.getApplicantId().equals(userId) ||
    expense.getStatus() != ExpenseStatus.DRAFT) {
    throw new BusinessException("提出できません");
}
```

### 3. 楽観的ロックの仕組み

```java
// ステップ1: 取得
Expense expense = expenseMapper.findById(1);
// expense.version = 5

// ステップ2: バージョンチェック
if (expense.getVersion() != requestVersion) {
    throw new BusinessException("他のユーザーに更新されています");
}

// ステップ3: 更新（SQLでversion++）
int updated = expenseMapper.approve(id, version);
// SQL: UPDATE ... SET version = version + 1 WHERE version = 5
// → 他のユーザーが先に更新していたら、WHERE条件に一致せず更新失敗
```

### 4. MyBatisのINSERT時の注意点

```java
@Insert("""
    INSERT INTO expenses
        (applicant_id, title, amount, currency, status)
    VALUES
        (#{applicantId}, #{title}, #{amount}, #{currency}, #{status})
    """)
@Options(useGeneratedKeys = true, keyProperty = "id")
void insert(Expense expense);
```

**重要:**
- `@Options(useGeneratedKeys = true)`: 自動採番されたIDを取得
- `keyProperty = "id"`: 取得したIDをExpenseの`id`フィールドに設定
- **問題**: `@AllArgsConstructor`でイミュータブルなので、idを後から設定できない！

**解決策:**
- MyBatisは内部的にリフレクションでフィールドに直接アクセスできる
- setterがなくてもidを設定できる

---

## 🎯 まとめ

### コンストラクタマッピング実装のポイント

1. ✅ **Expenseクラス**: `@Getter` + `@AllArgsConstructor`でsetterなし
2. ✅ **ファクトリメソッド**: `createDraft()`で安全にオブジェクト生成
3. ✅ **ビジネスメソッド**: `canBeSubmitted()`などでルールをカプセル化
4. ✅ **MyBatis XML**: `<constructor>`タグでマッピング
5. ✅ **MyBatis アノテーション**: `@ConstructorArgs`でマッピング
6. ✅ **Service層**: ドメインのメソッドでビジネスルールをチェック

### 従来のsetterアプローチとの違い

| 項目 | package-privateなsetter | コンストラクタマッピング |
|------|------------------------|----------------------|
| **カプセル化** | ⚠️ 部分的 | ✅ 完全 |
| **イミュータビリティ** | ❌ 可変 | ✅ 不変 |
| **MyBatis設定** | シンプル | やや複雑 |
| **アーキテクチャ** | ドメインをrepositoryに移動が必要 | ドメイン層をそのまま維持 |
| **安全性** | ⚠️ setterを誤って呼べる | ✅ setterが存在しない |

### 次のステップ

現在の実装では、まだ**状態変更がドメイン内で完結していません**。

次の改善案:
1. `submit()`, `approve()`, `reject()`メソッドをExpenseクラスに追加
2. これらのメソッドで新しいExpenseインスタンスを返す（イミュータブル）
3. `updateWithOptimisticLock()`のような汎用更新メソッドを追加

**例:**
```java
public Expense submit() {
    if (!canBeSubmitted()) {
        throw new IllegalStateException("下書き以外は提出できません");
    }
    return new Expense(
        this.id,
        this.applicantId,
        this.title,
        this.amount,
        this.currency,
        ExpenseStatus.SUBMITTED,  // ステータス変更
        LocalDateTime.now(),      // 提出日時設定
        this.createdAt,
        LocalDateTime.now(),      // 更新日時
        this.version + 1          // バージョン++
    );
}
```

これが**真のリッチドメインモデル + イミュータビリティ**の実装です！
