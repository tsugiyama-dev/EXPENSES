package com.example.expenses.dto.batch;

import com.example.expenses.domain.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/**
 * 月次経費レポートDTO
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>集計結果の構造化</li>
 *   <li>ステータス別集計の管理</li>
 *   <li>DTOパターンの活用</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyExpenseReport {

    /**
     * 集計対象年月
     */
    private YearMonth targetMonth;

    /**
     * 合計件数
     */
    private int totalCount;

    /**
     * 合計金額
     */
    private BigDecimal totalAmount;

    /**
     * ステータス別集計（ステータス → 集計情報）
     */
    private Map<ExpenseStatus, StatusSummary> statusSummaries;

    /**
     * ステータス別集計情報
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusSummary {
        /**
         * 件数
         */
        private int count;

        /**
         * 合計金額
         */
        private BigDecimal amount;

        /**
         * 割合（%）
         */
        private double percentage;
    }
}
