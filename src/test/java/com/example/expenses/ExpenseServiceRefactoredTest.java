package com.example.expenses;

import com.example.expenses.domain.Category;
import com.example.expenses.domain.Expense;
import com.example.expenses.dto.ExpenseCreateRequest;
import com.example.expenses.dto.ExpenseResponse;
import com.example.expenses.exception.EntityNotFoundException;
import com.example.expenses.mapper.CategoryMapper;
import com.example.expenses.mapper.ExpenseMapper;
import com.example.expenses.security.AuthenticationContext;
import com.example.expenses.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ExpenseServiceのリファクタリング後のテスト
 *
 * Day 5で学習した以下の改善を反映しています：
 * 1. AuthenticationContext を使用（静的メソッドの排除）
 * 2. 依存性注入（DI）による疎結合
 * 3. テスト可能な設計
 *
 * このテストファイルは、リファクタリング後のコードがテストしやすくなったことを示します。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService リファクタリング後のテスト")
class ExpenseServiceRefactoredTest {

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private AuthenticationContext authContext;

    @InjectMocks
    private ExpenseService expenseService;

    private Long currentUserId;

    @BeforeEach
    void setUp() {
        // 共通セットアップ: 現在のユーザーIDを100Lに設定
        currentUserId = 100L;
        when(authContext.getCurrentUserId()).thenReturn(currentUserId);
    }

    // ========================================================================
    // リファクタリングのメリットを示すテスト
    // ========================================================================

    @Test
    @DisplayName("✅ リファクタリング後: 任意のユーザーIDでテストできる")
    void リファクタリング後_任意のユーザーIDでテストできる() {
        // Before: CurrentUser.actorId() は静的メソッドなのでモックできない
        // After: authContext.getCurrentUserId() は注入可能なのでモックできる！

        // Arrange
        Long customUserId = 999L;  // ✅ 任意のユーザーIDを設定可能
        when(authContext.getCurrentUserId()).thenReturn(customUserId);

        var category = new Category();
        category.setId(1L);
        category.setName("食費");
        when(categoryMapper.findById(1L)).thenReturn(category);

        when(expenseMapper.insert(any())).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(1L);
            return null;
        });

        var request = new ExpenseCreateRequest(
            1L,
            new BigDecimal("1000"),
            "テスト経費",
            LocalDate.now()
        );

        // Act
        ExpenseResponse result = expenseService.create(request);

        // Assert
        assertThat(result).isNotNull();

        // ✅ 正しいユーザーIDが設定されたことを検証
        verify(expenseMapper).insert(argThat(expense ->
            expense.getApplicantId().equals(customUserId)
        ));
    }

    @Test
    @DisplayName("✅ リファクタリング後: SecurityContextの設定が不要")
    void リファクタリング後_SecurityContextの設定が不要() {
        // Before: SecurityContextHolder.setContext() で複雑なセットアップが必要
        // After: シンプルなモック設定だけでOK！

        // Arrange
        var category = new Category();
        category.setId(1L);
        category.setName("食費");
        when(categoryMapper.findById(1L)).thenReturn(category);

        when(expenseMapper.insert(any())).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(1L);
            return null;
        });

        var request = new ExpenseCreateRequest(
            1L,
            new BigDecimal("1000"),
            "テスト経費",
            LocalDate.now()
        );

        // Act
        ExpenseResponse result = expenseService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);

        // ✅ シンプルなテストで済む
        verify(authContext).getCurrentUserId();
    }

    @Test
    @DisplayName("✅ リファクタリング後: 複数ユーザーのシナリオをテストできる")
    void リファクタリング後_複数ユーザーのシナリオをテストできる() {
        // 異なるユーザーIDで複数回実行するテスト

        // ユーザー1のテスト
        Long userId1 = 100L;
        when(authContext.getCurrentUserId()).thenReturn(userId1);
        testCreateExpenseForUser(userId1);

        // ユーザー2のテスト
        Long userId2 = 200L;
        when(authContext.getCurrentUserId()).thenReturn(userId2);
        testCreateExpenseForUser(userId2);

        // ✅ 簡単に異なるユーザーのシナリオをテストできる
    }

    private void testCreateExpenseForUser(Long expectedUserId) {
        var category = new Category();
        category.setId(1L);
        category.setName("食費");
        when(categoryMapper.findById(1L)).thenReturn(category);

        when(expenseMapper.insert(any())).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(expectedUserId);  // ユーザーIDをそのまま使用
            return null;
        });

        var request = new ExpenseCreateRequest(
            1L,
            new BigDecimal("1000"),
            "テスト経費",
            LocalDate.now()
        );

        // Act
        expenseService.create(request);

        // Verify
        verify(expenseMapper).insert(argThat(expense ->
            expense.getApplicantId().equals(expectedUserId)
        ));
    }

    // ========================================================================
    // バリデーションのテスト
    // ========================================================================

    @Test
    @DisplayName("金額が負の場合は例外が発生する")
    void 金額が負の場合は例外が発生する() {
        // Arrange
        var request = new ExpenseCreateRequest(
            1L,
            new BigDecimal("-100"),
            "テスト経費",
            LocalDate.now()
        );

        // Act & Assert
        assertThatThrownBy(() -> expenseService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("金額は正の数である必要があります");

        // Verify: DB操作は実行されない
        verify(expenseMapper, never()).insert(any());
    }

    @Test
    @DisplayName("存在しないカテゴリIDの場合は例外が発生する")
    void 存在しないカテゴリIDの場合は例外が発生する() {
        // Arrange
        Long nonExistentCategoryId = 999L;
        when(categoryMapper.findById(nonExistentCategoryId)).thenReturn(null);

        var request = new ExpenseCreateRequest(
            nonExistentCategoryId,
            new BigDecimal("1000"),
            "テスト経費",
            LocalDate.now()
        );

        // Act & Assert
        assertThatThrownBy(() -> expenseService.create(request))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("カテゴリが見つかりません: " + nonExistentCategoryId);

        // Verify
        verify(categoryMapper).findById(nonExistentCategoryId);
        verify(expenseMapper, never()).insert(any());
    }

    @Test
    @DisplayName("説明が空文字の場合は例外が発生する")
    void 説明が空文字の場合は例外が発生する() {
        // Arrange
        var request = new ExpenseCreateRequest(
            1L,
            new BigDecimal("1000"),
            "",  // 空文字
            LocalDate.now()
        );

        // Act & Assert
        assertThatThrownBy(() -> expenseService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("説明は必須です");

        verify(expenseMapper, never()).insert(any());
    }

    @Test
    @DisplayName("未来の日付の場合は例外が発生する")
    void 未来の日付の場合は例外が発生する() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(1);
        var request = new ExpenseCreateRequest(
            1L,
            new BigDecimal("1000"),
            "テスト経費",
            futureDate
        );

        // Act & Assert
        assertThatThrownBy(() -> expenseService.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("経費日付は未来の日付にできません");

        verify(expenseMapper, never()).insert(any());
    }

    // ========================================================================
    // 正常ケースのテスト
    // ========================================================================

    @Test
    @DisplayName("経費を正常に作成できる")
    void 経費を正常に作成できる() {
        // Arrange
        var category = new Category();
        category.setId(1L);
        category.setName("食費");
        when(categoryMapper.findById(1L)).thenReturn(category);

        when(expenseMapper.insert(any())).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(1L);
            return null;
        });

        var request = new ExpenseCreateRequest(
            1L,
            new BigDecimal("1000"),
            "ランチ",
            LocalDate.of(2024, 1, 15)
        );

        // Act
        ExpenseResponse result = expenseService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.amount()).isEqualByComparingTo("1000");
        assertThat(result.description()).isEqualTo("ランチ");
        assertThat(result.categoryName()).isEqualTo("食費");

        // Verify
        verify(authContext).getCurrentUserId();
        verify(categoryMapper).findById(1L);
        verify(expenseMapper).insert(argThat(expense ->
            expense.getApplicantId().equals(currentUserId) &&
            expense.getCategoryId().equals(1L) &&
            expense.getAmount().compareTo(new BigDecimal("1000")) == 0 &&
            expense.getDescription().equals("ランチ")
        ));
    }
}
